package com.softwareloop.glo

import com.softwareloop.glo.model.Datafile
import com.softwareloop.glo.model.RomSummary
import org.apache.commons.io.FilenameUtils
import org.xml.sax.InputSource
import java.nio.charset.StandardCharsets
import java.nio.file.Files
import java.nio.file.Path
import java.util.*
import java.util.regex.Pattern
import javax.xml.bind.JAXBContext
import javax.xml.parsers.SAXParserFactory
import javax.xml.transform.sax.SAXSource

class DatStore {

    //--------------------------------------------------------------------------
    // Constants
    //--------------------------------------------------------------------------

    companion object {
        val MD5_PATTERN: Pattern = Pattern.compile("[A-F0-9]{32}")
        val ROM_NAME_ILLEGAL_PATTERN: Pattern = Pattern.compile("[/\\\\]")
        val jaxbContext: JAXBContext = JAXBContext.newInstance(Datafile::class.java)
        val spf: SAXParserFactory = SAXParserFactory.newInstance().apply {
            setFeature("http://apache.org/xml/features/nonvalidating/load-external-dtd", false)
            setFeature("http://xml.org/sax/features/validation", false)
        }
    }

    //--------------------------------------------------------------------------
    // Fields
    //--------------------------------------------------------------------------

    val md5Map = HashMap<String, MutableList<RomSummary>>()

    //--------------------------------------------------------------------------
    // Public methods
    //--------------------------------------------------------------------------

    fun loadDatDir(datDir: Path) {
        Files.newDirectoryStream(datDir).use { stream ->
            var nDats = 0
            for (datFile in stream) {
                Log.debug("Reading: %s", datFile)
                val fileName = datFile.fileName.toString()
                val extension = FilenameUtils.getExtension(fileName)
                if (Files.isRegularFile(datFile) && "dat".equals(extension, ignoreCase = true)) {
                    val datafile = loadDatFile(datFile)
                    add(datafile)
                    nDats++
                }
            }
            Log.info("Loaded %d dat files and %d entries", nDats, md5Map.size)
        }
    }

    fun loadDatFile(datFile: Path): Datafile {
        Files.newBufferedReader(datFile, StandardCharsets.UTF_8).use { reader ->
            val xmlReader = spf.newSAXParser().xmlReader
            val inputSource = InputSource(reader)
            val source = SAXSource(xmlReader, inputSource)
            val jaxbUnmarshaller = jaxbContext.createUnmarshaller()
            return jaxbUnmarshaller.unmarshal(source) as Datafile
        }
    }

    fun add(datafile: Datafile) {
        val header = datafile.header ?: return
        val datName = header.name ?: return
        val games = datafile.games ?: return
        for (game in games) {
            val roms = game.roms ?: return
            for (rom in roms) {
                val romName = rom.name ?: continue
                val romNameMatcher = ROM_NAME_ILLEGAL_PATTERN.matcher(romName)
                if (romNameMatcher.find()) {
                    Log.debug("Rom name '%s'contains illegal characters, skipping.", romName)
                }
                var romMd5 = rom.md5
                if (romMd5 == null) {
                    Log.debug("md5 is null, skipping")
                    continue
                }
                romMd5 = romMd5.uppercase(Locale.getDefault())
                val md5Matcher = MD5_PATTERN.matcher(romMd5)
                if (!md5Matcher.matches()) {
                    Log.debug("md5 has invalid format, skipping")
                    continue
                }
                val romSummary = RomSummary(datName, romName)
                val romSummaries = md5Map.computeIfAbsent(romMd5) { k: String? -> ArrayList() }
                if (!romSummaries.contains(romSummary)) {
                    romSummaries.add(romSummary)
                }
            }
        }
    }

    fun getRomSummaryByMd5(md5: String): List<RomSummary>? {
        return md5Map[md5]
    }

}