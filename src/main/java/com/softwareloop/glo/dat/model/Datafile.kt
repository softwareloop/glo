package com.softwareloop.glo.dat.model

import javax.xml.bind.annotation.XmlAccessType
import javax.xml.bind.annotation.XmlAccessorType
import javax.xml.bind.annotation.XmlElement
import javax.xml.bind.annotation.XmlRootElement

@XmlRootElement
@XmlAccessorType(XmlAccessType.FIELD)
class Datafile {

    @XmlElement
    val header: Header? = null

    @XmlElement(name = "game")
    val games: List<Game>? = null

}