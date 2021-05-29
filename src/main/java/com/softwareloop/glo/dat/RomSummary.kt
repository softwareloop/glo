package com.softwareloop.glo.dat

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
}