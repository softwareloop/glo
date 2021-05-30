package com.softwareloop.glo

import com.softwareloop.glo.dat.DatStore
import com.softwareloop.glo.dat.RomSummary
import org.apache.commons.io.FilenameUtils
import java.io.BufferedInputStream
import java.nio.file.FileSystems
import java.nio.file.Files
import java.nio.file.Path
import java.nio.file.StandardCopyOption
import java.security.MessageDigest
import java.util.*
import javax.xml.bind.DatatypeConverter


class RomProcessor(
    val datStore: DatStore,
    val indentation: String
) {

    constructor(datStore: DatStore) : this(datStore, "")

    //--------------------------------------------------------------------------
    // Companion
    //--------------------------------------------------------------------------

    companion object {
        val inesStart = byteArrayOf(0x4E, 0x45, 0x53, 0x1A)
    }

    //--------------------------------------------------------------------------
    // Properties
    //--------------------------------------------------------------------------

    var renameEnabled = false
    var unzipEnabled = false

    private val matchedFiles: MutableMap<String, RomSummary> = HashMap()
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
            Log.info("%s%s", indentation, fileName)
            val fileExtension = FilenameUtils.getExtension(fileName).lowercase()
            if ("zip".equals(fileExtension)) {
                processZip(romDir, fileName)
            } else {
                processRom(romDir, fileName)
            }
        }
    }

    private fun processZip(
        romDir: Path,
        zipName: String
    ) {
        val zipFile = romDir.resolve(zipName)
        val zipRomProcessor = RomProcessor(datStore, indentation + "    ")
        FileSystems.newFileSystem(zipFile, ClassLoader.getSystemClassLoader()).use {
            zipRomProcessor.renameEnabled = renameEnabled
            for (rootDirectory in it.rootDirectories) {
                zipRomProcessor.processDir(rootDirectory)
            }
        }
        val zipRomSummary = RomSummary()
        for (matchedFile in zipRomProcessor.matchedFiles) {
            zipRomSummary.addAll(matchedFile.value)
        }
        if (zipRomSummary.isEmpty()) {
            Log.debug("%s    No match found", indentation)
            unmatchedFiles.add(zipName)
            return
        }
        val finalFileName = if (renameEnabled) {
            rename(zipRomSummary, romDir, zipName, true) { commonName ->
                val baseName = FilenameUtils.getBaseName(commonName)
                "$baseName.zip"
            }
        } else zipName
        matchedFiles[finalFileName] = zipRomSummary
    }

    private fun processRom(
        romDir: Path,
        fileName: String
    ) {
        val romFile = romDir.resolve(fileName)
        val md5s = computeMd5s(romFile)
        var romSummary: RomSummary? = null
        for (md5 in md5s) {
            romSummary = datStore.getRomSummaryByMd5(md5)
            if (romSummary != null) {
                break
            }
        }
        if (romSummary == null) {
            Log.debug("%s    No match found", indentation)
            unmatchedFiles.add(fileName)
            return
        }
        for (romEntry in romSummary.romEntries) {
            val newFileName = romEntry.romName
            Log.info("%s    %s [%s]", indentation, newFileName, romEntry.datName)
        }
        val finalFileName = if (renameEnabled) {
            rename(romSummary, romDir, fileName, false) { s -> s }
        } else fileName
        matchedFiles[finalFileName] = romSummary
    }

    private fun rename(
        romSummary: RomSummary,
        romDir: Path,
        fileName: String,
        ignoreExtension: Boolean,
        newFileNameSupplier: (String) -> String
    ): String {
        if (romSummary.containsRomName(fileName, ignoreExtension)) {
            Log.debug("%s    No need to rename", indentation)
            return fileName
        } else {
            val commonName = romSummary.getCommonName()
            if (commonName == null) {
                Log.info("%s    Multiple matching rom names. Not renaming.", indentation)
                return fileName
            } else {
                val newFileName = newFileNameSupplier.invoke(commonName)
                rename(romDir, fileName, newFileName)
                return newFileName
            }
        }
    }

    private fun rename(
        romDir: Path,
        fileName: String,
        newFileName: String
    ) {
        Log.info("%s    Renaming to: %s", indentation, newFileName)
        val romFile = romDir.resolve(fileName)
        val newRomFile = romDir.resolve(newFileName)
        Files.move(romFile, newRomFile, StandardCopyOption.REPLACE_EXISTING)
        nRenamedFiles++
    }

    fun printUnmatched() {
        Log.info("\n%sUnmatched files:", indentation)
        if (unmatchedFiles.isEmpty()) {
            Log.info("%sNo unmatched files", indentation)
        } else {
            for (romFile in unmatchedFiles) {
                Log.info("%s%s", indentation, romFile)
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