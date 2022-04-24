package com.softwareloop.glo.dat.model

import jakarta.xml.bind.annotation.XmlAccessType
import jakarta.xml.bind.annotation.XmlAccessorType
import jakarta.xml.bind.annotation.XmlAttribute
import jakarta.xml.bind.annotation.XmlElement

@XmlAccessorType(XmlAccessType.FIELD)
class Game {

    @XmlAttribute
    val name: String? = null

    @XmlAttribute
    val sourcefile: String? = null

    @XmlAttribute
    val isbios: String? = null

    @XmlAttribute
    val cloneof: String? = null

    @XmlAttribute
    val romof: String? = null

    @XmlAttribute
    val sampleof: String? = null

    @XmlAttribute
    val board: String? = null

    @XmlAttribute
    val rebuildto: String? = null

    @XmlAttribute
    val description: String? = null

    @XmlElement(name = "rom")
    val roms: List<Rom>? = null

}