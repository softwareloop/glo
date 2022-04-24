package com.softwareloop.glo.dat.model

import jakarta.xml.bind.annotation.XmlAccessType
import jakarta.xml.bind.annotation.XmlAccessorType
import jakarta.xml.bind.annotation.XmlElement
import jakarta.xml.bind.annotation.XmlRootElement

@XmlRootElement
@XmlAccessorType(XmlAccessType.FIELD)
class Datafile {

    @XmlElement
    val header: Header? = null

    @XmlElement(name = "game")
    val games: List<Game>? = null

}