package com.trustedgelabs.trustguard.data.dns

import java.io.ByteArrayOutputStream

object DnsResponseBuilder {

    /**
     * Build a DNS response that returns 0.0.0.0 for a blocked domain.
     * This is more compatible than NXDOMAIN as some apps retry on NXDOMAIN.
     */
    fun buildBlockedResponse(originalQuery: ByteArray, dnsPacket: DnsPacket): ByteArray {
        val output = ByteArrayOutputStream()

        // Transaction ID (2 bytes) - copy from query
        output.write(originalQuery[0].toInt())
        output.write(originalQuery[1].toInt())

        // Flags (2 bytes): QR=1 (response), OPCODE=0, AA=1, TC=0, RD=1, RA=1, RCODE=0
        output.write(0x81) // 1000 0001
        output.write(0x80) // 1000 0000

        // QDCOUNT = 1
        output.write(0x00)
        output.write(0x01)

        // ANCOUNT = 1 (if A query) or 0
        val isTypeA = dnsPacket.questionType == 1
        output.write(0x00)
        output.write(if (isTypeA) 0x01 else 0x00)

        // NSCOUNT = 0, ARCOUNT = 0
        output.write(0x00)
        output.write(0x00)
        output.write(0x00)
        output.write(0x00)

        // Copy question section from original query
        for (i in 12 until dnsPacket.questionEndOffset) {
            output.write(originalQuery[i].toInt())
        }

        // Answer section (only for A queries)
        if (isTypeA) {
            // NAME pointer to question name at offset 12
            output.write(0xC0)
            output.write(0x0C)

            // TYPE A
            output.write(0x00)
            output.write(0x01)

            // CLASS IN
            output.write(0x00)
            output.write(0x01)

            // TTL = 300 seconds
            output.write(0x00)
            output.write(0x00)
            output.write(0x01)
            output.write(0x2C)

            // RDLENGTH = 4
            output.write(0x00)
            output.write(0x04)

            // RDATA = 0.0.0.0
            output.write(0x00)
            output.write(0x00)
            output.write(0x00)
            output.write(0x00)
        }

        return output.toByteArray()
    }
}
