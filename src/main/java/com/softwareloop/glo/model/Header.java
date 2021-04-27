package com.softwareloop.glo.model;

import lombok.Data;

import javax.xml.bind.annotation.XmlAccessType;
import javax.xml.bind.annotation.XmlAccessorType;
import javax.xml.bind.annotation.XmlElement;

@Data
@XmlAccessorType(XmlAccessType.FIELD)
public class Header {

    @XmlElement
    private String name;

    @XmlElement
    private String description;

    @XmlElement
    private String category;

    @XmlElement
    private String version;

    @XmlElement
    private String date;

    @XmlElement
    private String author;

    @XmlElement
    private String email;

    @XmlElement
    private String homepage;

    @XmlElement
    private String url;

    @XmlElement
    private String comment;

}
