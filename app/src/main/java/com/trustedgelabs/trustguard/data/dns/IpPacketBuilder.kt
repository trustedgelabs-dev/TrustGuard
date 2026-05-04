package com.trustedgelabs.trustguard.data.dns

import java.io.ByteArrayOutputStream

object IpPacketBuilder {

    /**
     * Builds an IP + UDP response packet by swapping source/dest from the original query packet,
     * inserting the DNS response payload.
     */
    fun buildResponse(
        originalIpPacket: ByteArray,
        ipHeaderLength: Int,
        dnsResponsePayload: ByteArray
    ): ByteArray {
        val udpHeaderLength = 8
        val totalUdpLength = udpHeaderLength + dnsResponsePayload.size
        val totalIpLength = ipHeaderLength + totalUdpLength

        val output = ByteArrayOutputStream(totalIpLength)

        // IP Header
        // Version + IHL (same as original)
        output.write(originalIpPacket[0].toInt())
        // DSCP / ECN
        output.write(originalIpPacket[1].toInt())
        // Total Length
        output.write((totalIpLength shr 8) and 0xFF)
        output.write(totalIpLength and 0xFF)
        // Identification
        output.write(originalIpPacket[4].toInt())
        output.write(originalIpPacket[5].toInt())
        // Flags + Fragment Offset
        output.write(0x40) // Don't Fragment
        output.write(0x00)
        // TTL
        output.write(64)
        // Protocol (UDP = 17)
        output.write(17)
        // Header Checksum (placeholder - will calculate)
        output.write(0x00)
        output.write(0x00)

        // Swap Source and Destination IP
        // Source IP = original Dest IP
        output.write(originalIpPacket[16].toInt() and 0xFF)
        output.write(originalIpPacket[17].toInt() and 0xFF)
        output.write(originalIpPacket[18].toInt() and 0xFF)
        output.write(originalIpPacket[19].toInt() and 0xFF)
        // Dest IP = original Source IP
        output.write(originalIpPacket[12].toInt() and 0xFF)
        output.write(originalIpPacket[13].toInt() and 0xFF)
        output.write(originalIpPacket[14].toInt() and 0xFF)
        output.write(originalIpPacket[15].toInt() and 0xFF)

        val packet = output.toByteArray()

        // Calculate IP header checksum
        val checksum = calculateChecksum(packet, ipHeaderLength)
        packet[10] = ((checksum shr 8) and 0xFF).toByte()
        packet[11] = (checksum and 0xFF).toByte()

        // UDP Header
        val udpOutput = ByteArrayOutputStream(totalUdpLength)

        // Swap source/dest ports
        val origSrcPort0 = originalIpPacket[ipHeaderLength].toInt() and 0xFF
        val origSrcPort1 = originalIpPacket[ipHeaderLength + 1].toInt() and 0xFF
        val origDstPort0 = originalIpPacket[ipHeaderLength + 2].toInt() and 0xFF
        val origDstPort1 = originalIpPacket[ipHeaderLength + 3].toInt() and 0xFF

        // Source port = original dest port (53)
        udpOutput.write(origDstPort0)
        udpOutput.write(origDstPort1)
        // Dest port = original source port
        udpOutput.write(origSrcPort0)
        udpOutput.write(origSrcPort1)
        // UDP Length
        udpOutput.write((totalUdpLength shr 8) and 0xFF)
        udpOutput.write(totalUdpLength and 0xFF)
        // UDP Checksum = 0 (optional for IPv4)
        udpOutput.write(0x00)
        udpOutput.write(0x00)

        // DNS payload
        udpOutput.write(dnsResponsePayload)

        // Combine IP header + UDP
        return packet + udpOutput.toByteArray()
    }

    private fun calculateChecksum(header: ByteArray, length: Int): Int {
        var sum = 0L
        var i = 0
        while (i < length) {
            val word = ((header[i].toInt() and 0xFF) shl 8) or
                    (if (i + 1 < length) header[i + 1].toInt() and 0xFF else 0)
            // Skip checksum field at offset 10-11
            if (i != 10) {
                sum += word
            }
            i += 2
        }
        while (sum shr 16 != 0L) {
            sum = (sum and 0xFFFF) + (sum shr 16)
        }
        return (sum.toInt().inv()) and 0xFFFF
    }
}
