package com.softwareloop.glo;

import lombok.Getter;
import lombok.Setter;

public class Log {

    @Getter
    @Setter
    private static boolean verbose;

    //--------------------------------------------------------------------------
    // Public methods
    //--------------------------------------------------------------------------

    public static void info(String fmt, Object... args) {
        log(fmt, args);
    }

    public static void debug(String fmt, Object... args) {
        if (verbose) {
            log(fmt, args);
        }
    }

    //--------------------------------------------------------------------------
    // Private methods
    //--------------------------------------------------------------------------

    private static void log(String fmt, Object[] args) {
        System.out.printf(fmt + "%n", args);
    }

}
