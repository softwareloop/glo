package com.softwareloop.glo;

import com.softwareloop.glo.model.RomSummary;
import lombok.Getter;
import lombok.Setter;
import lombok.SneakyThrows;

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

    @Getter
    private int nProcessedFiles;
    @Getter
    private int nMatchedFiles;
    @Getter
    private int nUnmatchedFiles;
    @Getter
    private int nRenamedFiles;

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
            Log.info("Unmatched files:");
            for (String romFile : unmatched) {
                Log.info(romFile);
            }
        }
    }

    @SneakyThrows
    private boolean processRom(
            Path romDir,
            String fileName
    ) {
        nProcessedFiles++;
        Path romFile = romDir.resolve(fileName);
        String md5 = computeMd5(romFile);
        List<RomSummary> romSummaries = datStore.getRomSummaryByMd5(md5);
        if (romSummaries == null) {
            Log.debug("No match found");
            nUnmatchedFiles++;
            return false;
        }
        nMatchedFiles++;
        if (renameEnabled) {
            Set<String> newFileNames = new HashSet<>();
            for (RomSummary romSummary : romSummaries) {
                String newFileName = romSummary.getRomName();
                newFileNames.add(newFileName);
            }
            if (newFileNames.size() == 1) {
                String newFileName = newFileNames.iterator().next();
                if (fileName.equals(newFileName)) {
                    Log.debug("Name matches dat entry: {}", fileName);
                } else {
                    Log.info("Renaming {} -> {}", fileName, newFileName);
                    Path newRomFile = romDir.resolve(newFileName);
                    Files.move(romFile, newRomFile, StandardCopyOption.REPLACE_EXISTING);
                    nRenamedFiles++;
                }
            } else {
                if (newFileNames.contains(fileName)) {
                    Log.debug("Name matches dat entry: {}", fileName);
                } else {
                    Log.info("Skipping {} - multiple matching rom names:", fileName);
                    for (RomSummary romSummary : romSummaries) {
                        String newFileName = romSummary.getRomName();
                        Log.info("    {} [{}]", newFileName, romSummary.getDatName());
                    }
                }
            }
        } else {
            Log.info(fileName);
            for (RomSummary romSummary : romSummaries) {
                String newFileName = romSummary.getRomName();
                Log.info("    {} [{}]", newFileName, romSummary.getDatName());
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

    public void printStats() {
        Log.info("\nFile stats:");
        Log.info("Processed: {}", nProcessedFiles);
        Log.info("Matched  : {}", nMatchedFiles);
        Log.info("Unmatched: {}", nUnmatchedFiles);
        Log.info("Renamed  : {}", nRenamedFiles);
    }
}
