package com.softwareloop.glo;

import com.softwareloop.glo.model.*;
import lombok.SneakyThrows;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.io.FilenameUtils;
import org.xml.sax.InputSource;
import org.xml.sax.XMLReader;

import javax.xml.bind.JAXBContext;
import javax.xml.bind.Unmarshaller;
import javax.xml.parsers.SAXParserFactory;
import javax.xml.transform.sax.SAXSource;
import java.io.IOException;
import java.io.Reader;
import java.nio.charset.StandardCharsets;
import java.nio.file.DirectoryStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

@Slf4j
public class DatStore {

    //--------------------------------------------------------------------------
    // Constants
    //--------------------------------------------------------------------------

    public static final Pattern MD5_PATTERN = Pattern.compile("[A-F0-9]{32}");

    //--------------------------------------------------------------------------
    // Fields
    //--------------------------------------------------------------------------

    private final JAXBContext jaxbContext;
    private final SAXParserFactory spf;
    private final Map<String, List<RomSummary>> md5Map;

    //--------------------------------------------------------------------------
    // Constructors
    //--------------------------------------------------------------------------

    @SneakyThrows
    public DatStore() {
        jaxbContext = JAXBContext.newInstance(Datafile.class);
        spf = SAXParserFactory.newInstance();
        spf.setFeature("http://apache.org/xml/features/nonvalidating/load-external-dtd", false);
        spf.setFeature("http://xml.org/sax/features/validation", false);
        md5Map = new HashMap<>();
    }


    //--------------------------------------------------------------------------
    // Public methods
    //--------------------------------------------------------------------------

    public void loadDatDir(Path datDir) throws IOException {
        try (DirectoryStream<Path> stream = Files.newDirectoryStream(datDir)) {
            for (Path datFile : stream) {
                log.debug("Reading: {}", datFile);
                String fileName = datFile.getFileName().toString();
                String extension = FilenameUtils.getExtension(fileName);
                if (Files.isRegularFile(datFile) && "dat".equalsIgnoreCase(extension)) {
                    Datafile datafile = loadDatFile(datFile);
                    add(datafile);
                }
            }
        }
    }

    @SneakyThrows
    public Datafile loadDatFile(Path datFile) {
        try (Reader reader = Files.newBufferedReader(datFile, StandardCharsets.UTF_8)) {
            XMLReader xmlReader = spf.newSAXParser().getXMLReader();
            InputSource inputSource = new InputSource(reader);
            SAXSource source = new SAXSource(xmlReader, inputSource);
            Unmarshaller jaxbUnmarshaller = jaxbContext.createUnmarshaller();
            return (Datafile) jaxbUnmarshaller.unmarshal(source);
        }
    }

    public void add(Datafile datafile) {
        Header header = datafile.getHeader();
        String datName = header.getName();
        for (Game game : datafile.getGames()) {
            for (Rom rom : game.getRoms()) {
                String romMd5 = rom.getMd5();
                if (romMd5 == null) {
                    log.trace("md5 is null, skipping");
                    continue;
                }
                romMd5 = romMd5.toUpperCase();
                Matcher md5Matcher = MD5_PATTERN.matcher(romMd5);
                if (!md5Matcher.matches()) {
                    log.trace("md5 has invalid format, skipping");
                    continue;
                }
                RomSummary romSummary = new RomSummary();
                romSummary.setDatName(datName);
                romSummary.setRomName(rom.getName());
                romSummary.setRomSize(rom.getSize());
                romSummary.setRomMd5(romMd5);
                List<RomSummary> romSummaries =
                        md5Map.computeIfAbsent(romMd5, k -> new ArrayList<>());
                if (!romSummaries.contains(romSummary)) {
                    romSummaries.add(romSummary);
                }
            }
        }
    }

    public List<RomSummary> getRomSummaryByMd5(String md5) {
        return md5Map.get(md5);
    }


    //--------------------------------------------------------------------------
    // Methods
    //--------------------------------------------------------------------------

}
