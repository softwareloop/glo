package com.softwareloop.glo.util

fun String.getBaseFilename(): String {
    val dot: Int = lastIndexOf('.')
    return if (dot == -1) this else this.substring(0, dot)
}

fun String.getFilenameExtension(): String {
    val dot: Int = lastIndexOf('.')
    return if (dot == -1) "" else substring(dot + 1)
}