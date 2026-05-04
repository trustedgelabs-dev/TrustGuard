package com.trustedgelabs.trustguard.data.dns

class DnsPacket private constructor(
    val transactionId: Int,
    val flags: Int,
    val questionCount: Int,
    val questionDomain: String,
    val questionType: Int,
    val questionClass: Int,
    val questionEndOffset: Int,
    val rawData: ByteArray
) {
    companion object {
        fun parse(data: ByteArray): DnsPacket? {
            if (data.size < 12) return null

            val transactionId = ((data[0].toInt() and 0xFF) shl 8) or (data[1].toInt() and 0xFF)
            val flags = ((data[2].toInt() and 0xFF) shl 8) or (data[3].toInt() and 0xFF)
            val questionCount = ((data[4].toInt() and 0xFF) shl 8) or (data[5].toInt() and 0xFF)

            if (questionCount < 1) return null

            // Parse domain name starting at offset 12
            val domainBuilder = StringBuilder()
            var offset = 12

            while (offset < data.size) {
                val labelLength = data[offset].toInt() and 0xFF
                if (labelLength == 0) {
                    offset++
                    break
                }
                if (offset + labelLength + 1 > data.size) return null

                if (domainBuilder.isNotEmpty()) domainBuilder.append('.')
                for (i in 1..labelLength) {
                    domainBuilder.append(data[offset + i].toInt().toChar())
                }
                offset += labelLength + 1
            }

            if (offset + 4 > data.size) return null

            val qType = ((data[offset].toInt() and 0xFF) shl 8) or (data[offset + 1].toInt() and 0xFF)
            val qClass = ((data[offset + 2].toInt() and 0xFF) shl 8) or (data[offset + 3].toInt() and 0xFF)
            val questionEndOffset = offset + 4

            return DnsPacket(
                transactionId = transactionId,
                flags = flags,
                questionCount = questionCount,
                questionDomain = domainBuilder.toString().lowercase(),
                questionType = qType,
                questionClass = qClass,
                questionEndOffset = questionEndOffset,
                rawData = data
            )
        }
    }
}
