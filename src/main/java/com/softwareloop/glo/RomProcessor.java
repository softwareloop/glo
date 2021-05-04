package com.softwareloop.glo;

import com.softwareloop.glo.model.RomSummary;
import lombok.Getter;
import lombok.Setter;
import lombok.SneakyThrows;
import lombok.extern.slf4j.Slf4j;

import javax.xml.bind.DatatypeConverter;
import java.io.BufferedInputStream;
import java.io.InputStream;
import java.nio.file.DirectoryStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.security.MessageDigest;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

@Slf4j
public class RomProcessor {

    //--------------------------------------------------------------------------
    // Constants
    //--------------------------------------------------------------------------

    //--------------------------------------------------------------------------
    // Fields
    //--------------------------------------------------------------------------

    private final DatStore datStore;

    @Getter
    @Setter
    boolean renameEnabled;

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
        List<String> unmatched = new ArrayList<>();
        List<String> fileNames = new ArrayList<>();
        try (DirectoryStream<Path> stream = Files.newDirectoryStream(romDir)) {
            for (Path romFile : stream) {
                String fileName = romFile.getFileName().toString();
                if (Files.isRegularFile(romFile) && !fileName.startsWith(".")) {
                    fileNames.add(fileName);
                }
            }
        }
        fileNames.sort(String::compareToIgnoreCase);
        for (String fileName : fileNames) {
            boolean matched = processRom(romDir, fileName);
            if (!matched) {
                unmatched.add(fileName);
            }
        }
        if (!unmatched.isEmpty()) {
            log.info("Unmatched files:");
            for (String romFile : unmatched) {
                log.info(romFile);
            }
        }
    }

    @SneakyThrows
    private boolean processRom(
            Path romDir,
            String fileName
    ) {
        Path romFile = romDir.resolve(fileName);
        String md5 = computeMd5(romFile);
        List<RomSummary> romSummaries = datStore.getRomSummaryByMd5(md5);
        if (romSummaries == null) {
            log.debug("No match found");
            return false;
        }
        if (renameEnabled) {
            Set<String> newFileNames = new HashSet<>();
            for (RomSummary romSummary : romSummaries) {
                String newFileName = romSummary.getRomName();
                newFileNames.add(newFileName);
            }
            if (newFileNames.size() == 1) {
                String newFileName = newFileNames.iterator().next();
                if (fileName.equals(newFileName)) {
                    log.debug("Name matches dat entry: {}", fileName);
                } else {
                    log.info("Renaming {} -> {}", fileName, newFileName);
                    Path newRomFile = romDir.resolve(newFileName);
                    Files.move(romFile, newRomFile, StandardCopyOption.REPLACE_EXISTING);
                }
            } else {
                if (newFileNames.contains(fileName)) {
                    log.debug("Name matches dat entry: {}", fileName);
                } else {
                    log.info("Skipping {} - multiple matching rom names:", fileName);
                    for (RomSummary romSummary : romSummaries) {
                        String newFileName = romSummary.getRomName();
                        log.info("    {} [{}]", newFileName, romSummary.getDatName());
                    }
                }
            }
        } else {
            log.info(fileName);
            for (RomSummary romSummary : romSummaries) {
                String newFileName = romSummary.getRomName();
                log.info("    {} [{}]", newFileName, romSummary.getDatName());
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
