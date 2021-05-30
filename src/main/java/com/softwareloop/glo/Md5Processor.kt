package com.softwareloop.glo

import java.io.BufferedInputStream
import java.nio.file.Files
import java.nio.file.Path
import java.security.MessageDigest
import java.util.*
import javax.xml.bind.DatatypeConverter

object Md5Processor {

    val inesStart = byteArrayOf(0x4E, 0x45, 0x53, 0x1A)

    fun computeMd5s(romFile: Path): List<String> {
        val fullMd = MessageDigest.getInstance("MD5")
        val headerlessMd = MessageDigest.getInstance("MD5")
        val buffer = ByteArray(1024)
        BufferedInputStream(Files.newInputStream(romFile)).use {
            // read the header
            var len = it.read(buffer)
            var headerLength = detectHeaderLength(buffer)
            while (len > 0) {
                fullMd.update(buffer, 0, len)
                if (headerLength != null) {
                    headerlessMd.update(buffer, headerLength, len - headerLength)
                    headerLength = 0
                }
                len = it.read(buffer)
            }
            val result = ArrayList<String>()
            result.add(getStringDigest(fullMd))
            if (headerLength != null) {
                result.add(getStringDigest(headerlessMd))
            }
            return result
        }
    }

    private fun getStringDigest(md: MessageDigest): String {
        return DatatypeConverter.printHexBinary(md.digest()).uppercase(Locale.getDefault())
    }

    private fun detectHeaderLength(buffer: ByteArray): Int? {
        if (buffer.size < 16) {
            return 0
        }
        var allMatch = true
        for (pos in 0..3) {
            if (buffer[pos] != inesStart[pos]) allMatch = false
        }
        return if (allMatch) 16 else null
    }

}