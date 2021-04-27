package com.softwareloop.glo;

import com.softwareloop.glo.model.Datafile;
import lombok.SneakyThrows;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.io.FilenameUtils;
import org.apache.commons.lang3.ArrayUtils;
import org.apache.commons.lang3.StringUtils;
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
import java.nio.file.Paths;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

@Slf4j
public class Glo {

    public static final String[] EXTENSIONS_TO_SKIP = { "xml" };
    public static final Pattern BASE_NAME_PATTERN = Pattern.compile("(\\d{2,}\\s+)?(.*)");

    public static boolean RENAME_ENABLED = false;

    //--------------------------------------------------------------------------
    // Fields
    //--------------------------------------------------------------------------

    JAXBContext jaxbContext;
    SAXParserFactory spf;

    //--------------------------------------------------------------------------
    // Constructor
    //--------------------------------------------------------------------------

    @SneakyThrows
    public Glo() {
        jaxbContext = JAXBContext.newInstance(Datafile.class);
        spf = SAXParserFactory.newInstance();
        spf.setFeature("http://apache.org/xml/features/nonvalidating/load-external-dtd", false);
        spf.setFeature("http://xml.org/sax/features/validation", false);
    }

    //--------------------------------------------------------------------------
    // Main
    //--------------------------------------------------------------------------

    public static void main(String[] args) throws IOException {
        String datDirPath = System.getenv("DAT_DIR_PATH");
        Path datDir = Paths.get(datDirPath);
        Glo glo = new Glo();
        glo.processDir(datDir);
    }

    //--------------------------------------------------------------------------
    // Methods
    //--------------------------------------------------------------------------

    private void processFile(Path inPath) throws IOException {
        String fileName = inPath.getFileName().toString();
        if (fileName.startsWith(".")) {
            log.debug("Skipping " + fileName);
            return;
        }
        String baseName = FilenameUtils.getBaseName(fileName);
        String extension = FilenameUtils.getExtension(fileName);
        if (ArrayUtils.contains(EXTENSIONS_TO_SKIP, extension.toLowerCase())) {
            log.debug("Skipping " + fileName);
            return;
        }
        if (StringUtils.isEmpty(extension)) {
            log.debug("Skipping " + fileName);
            return;
        }
        Matcher matcher = BASE_NAME_PATTERN.matcher(baseName);
        if (matcher.matches()) {
            String group = matcher.group(2);
            String outFileName = String.format(
                    "%s.%s",
                    StringUtils.trim(group),
                    extension);
            if (outFileName.equals(fileName)) {
                log.info(fileName + ": unmodifiled");
            } else {
                log.info(fileName + " -> " + outFileName);
                if (RENAME_ENABLED) {
                    Path outPath = inPath.getParent().resolve(outFileName);
                    Files.move(inPath, outPath);
                }
            }
        } else {
            log.debug("Unmatched pattern" + fileName);
        }
    }

    private void processDir(Path dirPath) throws IOException {
        try (DirectoryStream<Path> stream = Files.newDirectoryStream(dirPath)) {
            for (Path path : stream) {
                String fileName = path.getFileName().toString();
                String extension = FilenameUtils.getExtension(fileName);
                if ("dat".equalsIgnoreCase(extension)) {
                    processDat(path);
                }
            }
        }
    }

    @SneakyThrows
    private void processDat(Path path) {
        log.info("Reading: {}", path);
        try (Reader reader = Files.newBufferedReader(path, StandardCharsets.UTF_8)) {
            XMLReader xmlReader = spf.newSAXParser().getXMLReader();
            InputSource inputSource = new InputSource(reader);
            SAXSource source = new SAXSource(xmlReader, inputSource);
            Unmarshaller jaxbUnmarshaller = jaxbContext.createUnmarshaller();
            Datafile datafile = (Datafile) jaxbUnmarshaller.unmarshal(source);
        }
    }
}
