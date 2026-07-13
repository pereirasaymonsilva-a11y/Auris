package com.goldensystem.auris.data.backup.format

import java.io.InputStream

class BackupFormatDetector {

    enum class Format {
        GABK_V3_ZIP,
        GABK_V2_GZIP,
        UNKNOWN
    }

    fun detect(header: ByteArray): Format {
        if (header.size < 4) return Format.UNKNOWN

        val hasGabkMagic = header[0] == 'G'.code.toByte() &&
            header[1] == 'A'.code.toByte() &&
            header[2] == 'B'.code.toByte() &&
            header[3] == 'K'.code.toByte()

        if (!hasGabkMagic) {
            return Format.UNKNOWN
        }

        if (header.size < 8) return Format.GABK_V2_GZIP

        val afterMagic0 = header[4]
        val afterMagic1 = header[5]

        // ZIP local file header: PK\x03\x04
        if (afterMagic0 == 'P'.code.toByte() && afterMagic1 == 'K'.code.toByte()) {
            return Format.GABK_V3_ZIP
        }

        // GZIP magic: 1f 8b
        if (afterMagic0 == 0x1f.toByte() && afterMagic1 == 0x8b.toByte()) {
            return Format.GABK_V2_GZIP
        }

        return Format.GABK_V2_GZIP
    }

    fun readHeader(inputStream: InputStream, size: Int = 8): ByteArray {
        val buffer = ByteArray(size)
        val bytesRead = inputStream.read(buffer)
        return if (bytesRead < size) buffer.copyOf(bytesRead) else buffer
    }

    companion object {
        val GABK_MAGIC = byteArrayOf(
            'G'.code.toByte(),
            'A'.code.toByte(),
            'B'.code.toByte(),
            'K'.code.toByte()
        )
        const val GABK_MAGIC_SIZE = 4
    }
}
