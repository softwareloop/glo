package com.softwareloop.glo;

import com.softwareloop.glo.model.RomSummary;
import lombok.Getter;
import lombok.SneakyThrows;
import lombok.extern.slf4j.Slf4j;

import javax.xml.bind.DatatypeConverter;
import java.io.BufferedInputStream;
import java.io.InputStream;
import java.nio.file.DirectoryStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.security.MessageDigest;
import java.util.ArrayList;
import java.util.List;

@Slf4j
public class RomProcessor {

    //--------------------------------------------------------------------------
    // Constants
    //--------------------------------------------------------------------------

    //--------------------------------------------------------------------------
    // Fields
    //--------------------------------------------------------------------------

    private final DatStore datStore;
    private final Path romDir;

    private final List<String> datNames;
    private final List<RomSummary> overrides;

    @Getter
    private boolean initialized;

    //--------------------------------------------------------------------------
    // Constructors
    //--------------------------------------------------------------------------

    public RomProcessor(
            DatStore datStore,
            Path romDir
    ) {
        this.datStore = datStore;
        this.romDir = romDir;
        datNames = new ArrayList<>();
        overrides = new ArrayList<>();
        initialized = false;
    }

    //--------------------------------------------------------------------------
    // Public methods
    //--------------------------------------------------------------------------

    public void loadConfig() {
        Path configDir = romDir.resolve(".glo");
        if (!Files.isDirectory(configDir)) {
            log.warn("Config directory .glo does not exist. Re-run with --init");
            return;
        }
        initialized = true;
    }

    @SneakyThrows
    public void processDir(boolean dryRun) {
        List<Path> unmatchedRomFiles = new ArrayList<>();
        try (DirectoryStream<Path> stream = Files.newDirectoryStream(romDir)) {
            for (Path romFile : stream) {
                String fileName = romFile.getFileName().toString();
                if (Files.isRegularFile(romFile) && !fileName.startsWith(".")) {
                    boolean matched = processRom(fileName, dryRun);
                    if (!matched) {
                        unmatchedRomFiles.add(romFile);
                    }
                }
            }
        }
        if (!unmatchedRomFiles.isEmpty()) {
            log.info("Unmatched files:");
            unmatchedRomFiles.sort(Path::compareTo);
            for (Path romFile : unmatchedRomFiles) {
                log.info(romFile.getFileName().toString());
            }
        }
    }

    @SneakyThrows
    private boolean processRom(
            String fileName,
            boolean dryRun
    ) {
        log.debug("Processing: {}", fileName);
        Path romFile = romDir.resolve(fileName);
        String md5 = computeMd5(romFile);
        List<RomSummary> romSummaries = datStore.getRomSummaryByMd5(md5);
        if (romSummaries == null) {
            log.debug("No match found");
            return false;
        }
        RomSummary romSummary = romSummaries.get(0);
        log.debug("From dat file: {}", romSummary.getDatName());
        String newFileName = romSummary.getRomName();
        if (fileName.equalsIgnoreCase(newFileName)) {
            log.debug("Name matches dat entry");
        } else {
            log.info("Renaming {} -> {}", fileName, newFileName);
            Path newRomFile = romDir.resolve(newFileName);
            if (!dryRun) {
                Files.move(romFile, newRomFile);
            }
        }
        return true;
    }

    //--------------------------------------------------------------------------
    // Private methods
    //--------------------------------------------------------------------------

    @SneakyThrows
    private String computeMd5(Path romFile) {
        MessageDigest md = MessageDigest.getInstance("MD5");
        byte[] buffer = new byte[1024];
        try (InputStream is = new BufferedInputStream(Files.newInputStream(romFile))) {
            int len;
            while ((len = is.read(buffer)) > 0) {
                md.update(buffer, 0, len);
            }
            byte[] digest = md.digest();
            return DatatypeConverter.printHexBinary(digest).toUpperCase();
        }
    }

}
