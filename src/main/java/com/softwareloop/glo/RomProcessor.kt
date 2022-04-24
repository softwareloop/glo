package com.softwareloop.glo

import com.softwareloop.glo.dat.DatStore
import com.softwareloop.glo.dat.RomSummary
import com.softwareloop.glo.util.getBaseFilename
import com.softwareloop.glo.util.getFilenameExtension
import java.nio.file.FileSystems
import java.nio.file.Files
import java.nio.file.Path
import java.nio.file.StandardCopyOption


class RomProcessor(
    val datStore: DatStore,
    val indentation: String = ""
) {

    //--------------------------------------------------------------------------
    // Properties
    //--------------------------------------------------------------------------

    var renameEnabled = false
    var unzipEnabled = false

    private val matchedFiles: MutableMap<String, RomSummary> = HashMap()
    private val unmatchedFiles: MutableList<String> = ArrayList()
    private var nRenamedFiles = 0
    private var nUnzippedFiles = 0

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
            val fileExtension = fileName.getFilenameExtension().lowercase()
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
        val zipRomSummary = RomSummary()
        FileSystems.newFileSystem(zipFile, ClassLoader.getSystemClassLoader()).use {
            for (rootDirectory in it.rootDirectories) {
                val zipRomProcessor = RomProcessor(datStore, indentation + "    ")
                zipRomProcessor.renameEnabled = renameEnabled
                zipRomProcessor.processDir(rootDirectory)
                for ((romName, romSummary) in zipRomProcessor.matchedFiles) {
                    zipRomSummary.addAll(romSummary)
                    if (unzipEnabled) {
                        val fromPath = rootDirectory.resolve(romName)
                        val toPath = romDir.resolve(romName)
                        Log.info("%s    Extracting to: %s", indentation, romName)
                        Files.copy(fromPath, toPath, StandardCopyOption.REPLACE_EXISTING)
                        nUnzippedFiles++
                    }
                }
            }
        }
        if (zipRomSummary.isEmpty()) {
            Log.debug("%s    No match found", indentation)
            unmatchedFiles.add(zipName)
            return
        }
        val finalFileName = if (renameEnabled) {
            rename(zipRomSummary, romDir, zipName, true) { commonName ->
                val baseName = commonName.getBaseFilename()
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
        val md5s = Md5Processor.computeMd5s(romFile)
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
        return if (romSummary.containsRomName(fileName, ignoreExtension)) {
            Log.debug("%s    No need to rename", indentation)
            fileName
        } else {
            val commonName = romSummary.getCommonName()
            if (commonName == null) {
                Log.info("%s    Multiple matching rom names. Not renaming.", indentation)
                fileName
            } else {
                val newFileName = newFileNameSupplier.invoke(commonName)
                rename(romDir, fileName, newFileName)
                newFileName
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
        with(Log) {
            info("\nFile stats:")
            info("Processed: %s", matchedFiles.size + unmatchedFiles.size)
            info("Matched  : %s", matchedFiles.size)
            info("Unmatched: %s", unmatchedFiles.size)
            info("Renamed  : %s", nRenamedFiles)
        }
    }

}