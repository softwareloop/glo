package com.softwareloop.glo.dat.model

import jakarta.xml.bind.annotation.XmlAccessType
import jakarta.xml.bind.annotation.XmlAccessorType
import jakarta.xml.bind.annotation.XmlAttribute

@XmlAccessorType(XmlAccessType.FIELD)
class Rom {

    @XmlAttribute
    val name: String? = null

    @XmlAttribute
    val size: Int? = null

    @XmlAttribute
    val crc: String? = null

    @XmlAttribute
    val md5: String? = null

    @XmlAttribute
    val sha1: String? = null

}