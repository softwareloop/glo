package com.softwareloop.glo.model;

import lombok.Data;

import javax.xml.bind.annotation.XmlAccessType;
import javax.xml.bind.annotation.XmlAccessorType;
import javax.xml.bind.annotation.XmlAttribute;

@Data
@XmlAccessorType(XmlAccessType.FIELD)
public class Rom {

    @XmlAttribute
    private String name;

    @XmlAttribute
    private Integer size;

    @XmlAttribute
    private String crc;

    @XmlAttribute
    private String md5;

    @XmlAttribute
    private String sha1;

}
