package com.softwareloop.glo

import com.softwareloop.glo.model.RomSummary
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

    companion object {
        val inesStart = byteArrayOf(0x4E, 0x45, 0x53, 0x1A)
    }

    //--------------------------------------------------------------------------
    // Properties
    //--------------------------------------------------------------------------

    var renameEnabled = false

    private val matchedFiles: MutableMap<String, List<RomSummary>> = HashMap()
    private val unmatchedFiles: MutableList<String> = ArrayList()
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
            val matched: List<RomSummary>
            if ("zip".equals(fileExtension)) {
                matched = processZip(romDir, fileName)
            } else {
                matched = processRom(romDir, fileName)
            }
            if (matched.isEmpty()) {
                unmatchedFiles.add(fileName)
            } else {
                matchedFiles[fileName] = matched
            }
        }
    }

    private fun processZip(
        romDir: Path,
        zipName: String
    ): List<RomSummary> {
        val zipFile = romDir.resolve(zipName)
        Log.info("Processing zip file: %s", zipName)
        val zipRomProcessor = RomProcessor(datStore)
        FileSystems.newFileSystem(zipFile, ClassLoader.getSystemClassLoader()).use {
            zipRomProcessor.renameEnabled = renameEnabled
            for (rootDirectory in it.rootDirectories) {
                zipRomProcessor.processDir(rootDirectory)
            }
        }
        val allRomSummaries = ArrayList<RomSummary>()
        for (matchedFile in zipRomProcessor.matchedFiles) {
            allRomSummaries.addAll(matchedFile.value)
        }
        if (renameEnabled && allRomSummaries.size == 1) {
            val romSummary = allRomSummaries[0]
            val romName = romSummary.romName
            val baseName = FilenameUtils.getBaseName(romName)
            val newZipName = "$baseName.zip"
            rename(romDir, zipName, newZipName)
        }
        return allRomSummaries
    }

    private fun processRom(
        romDir: Path,
        fileName: String
    ): List<RomSummary> {
        val romFile = romDir.resolve(fileName)
        val md5s = computeMd5s(romFile)
        var romSummaries: List<RomSummary>? = null
        for (md5 in md5s) {
            romSummaries = datStore.getRomSummaryByMd5(md5)
            if (romSummaries != null) {
                break
            }
        }
        if (romSummaries == null) {
            Log.debug("No match found")
            return Collections.emptyList()
        }
        if (renameEnabled) {
            val newFileNames: MutableSet<String> = HashSet()
            for (romSummary in romSummaries) {
                val newFileName = romSummary.romName
                newFileNames.add(newFileName)
            }
            if (newFileNames.size == 1) {
                val newFileName = newFileNames.iterator().next()
                rename(romDir, fileName, newFileName)
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
        return romSummaries
    }

    private fun rename(
        romDir: Path,
        fileName: String,
        newFileName: String
    ) {
        if (fileName == newFileName) {
            Log.debug("Name matches dat entry: %s", fileName)
        } else {
            Log.info("Renaming %s -> %s", fileName, newFileName)
            val romFile = romDir.resolve(fileName)
            val newRomFile = romDir.resolve(newFileName)
            Files.move(romFile, newRomFile, StandardCopyOption.REPLACE_EXISTING)
            nRenamedFiles++
        }
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
        Log.info("Processed: %s", matchedFiles.size + unmatchedFiles.size)
        Log.info("Matched  : %s", matchedFiles.size)
        Log.info("Unmatched: %s", unmatchedFiles.size)
        Log.info("Renamed  : %s", nRenamedFiles)
    }

    //--------------------------------------------------------------------------
    // Private methods
    //--------------------------------------------------------------------------

    private fun computeMd5s(romFile: Path): List<String> {
        val fullMd = MessageDigest.getInstance("MD5")
        val headerlessMd = MessageDigest.getInstance("MD5")
        val buffer = ByteArray(1024)
        BufferedInputStream(Files.newInputStream(romFile)).use { inputStream ->
            // read the header
            var len = inputStream.read(buffer)
            var headerLength = detectHeaderLength(buffer)
            while (len > 0) {
                fullMd.update(buffer, 0, len)
                headerlessMd.update(buffer, headerLength, len - headerLength)
                headerLength = 0
                len = inputStream.read(buffer)
            }
            val result = ArrayList<String>()
            result.add(getStringDigest(fullMd))
            result.add(getStringDigest(headerlessMd))
            return result
        }
    }

    private fun getStringDigest(md: MessageDigest): String {
        return DatatypeConverter.printHexBinary(md.digest()).uppercase(Locale.getDefault())
    }

    private fun detectHeaderLength(buffer: ByteArray): Int {
        if (buffer.size < 16) {
            return 0
        }
        var allMatch = true
        (0..3).forEach { pos -> if (buffer[pos] != inesStart[pos]) allMatch = false }
        return if (allMatch) 16 else 0
    }

}