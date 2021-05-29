package com.softwareloop.glo.dat.model

import javax.xml.bind.annotation.XmlAccessType
import javax.xml.bind.annotation.XmlAccessorType
import javax.xml.bind.annotation.XmlElement

@XmlAccessorType(XmlAccessType.FIELD)
class Header {

    @XmlElement
    val name: String? = null

    @XmlElement
    val description: String? = null

    @XmlElement
    val category: String? = null

    @XmlElement
    val version: String? = null

    @XmlElement
    val date: String? = null

    @XmlElement
    val author: String? = null

    @XmlElement
    val email: String? = null

    @XmlElement
    val homepage: String? = null

    @XmlElement
    val url: String? = null

    @XmlElement
    val comment: String? = null

}