package com.softwareloop.glo;

import ch.qos.logback.classic.Level;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.cli.*;
import org.apache.commons.lang3.StringUtils;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;

@Slf4j
public class Glo {

    //--------------------------------------------------------------------------
    // Constants
    //--------------------------------------------------------------------------

    public static final String HELP_OPTION = "help";
    public static final String VERBOSE_OPTION = "verbose";
    public static final String RENAME_OPTION = "rename";
    public static final String DATDIR_OPTION = "datdir";
    public static final String DATDIR_ENV = "DATDIR";

    //--------------------------------------------------------------------------
    // Main
    //--------------------------------------------------------------------------

    public static void main(String[] args) throws IOException {
        Options options = createOptions();
        CommandLineParser parser = new DefaultParser();
        try {
            CommandLine commandLine = parser.parse(options, args);
            main(options, commandLine);
        } catch (ParseException exp) {
            log.error("Parsing failed.  Reason: {}", exp.getMessage());
        }
    }

    //--------------------------------------------------------------------------
    // Private methods
    //--------------------------------------------------------------------------

    private static void main(
            Options options,
            CommandLine commandLine
    ) throws IOException {
        if (commandLine.hasOption(HELP_OPTION)) {
            help(options);
        }

        boolean verbose = commandLine.hasOption(VERBOSE_OPTION);
        if (verbose) {
            ch.qos.logback.classic.Logger logger =
                    (ch.qos.logback.classic.Logger) LoggerFactory.getLogger("com.softwareloop.glo");
            logger.setLevel(Level.DEBUG);
            log.debug("DEBUG");
        }

        String[] args = commandLine.getArgs();
        if (args.length == 0) {
            log.info("No game dirs provided");
            return;
        }
        String datDir = System.getenv(DATDIR_ENV);
        if (commandLine.hasOption(DATDIR_OPTION)) {
            datDir = commandLine.getOptionValue(DATDIR_OPTION);
        }
        if (StringUtils.isBlank(datDir)) {
            log.error("No dat dir specified");
        }
        Path datDirPath = Paths.get(datDir);
        DatStore datStore = new DatStore();
        log.info("Loading dat files from: {}", datDir);
        datStore.loadDatDir(datDirPath);

        boolean renameEnabled = commandLine.hasOption(RENAME_OPTION);

        for (String arg : args) {
            Path romDir = Paths.get(arg);
            if (!Files.isDirectory(romDir)) {
                log.warn("Not a directory: {}", romDir);
            }
            RomProcessor romProcessor = new RomProcessor(datStore, romDir);
            romProcessor.loadConfig();
            romProcessor.processDir(renameEnabled);
        }
    }

    private static Options createOptions() {
        Options options = new Options();
        options.addOption(HELP_OPTION, "print this message");
        options.addOption(VERBOSE_OPTION, "be extra verbose");
        options.addOption(RENAME_OPTION, "rename games to official name");
        Option datdir = Option.builder(DATDIR_OPTION)
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
