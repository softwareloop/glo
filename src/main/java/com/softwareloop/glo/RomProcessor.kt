package com.softwareloop.glo

import org.apache.commons.io.FilenameUtils
import java.io.BufferedInputStream
import java.nio.file.FileSystems
import java.nio.file.Files
import java.nio.file.Path
import java.nio.file.StandardCopyOption
import java.security.MessageDigest
import java.util.*
import javax.xml.bind.DatatypeConverter


class RomProcessor(private val datStore: DatStore) {

    var renameEnabled = false

    private val matchedFiles: MutableList<String> = ArrayList()
    private val unmatchedFiles: MutableList<String> = ArrayList()
    private var nProcessedFiles = 0
    private var nRenamedFiles = 0

    //--------------------------------------------------------------------------
    // Public methods
    //--------------------------------------------------------------------------

    fun processDir(romDir: Path) {
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
            val fileExtension = FilenameUtils.getExtension(fileName).lowercase()
            val matched: Boolean
            if ("zip".equals(fileExtension)) {
                matched = processZip(romDir, fileName)
            } else {
                matched = processRom(romDir, fileName)
            }
            if (matched) {
                matchedFiles.add(fileName)
            } else {
                unmatchedFiles.add(fileName)
            }
        }
    }

    private fun processZip(
        romDir: Path,
        zipName: String
    ): Boolean {
        val zipFile = romDir.resolve(zipName)
        Log.info("Processing zip file: %s", zipName)
        val fs = FileSystems.newFileSystem(zipFile, ClassLoader.getSystemClassLoader())
        val zipRomProcessor = RomProcessor(datStore)
        for (rootDirectory in fs.rootDirectories) {
            zipRomProcessor.processDir(rootDirectory)
            zipRomProcessor.printStats()
        }
        return false
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
            return false
        }
        if (renameEnabled) {
            val newFileNames: MutableSet<String> = HashSet()
            for (romSummary in romSummaries) {
                val newFileName = romSummary.romName
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

    fun printUnmatched() {
        Log.info("\nUnmatched files:")
        if (unmatchedFiles.isEmpty()) {
            Log.info("No unmatched files")
        } else {
            for (romFile in unmatchedFiles) {
                Log.info(romFile)
            }
        }
    }

    fun printStats() {
        Log.info("\nFile stats:")
        Log.info("Processed: %s", nProcessedFiles)
        Log.info("Matched  : %s", matchedFiles.size)
        Log.info("Unmatched: %s", unmatchedFiles.size)
        Log.info("Renamed  : %s", nRenamedFiles)
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

}