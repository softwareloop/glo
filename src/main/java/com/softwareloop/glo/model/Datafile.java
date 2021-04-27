package com.softwareloop.glo.model;

import lombok.Data;

import javax.xml.bind.annotation.XmlAccessType;
import javax.xml.bind.annotation.XmlAccessorType;
import javax.xml.bind.annotation.XmlElement;
import javax.xml.bind.annotation.XmlRootElement;
import java.util.List;

@Data
@XmlRootElement
@XmlAccessorType(XmlAccessType.FIELD)
public class Datafile {

    @XmlElement
    private Header header;

    @XmlElement(name = "game")
    private List<Game> games;

}
