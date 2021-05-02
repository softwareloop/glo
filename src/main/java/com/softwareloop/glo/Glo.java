package com.softwareloop.glo;

import lombok.extern.slf4j.Slf4j;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;

@Slf4j
public class Glo {

    //--------------------------------------------------------------------------
    // Main
    //--------------------------------------------------------------------------

    public static void main(String[] args) throws IOException {
        if (args.length != 1) {
            log.error("Unexpected number of parameters");
            return;
        }
        String datDirPath = System.getenv("DAT_DIR_PATH");
        Path datDir = Paths.get(datDirPath);
        DatStore datStore = new DatStore();
        log.info("Loading dat files from: {}", datDirPath);
        datStore.loadDatDir(datDir);

        Path romDir = Paths.get(args[0]);
        if (!Files.isDirectory(romDir)) {
            log.warn("Not a directory: {}", romDir);
        }
        RomProcessor romProcessor = new RomProcessor(datStore, romDir);
        romProcessor.loadConfig();
        romProcessor.processDir(true);

    }

}
