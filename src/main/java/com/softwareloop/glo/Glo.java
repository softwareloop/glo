package com.softwareloop.glo;

import lombok.SneakyThrows;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.io.FilenameUtils;
import org.apache.commons.lang3.ArrayUtils;
import org.apache.commons.lang3.StringUtils;

import java.io.IOException;
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

    private final DatStore datStore;

    //--------------------------------------------------------------------------
    // Constructor
    //--------------------------------------------------------------------------

    @SneakyThrows
    public Glo(DatStore datStore) {
        this.datStore = datStore;
    }

    //--------------------------------------------------------------------------
    // Main
    //--------------------------------------------------------------------------

    public static void main(String[] args) throws IOException {
        String datDirPath = System.getenv("DAT_DIR_PATH");
        Path datDir = Paths.get(datDirPath);
        DatStore datStore = new DatStore();
        datStore.loadDatDir(datDir);
        Glo glo = new Glo(datStore);
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

}
