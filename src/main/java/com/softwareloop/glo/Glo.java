package com.softwareloop.glo;

import org.apache.commons.cli.*;
import org.apache.commons.lang3.StringUtils;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;

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
            Log.info("Parsing failed.  Reason: {}", exp.getMessage());
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
            return;
        }

        boolean verbose = commandLine.hasOption(VERBOSE_OPTION);
        if (verbose) {
            Log.setVerbose(true);
        }

        String[] args = commandLine.getArgs();
        if (args.length == 0) {
            Log.info("No rom dirs provided");
            return;
        }
        String datDir = System.getenv(DATDIR_ENV);
        if (commandLine.hasOption(DATDIR_OPTION)) {
            datDir = commandLine.getOptionValue(DATDIR_OPTION);
        }
        if (StringUtils.isBlank(datDir)) {
            Log.info("No dat dir specified");
        }
        Path datDirPath = Paths.get(datDir);
        DatStore datStore = new DatStore();
        Log.info("Loading dat files from: %s", datDir);
        datStore.loadDatDir(datDirPath);

        RomProcessor romProcessor = new RomProcessor(datStore);
        boolean renameEnabled = commandLine.hasOption(RENAME_OPTION);
        romProcessor.setRenameEnabled(renameEnabled);

        for (String arg : args) {
            Path romDir = Paths.get(arg);
            if (!Files.isDirectory(romDir)) {
                Log.info("Not a directory: %s", romDir);
                continue;
            }
            Log.info("\nProcessing rom dir: %s", arg);
            romProcessor.processDir(romDir);
        }
        romProcessor.printStats();
    }

    private static Options createOptions() {
        Options options = new Options();
        options.addOption(HELP_OPTION, "print this usage information");
        options.addOption(VERBOSE_OPTION, "be extra verbose");
        options.addOption(RENAME_OPTION, "rename roms to official name");
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
        formatter.printHelp("glo.sh [OPTION]... [ROMDIR]...", options);
    }

}
