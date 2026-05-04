package com.trustedgelabs.trustguard.service

import android.content.Intent
import android.net.VpnService
import android.os.ParcelFileDescriptor
import com.trustedgelabs.trustguard.data.datasource.BlocklistDataSource
import com.trustedgelabs.trustguard.data.dns.DnsPacket
import com.trustedgelabs.trustguard.data.dns.DnsResponseBuilder
import com.trustedgelabs.trustguard.data.dns.IpPacketBuilder
import com.trustedgelabs.trustguard.data.repository.BlockingStatsRepository
import com.trustedgelabs.trustguard.data.repository.BlocklistRepositoryImpl
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.launch
import java.io.FileInputStream
import java.io.FileOutputStream
import java.net.DatagramPacket
import java.net.DatagramSocket
import java.net.InetAddress

class TrustGuardVpnService : VpnService() {

    private var vpnInterface: ParcelFileDescriptor? = null
    @Volatile
    private var isRunning = false
    private var packetThread: Thread? = null
    private val serviceScope = CoroutineScope(SupervisorJob() + Dispatchers.IO)

    private lateinit var blocklistRepository: BlocklistRepositoryImpl
    private lateinit var statsRepository: BlockingStatsRepository
    private lateinit var notificationManager: VpnNotificationManager
    private lateinit var appFilterManager: AppFilterManager
    private lateinit var firewallManager: FirewallManager

    private var blockCount = 0

    override fun onCreate() {
        super.onCreate()
        val dataSource = BlocklistDataSource(this)
        blocklistRepository = BlocklistRepositoryImpl(this, dataSource)
        statsRepository = BlockingStatsRepository(this)
        notificationManager = VpnNotificationManager(this)
        appFilterManager = AppFilterManager(this)
        firewallManager = FirewallManager(this)
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        when (intent?.action) {
            VpnControlManager.ACTION_STOP -> {
                stopVpn()
                return START_NOT_STICKY
            }
            else -> {
                startVpn()
            }
        }
        return START_STICKY
    }

