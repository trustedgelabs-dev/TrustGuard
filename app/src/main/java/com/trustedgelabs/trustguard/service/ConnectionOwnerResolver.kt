package com.trustedgelabs.trustguard.service

import android.net.ConnectivityManager
import android.os.Build
import java.io.BufferedReader
import java.io.FileReader
import java.net.InetSocketAddress

/**
 * Resolves which app (UID) owns a network connection.
 * Uses ConnectivityManager on API 29+ and proc/net parsing on older versions.
 */
class ConnectionOwnerResolver(private val connectivityManager: ConnectivityManager?) {

    /**
     * Returns the UID that owns a connection with the given parameters.
     * Returns -1 if the owner cannot be determined.
     */
    fun getUidForConnection(
        protocol: Int,
        srcAddr: String,
        srcPort: Int,
        dstAddr: String,
        dstPort: Int
    ): Int {
        // Try ConnectivityManager first (API 29+)
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q && connectivityManager != null) {
            try {
                val protoId = if (protocol == 6) ConnectivityManager.CONNECTIVITY_ACTION.length else 0
                // Use getConnectionOwnerUid
                val uid = connectivityManager.getConnectionOwnerUid(
                    protocol,
                    InetSocketAddress(srcAddr, srcPort),
                    InetSocketAddress(dstAddr, dstPort)
                )
                if (uid >= 0) return uid
            } catch (_: Exception) {}
        }

        // Fallback to /proc/net parsing
        return getUidFromProcNet(protocol, srcAddr, srcPort, dstAddr, dstPort)
    }

    /**
     * Parse proc net files (tcp, tcp6, udp, udp6)
     * to find the UID for a given connection.
     */
    private fun getUidFromProcNet(
        protocol: Int,
        srcAddr: String,
        srcPort: Int,
        dstAddr: String,
        dstPort: Int
    ): Int {
        val procFiles = when (protocol) {
            6 -> listOf("/proc/net/tcp", "/proc/net/tcp6")   // TCP
            17 -> listOf("/proc/net/udp", "/proc/net/udp6")  // UDP
            else -> return -1
        }

        val srcPortHex = String.format("%04X", srcPort)
        val dstPortHex = String.format("%04X", dstPort)

        for (procFile in procFiles) {
            try {
                BufferedReader(FileReader(procFile)).use { reader ->
                    reader.readLine() // Skip header
                    var line = reader.readLine()
                    while (line != null) {
                        val uid = parseProcNetLine(line, srcPortHex, dstPortHex)
                        if (uid >= 0) return uid
                        line = reader.readLine()
                    }
                }
            } catch (_: Exception) {}
        }

        return -1
    }

    private fun parseProcNetLine(line: String, srcPortHex: String, dstPortHex: String): Int {
        try {
            val parts = line.trim().split("\\s+".toRegex())
            if (parts.size < 8) return -1

            val localParts = parts[1].split(":")
            val remoteParts = parts[2].split(":")

            if (localParts.size == 2 && remoteParts.size == 2) {
                val localPort = localParts[1]
                val remotePort = remoteParts[1]

                if (localPort.equals(srcPortHex, ignoreCase = true) &&
                    remotePort.equals(dstPortHex, ignoreCase = true)) {
                    return parts[7].toIntOrNull() ?: -1
                }
            }
        } catch (_: Exception) {}
        return -1
    }

    /**
     * Quick lookup: get UID for a source port.
     * This is faster than full connection matching.
     */
    fun getUidForSourcePort(protocol: Int, srcPort: Int): Int {
        val procFiles = when (protocol) {
            6 -> listOf("/proc/net/tcp", "/proc/net/tcp6")
            17 -> listOf("/proc/net/udp", "/proc/net/udp6")
            else -> return -1
        }

        val srcPortHex = String.format("%04X", srcPort)

        for (procFile in procFiles) {
            try {
                BufferedReader(FileReader(procFile)).use { reader ->
                    reader.readLine() // Skip header
                    var line = reader.readLine()
                    while (line != null) {
                        val parts = line.trim().split("\\s+".toRegex())
                        if (parts.size >= 8) {
                            val localParts = parts[1].split(":")
                            if (localParts.size == 2 && localParts[1].equals(srcPortHex, ignoreCase = true)) {
                                val uid = parts[7].toIntOrNull()
                                if (uid != null && uid >= 0) return uid
                            }
                        }
                        line = reader.readLine()
                    }
                }
            } catch (_: Exception) {}
        }

        return -1
    }
}
