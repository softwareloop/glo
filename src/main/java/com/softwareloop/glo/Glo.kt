package com.softwareloop.glo

import org.apache.commons.cli.*
import org.apache.commons.lang3.StringUtils
import java.nio.file.Files
import java.nio.file.Paths

object Glo {

    //--------------------------------------------------------------------------
    // Constants
    //--------------------------------------------------------------------------

    const val HELP_OPTION = "help"
    const val VERBOSE_OPTION = "verbose"
    const val RENAME_OPTION = "rename"
    const val DATDIR_OPTION = "datdir"
    const val DATDIR_ENV = "DATDIR"

    val options = Options().apply {
        addOption(HELP_OPTION, "print this usage information")
        addOption(VERBOSE_OPTION, "be extra verbose")
        addOption(RENAME_OPTION, "rename roms to official name")
        val datdir = Option.builder(DATDIR_OPTION)
            .hasArg()
            .argName("DIR")
            .desc("set the dat directory (overrides \$DATDIR)")
            .build()
        addOption(datdir)
    }

    //--------------------------------------------------------------------------
    // Public methods
    //--------------------------------------------------------------------------

    fun run(args: Array<String>) {
        try {
            val parser = DefaultParser()
            val commandLine = parser.parse(options, args)
            run(commandLine)
        } catch (exp: ParseException) {
            Log.info("Parsing failed.  Reason: {}", exp.message!!)
        }
    }

    fun run(commandLine: CommandLine) {
        if (commandLine.hasOption(HELP_OPTION)) {
            help()
            return
        }
        val verbose = commandLine.hasOption(VERBOSE_OPTION)
        if (verbose) {
            Log.verbose = true
        }
        var datDir = System.getenv(DATDIR_ENV)
        if (commandLine.hasOption(DATDIR_OPTION)) {
            datDir = commandLine.getOptionValue(DATDIR_OPTION)
        }
        if (StringUtils.isBlank(datDir)) {
            Log.info("No dat dir specified")
        }
        val datDirPath = Paths.get(datDir)
        val datStore = DatStore()
        Log.info("Loading dat files from: %s", datDir!!)
        datStore.loadDatDir(datDirPath)
        val romProcessor = RomProcessor(datStore)
        val renameEnabled = commandLine.hasOption(RENAME_OPTION)
        romProcessor.renameEnabled = renameEnabled

        val args = commandLine.args
        if (args.isEmpty()) {
            Log.info("No rom dirs provided")
            return
        }

        for (arg in args) {
            val romDir = Paths.get(arg)
            if (!Files.isDirectory(romDir)) {
                Log.info("Not a directory: %s", romDir)
                continue
            }
            Log.info("\nProcessing rom dir: %s", arg)
            romProcessor.processDir(romDir)
        }
        romProcessor.printUnmatched()
        romProcessor.printStats()
    }

    fun help() {
        val formatter = HelpFormatter()
        formatter.printHelp("glo.sh [OPTION]... [ROMDIR]...", options)
    }

    //--------------------------------------------------------------------------
    // Main
    //--------------------------------------------------------------------------

    @JvmStatic
    fun main(args: Array<String>) {
        run(args)
    }
}




