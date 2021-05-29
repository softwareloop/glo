package com.softwareloop.glo.dat

import org.apache.commons.io.FilenameUtils

class RomSummary {
    val romEntries = ArrayList<RomEntry>()

    fun contains(romEntry: RomEntry): Boolean {
        return romEntries.contains(romEntry)
    }

    fun add(romEntry: RomEntry) {
        romEntries.add(romEntry)
    }

    fun addAll(other: RomSummary) {
        romEntries.addAll(other.romEntries)
    }

    fun isEmpty(): Boolean {
        return romEntries.isEmpty()
    }

    fun getCommonName(): String? {
        val uniqueNames: MutableSet<String> = HashSet()
        for (romEntry in romEntries) {
            val name = romEntry.romName
            uniqueNames.add(name)
        }
        return if (uniqueNames.size == 1) uniqueNames.iterator().next() else null
    }

    fun containsRomName(romName: String): Boolean {
        for (romEntry in romEntries) {
            if (romName == romEntry.romName) {
                return true
            }
        }
        return false
    }

    fun containsRomBaseName(romBaseName: String): Boolean {
        for (romEntry in romEntries) {
            if (romBaseName == FilenameUtils.getBaseName(romEntry.romName)) {
                return true
            }
        }
        return false
    }
}