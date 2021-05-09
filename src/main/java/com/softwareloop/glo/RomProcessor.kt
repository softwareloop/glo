package com.softwareloop.glo

import java.io.BufferedInputStream
import java.nio.file.Files
import java.nio.file.Path
import java.nio.file.StandardCopyOption
import java.security.MessageDigest
import java.util.*
import javax.xml.bind.DatatypeConverter

class RomProcessor(private val datStore: DatStore) {

    var renameEnabled = false

    private var nProcessedFiles = 0
    private var nMatchedFiles = 0
    private var nUnmatchedFiles = 0
    private var nRenamedFiles = 0

    //--------------------------------------------------------------------------
    // Public methods
    //--------------------------------------------------------------------------

    fun processDir(romDir: Path) {
        val unmatched: MutableList<String> = ArrayList()
        val fileNames: MutableList<String> = ArrayList()
        Files.newDirectoryStream(romDir).use { stream ->
            for (romFile in stream) {
                val fileName = romFile.fileName.toString()
                if (Files.isRegularFile(romFile) && !fileName.startsWith(".")) {
                    fileNames.add(fileName)
                }
            }
        }
        fileNames.sortWith { obj: String, str: String? -> obj.compareTo(str!!, ignoreCase = true) }
        for (fileName in fileNames) {
            val matched = processRom(romDir, fileName)
            if (!matched) {
                unmatched.add(fileName)
            }
        }
        if (!unmatched.isEmpty()) {
            Log.info("Unmatched files:")
            for (romFile in unmatched) {
                Log.info(romFile)
            }
        }
    }

    private fun processRom(
        romDir: Path,
        fileName: String
    ): Boolean {
        nProcessedFiles++
        val romFile = romDir.resolve(fileName)
        val md5 = computeMd5(romFile)
        val romSummaries = datStore.getRomSummaryByMd5(md5)
        if (romSummaries == null) {
            Log.debug("No match found")
            nUnmatchedFiles++
            return false
        }
        nMatchedFiles++
        if (renameEnabled) {
            val newFileNames: MutableSet<String> = HashSet()
            for (romSummary in romSummaries) {
                val newFileName = romSummary.romName ?: continue
                newFileNames.add(newFileName)
            }
            if (newFileNames.size == 1) {
                val newFileName = newFileNames.iterator().next()
                if (fileName == newFileName) {
                    Log.debug("Name matches dat entry: %s", fileName)
                } else {
                    Log.info("Renaming %s -> %s", fileName, newFileName)
                    val newRomFile = romDir.resolve(newFileName)
                    Files.move(romFile, newRomFile, StandardCopyOption.REPLACE_EXISTING)
                    nRenamedFiles++
                }
            } else {
                if (newFileNames.contains(fileName)) {
                    Log.debug("Name matches dat entry: %s", fileName)
                } else {
                    Log.info("Skipping %s - multiple matching rom names:", fileName)
                    for (romSummary in romSummaries) {
                        val newFileName = romSummary.romName
                        Log.info("    %s [%s]", newFileName, romSummary.datName)
                    }
                }
            }
        } else {
            Log.info(fileName)
            for (romSummary in romSummaries) {
                val newFileName = romSummary.romName
                Log.info("    %s [%s]", newFileName, romSummary.datName)
            }
        }
        return true
    }

    //--------------------------------------------------------------------------
    // Private methods
    //--------------------------------------------------------------------------

    private fun computeMd5(romFile: Path): String {
        val md = MessageDigest.getInstance("MD5")
        val buffer = ByteArray(1024)
        BufferedInputStream(Files.newInputStream(romFile)).use { inputStream ->
            var len: Int
            while (inputStream.read(buffer).also { len = it } > 0) {
                md.update(buffer, 0, len)
            }
            val digest = md.digest()
            return DatatypeConverter.printHexBinary(digest).uppercase(Locale.getDefault())
        }
    }

    fun printStats() {
        Log.info("\nFile stats:")
        Log.info("Processed: %s", nProcessedFiles)
        Log.info("Matched  : %s", nMatchedFiles)
        Log.info("Unmatched: %s", nUnmatchedFiles)
        Log.info("Renamed  : %s", nRenamedFiles)
    }

}