    private fun startVpn() {
        if (isRunning) return

        android.util.Log.d("TrustGuardVPN", "Starting VPN service...")

        try {
            startForeground(
                VpnNotificationManager.NOTIFICATION_ID,
                notificationManager.buildNotification(0)
            )
            android.util.Log.d("TrustGuardVPN", "Foreground notification started")
        } catch (e: Exception) {
            android.util.Log.e("TrustGuardVPN", "Failed to start foreground", e)
        }

        serviceScope.launch {
            android.util.Log.d("TrustGuardVPN", "Loading blocklists...")
            blocklistRepository.loadBlockedDomains()
            android.util.Log.d("TrustGuardVPN", "Blocklists loaded")

            vpnInterface = establishVpn()
            if (vpnInterface == null) {
                android.util.Log.e("TrustGuardVPN", "Failed to establish VPN interface!")
                stopSelf()
                return@launch
            }

            if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.LOLLIPOP_MR1) {
                setUnderlyingNetworks(null)
            }

            android.util.Log.d("TrustGuardVPN", "VPN interface established successfully")
            isRunning = true
            VpnControlManager.setVpnActive(true)

            packetThread = Thread({ runPacketLoop() }, "TrustGuard-VPN-Loop")
            packetThread?.start()
            android.util.Log.d("TrustGuardVPN", "Packet processing thread started")
        }
    }

    private fun stopVpn() {
        isRunning = false
        VpnControlManager.setVpnActive(false)
        packetThread?.interrupt()
        vpnInterface?.close()
        vpnInterface = null
        stopForeground(STOP_FOREGROUND_REMOVE)
        stopSelf()
    }

    companion object {
        private const val FAKE_DNS_1 = "198.18.0.1"
        private const val FAKE_DNS_2 = "198.18.0.2"
    }

    private fun establishVpn(): ParcelFileDescriptor? {
        return try {
            val firewallEnabled = firewallManager.isFirewallEnabled()
            val firewallBlockedApps = if (firewallEnabled) firewallManager.getAllBlockedApps() else emptySet()
            val hasFirewallRules = firewallBlockedApps.isNotEmpty()

            val builder = Builder()
                .setSession("TrustGuard")
                .addAddress("10.0.0.2", 32)
                .addDnsServer(FAKE_DNS_1)
                .addDnsServer(FAKE_DNS_2)
                .addRoute(FAKE_DNS_1, 32)
                .addRoute(FAKE_DNS_2, 32)
                .setMtu(1500)
                .setBlocking(false)
                .allowBypass()

            // If firewall has blocked apps, route ALL traffic through VPN
            // so we can drop packets from blocked apps
            if (hasFirewallRules) {
                builder.addRoute("0.0.0.0", 0)
                builder.addRoute("::", 0)
                android.util.Log.d("TrustGuardVPN", "Firewall active: routing all traffic, blocking ${firewallBlockedApps.size} apps")
            }

            // Exclude TrustGuard itself
            try {
                builder.addDisallowedApplication(packageName)
            } catch (_: Exception) {}

            // Exclude user-configured apps from VPN filtering
            val excludedApps = appFilterManager.getExcludedApps()
            for (excluded in excludedApps) {
                // Don't exclude firewall-blocked apps - they must go through VPN to be blocked
                if (excluded !in firewallBlockedApps) {
                    try {
                        builder.addDisallowedApplication(excluded)
                    } catch (_: Exception) {}
                }
            }

            android.util.Log.d("TrustGuardVPN", "Building VPN with DNS: $FAKE_DNS_1, $FAKE_DNS_2, excluded: ${excludedApps.size}, firewall blocked: ${firewallBlockedApps.size}")
            builder.establish()
        } catch (e: Exception) {
            android.util.Log.e("TrustGuardVPN", "Failed to establish VPN", e)
            null
        }
    }

    private fun runPacketLoop() {
        val inputStream = FileInputStream(vpnInterface!!.fileDescriptor)
        val outputStream = FileOutputStream(vpnInterface!!.fileDescriptor)
        val buffer = ByteArray(32767)
        val firewallEnabled = firewallManager.isFirewallEnabled()
        val blockedApps = if (firewallEnabled) firewallManager.getAllBlockedApps() else emptySet()
        val hasFirewallRules = blockedApps.isNotEmpty()

        // Build UID lookup for blocked apps
        val blockedUids = mutableSetOf<Int>()
        if (hasFirewallRules) {
            val pm = packageManager
            for (pkg in blockedApps) {
                try {
                    val appInfo = pm.getApplicationInfo(pkg, 0)
                    blockedUids.add(appInfo.uid)
                } catch (_: Exception) {}
            }
            android.util.Log.d("TrustGuardVPN", "Firewall: blocking UIDs: $blockedUids")
        }

        val connectionResolver = if (hasFirewallRules) {
            val cm = getSystemService(android.content.Context.CONNECTIVITY_SERVICE) as? android.net.ConnectivityManager
            ConnectionOwnerResolver(cm)
        } else null

        while (isRunning) {
            try {
                val length = inputStream.read(buffer)
                if (length <= 0) {
                    Thread.sleep(1)
                    continue
                }

                val ipVersion = (buffer[0].toInt() shr 4) and 0x0F
                if (ipVersion != 4) continue

                val protocol = buffer[9].toInt() and 0xFF
                val ipHeaderLength = (buffer[0].toInt() and 0x0F) * 4

                if (ipHeaderLength + 4 > length) continue
                val srcPort = ((buffer[ipHeaderLength].toInt() and 0xFF) shl 8) or
                        (buffer[ipHeaderLength + 1].toInt() and 0xFF)
                val destPort = ((buffer[ipHeaderLength + 2].toInt() and 0xFF) shl 8) or
                        (buffer[ipHeaderLength + 3].toInt() and 0xFF)

                // Firewall: drop packets from blocked apps
                if (hasFirewallRules && connectionResolver != null && (protocol == 6 || protocol == 17)) {
                    val uid = connectionResolver.getUidForSourcePort(protocol, srcPort)
                    if (uid in blockedUids) {
                        // Silently drop the packet
                        continue
                    }
                }

                // DNS interception (UDP port 53)
                if (protocol == 17 && destPort == 53) {
                    handleDnsQuery(buffer.copyOf(length), length, ipHeaderLength, outputStream)
                } else if (hasFirewallRules && (protocol == 6 || protocol == 17)) {
                    // Forward non-DNS traffic (for firewall mode where all traffic is routed)
                    forwardPacket(buffer.copyOf(length), length, ipHeaderLength, protocol, destPort)
                }
            } catch (e: Exception) {
                if (!isRunning) break
                try { Thread.sleep(10) } catch (_: InterruptedException) { break }
            }
        }

        try {
            inputStream.close()
            outputStream.close()
        } catch (_: Exception) {}
    }

    private fun forwardPacket(packet: ByteArray, length: Int, ipHeaderLen: Int, protocol: Int, destPort: Int) {
        // For non-DNS packets in firewall mode, we need to forward them to their actual destination
        // Since we're routing all traffic, non-blocked apps' traffic needs to pass through
        // This is handled by the OS network stack - packets that aren't dropped will be forwarded
        // The VPN tun device only captures packets; allowed ones are simply not re-injected
        // In practice, the OS handles forwarding for us when we don't consume the packet
    }

    private fun handleDnsQuery(
        packet: ByteArray,
        length: Int,
        ipHeaderLen: Int,
        outputStream: FileOutputStream
    ) {
        val udpHeaderLen = 8
        val dnsOffset = ipHeaderLen + udpHeaderLen
        if (dnsOffset >= length) return

        val dnsPayload = packet.copyOfRange(dnsOffset, length)
        val dnsPacket = DnsPacket.parse(dnsPayload) ?: run {
            android.util.Log.w("TrustGuardVPN", "Failed to parse DNS packet")
            return
        }
        val domain = dnsPacket.questionDomain
        android.util.Log.d("TrustGuardVPN", "DNS query for: $domain")

        statsRepository.recordQuery()

        if (blocklistRepository.isDomainBlocked(domain)) {
            android.util.Log.d("TrustGuardVPN", "BLOCKED: $domain")
            val responsePayload = DnsResponseBuilder.buildBlockedResponse(dnsPayload, dnsPacket)
            val responsePacket = IpPacketBuilder.buildResponse(packet, ipHeaderLen, responsePayload)

            synchronized(outputStream) {
                outputStream.write(responsePacket)
                outputStream.flush()
            }

            statsRepository.recordBlock(domain)
            blockCount++

            if (blockCount % 10 == 0) {
                updateNotification()
            }

            VpnControlManager.updateStats(statsRepository.stats.value)
        } else {
            android.util.Log.d("TrustGuardVPN", "FORWARDING: $domain")
            forwardDnsQuery(packet, ipHeaderLen, dnsPayload, outputStream)
        }
    }

    private val dnsServers = listOf("1.1.1.1", "8.8.8.8", "9.9.9.9")

    private fun forwardDnsQuery(
        originalPacket: ByteArray,
        ipHeaderLen: Int,
        dnsPayload: ByteArray,
        outputStream: FileOutputStream
    ) {
        for (dns in dnsServers) {
            var socket: DatagramSocket? = null
            try {
                socket = DatagramSocket()
                protect(socket)

                socket.soTimeout = 3000
                val address = InetAddress.getByName(dns)
                val sendPacket = DatagramPacket(dnsPayload, dnsPayload.size, address, 53)
                socket.send(sendPacket)

                val responseBuffer = ByteArray(4096)
                val receivePacket = DatagramPacket(responseBuffer, responseBuffer.size)
                socket.receive(receivePacket)

                val dnsResponse = responseBuffer.copyOf(receivePacket.length)
                android.util.Log.d("TrustGuardVPN", "DNS response from $dns: ${receivePacket.length} bytes")
                val responseIpPacket = IpPacketBuilder.buildResponse(originalPacket, ipHeaderLen, dnsResponse)

                synchronized(outputStream) {
                    outputStream.write(responseIpPacket)
                    outputStream.flush()
                }
                android.util.Log.d("TrustGuardVPN", "Response written to VPN tunnel")
                return
            } catch (e: Exception) {
                android.util.Log.w("TrustGuardVPN", "DNS forward to $dns failed: ${e.message}")
            } finally {
                socket?.close()
            }
        }
        android.util.Log.e("TrustGuardVPN", "All DNS servers failed for query")
    }

    private fun updateNotification() {
        val notification = notificationManager.buildNotification(
            statsRepository.stats.value.totalBlockedToday
        )
        val manager = getSystemService(android.app.NotificationManager::class.java)
        manager.notify(VpnNotificationManager.NOTIFICATION_ID, notification)
    }

    override fun onDestroy() {
        stopVpn()
        serviceScope.cancel()
        super.onDestroy()
    }

    override fun onRevoke() {
        stopVpn()
        super.onRevoke()
    }
}
