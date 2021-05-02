package com.softwareloop.glo;

import lombok.SneakyThrows;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.cli.*;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;

@Slf4j
public class Glo {
    public static final String HELP = "help";

    //--------------------------------------------------------------------------
    // Main
    //--------------------------------------------------------------------------

    public static void main(String[] args) throws IOException {
        Options options = createOptions();
        CommandLineParser parser = new DefaultParser();
        try {
            // parse the command line arguments
            CommandLine commandLine = parser.parse(options, args);
            main(options, commandLine);
        } catch (ParseException exp) {
            // oops, something went wrong
            log.error("Parsing failed.  Reason: {}", exp.getMessage());
        }
    }

    @SneakyThrows
    private static void main(Options options, CommandLine commandLine) {
        if (commandLine.hasOption(HELP)) {
            help(options);
        }

        String[] args = commandLine.getArgs();
        if (args.length == 0) {
            log.info("No game dirs provided");
            return;
        }
        String datDirPath = System.getenv("DAT_DIR_PATH");
        Path datDir = Paths.get(datDirPath);
        DatStore datStore = new DatStore();
        log.info("Loading dat files from: {}", datDirPath);
        datStore.loadDatDir(datDir);

        for (String arg : args) {
            Path romDir = Paths.get(arg);
            if (!Files.isDirectory(romDir)) {
                log.warn("Not a directory: {}", romDir);
            }
            RomProcessor romProcessor = new RomProcessor(datStore, romDir);
            romProcessor.loadConfig();
            romProcessor.processDir(true);
        }
    }

    //--------------------------------------------------------------------------
    // Private methods
    //--------------------------------------------------------------------------

    private static Options createOptions() {
        Options options = new Options();
        options.addOption(HELP, "print this message");
        options.addOption("verbose", "be extra verbose");
        options.addOption("rename", "rename games to official name");
        Option datdir = Option.builder("datdir")
                              .hasArg()
                              .argName("DIR")
                              .desc("set the dat directory (overrides $DATDIR)")
                              .build();
        options.addOption(datdir);
        return options;
    }

    private static void help(Options options) {
        HelpFormatter formatter = new HelpFormatter();
        formatter.printHelp("glo.sh [OPTION]... [GAMEDIR]...", options);
    }

}
