package com.softwareloop.glo.model;

import lombok.Data;

import javax.xml.bind.annotation.XmlAccessType;
import javax.xml.bind.annotation.XmlAccessorType;
import javax.xml.bind.annotation.XmlAttribute;
import javax.xml.bind.annotation.XmlElement;
import java.util.List;

@Data
@XmlAccessorType(XmlAccessType.FIELD)
public class Game {

    @XmlAttribute
    private String name;

    @XmlAttribute
    private String sourcefile;

    @XmlAttribute
    private String isbios;

    @XmlAttribute
    private String cloneof;

    @XmlAttribute
    private String romof;

    @XmlAttribute
    private String sampleof;

    @XmlAttribute
    private String board;

    @XmlAttribute
    private String rebuildto;

    @XmlAttribute
    private String description;

    @XmlElement(name = "rom")
    private List<Rom> roms;

}
