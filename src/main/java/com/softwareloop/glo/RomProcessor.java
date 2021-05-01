package com.softwareloop.glo;

import com.softwareloop.glo.model.RomSummary;
import lombok.SneakyThrows;
import lombok.extern.slf4j.Slf4j;

import javax.xml.bind.DatatypeConverter;
import java.io.BufferedInputStream;
import java.io.InputStream;
import java.nio.file.DirectoryStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.security.MessageDigest;
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

    //--------------------------------------------------------------------------
    // Constructors
    //--------------------------------------------------------------------------

    public RomProcessor(DatStore datStore) {
        this.datStore = datStore;
    }

    //--------------------------------------------------------------------------
    // Public methods
    //--------------------------------------------------------------------------

    @SneakyThrows
    public void processDir(Path romDir) {
        try (DirectoryStream<Path> stream = Files.newDirectoryStream(romDir)) {
            for (Path romFile : stream) {
                String fileName = romFile.getFileName().toString();
                if (Files.isRegularFile(romFile) && !fileName.startsWith(".")) {
                    processRom(romFile);
                }
            }
        }
    }

    private void processRom(Path romFile) {
        String fileName = romFile.getFileName().toString();
        log.info("Processing: {}", fileName);
        String md5 = computeMd5(romFile);
        List<RomSummary> romSummaries = datStore.getRomSummaryByMd5(md5);
        if (romSummaries == null) {
            log.info("    No match found");
            return;
        }
        RomSummary romSummary = romSummaries.get(0);
        log.info("    From dat file: {}", romSummary.getDatName());
        if (fileName.equalsIgnoreCase(romSummary.getRomName())) {
            log.info("    Name matches dat entry");
        } else {
            log.info("    Renaming to: {}", romSummary.getRomName());
        }
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
