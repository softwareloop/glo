package com.softwareloop.glo

object Log {

    var verbose = false

    //--------------------------------------------------------------------------
    // Public methods
    //--------------------------------------------------------------------------
    fun info(fmt: String, vararg args: Any?) {
        log(fmt, *args)
    }

    fun debug(fmt: String, vararg args: Any?) {
        if (verbose) {
            log(fmt, *args)
        }
    }

    //--------------------------------------------------------------------------
    // Private methods
    //--------------------------------------------------------------------------

    private fun log(fmt: String, vararg args: Any?) {
        System.out.println(fmt.format(*args))
    }

}