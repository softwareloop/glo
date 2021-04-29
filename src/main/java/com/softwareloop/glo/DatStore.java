package com.softwareloop.glo;

import com.softwareloop.glo.model.Datafile;
import lombok.SneakyThrows;
import lombok.extern.slf4j.Slf4j;
import org.xml.sax.InputSource;
import org.xml.sax.XMLReader;

import javax.xml.bind.JAXBContext;
import javax.xml.bind.Unmarshaller;
import javax.xml.parsers.SAXParserFactory;
import javax.xml.transform.sax.SAXSource;
import java.io.Reader;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;

@Slf4j
public class DatStore {

    //--------------------------------------------------------------------------
    // Constants
    //--------------------------------------------------------------------------

    //--------------------------------------------------------------------------
    // Fields
    //--------------------------------------------------------------------------

    final JAXBContext jaxbContext;
    final SAXParserFactory spf;

    //--------------------------------------------------------------------------
    // Constructors
    //--------------------------------------------------------------------------

    @SneakyThrows
    public DatStore() {
        jaxbContext = JAXBContext.newInstance(Datafile.class);
        spf = SAXParserFactory.newInstance();
        spf.setFeature("http://apache.org/xml/features/nonvalidating/load-external-dtd", false);
        spf.setFeature("http://xml.org/sax/features/validation", false);
    }


    //--------------------------------------------------------------------------
    // Interface implementations
    //--------------------------------------------------------------------------

    @SneakyThrows
    public void processDat(Path datDir) {
        log.info("Reading: {}", datDir);
        try (Reader reader = Files.newBufferedReader(datDir, StandardCharsets.UTF_8)) {
            XMLReader xmlReader = spf.newSAXParser().getXMLReader();
            InputSource inputSource = new InputSource(reader);
            SAXSource source = new SAXSource(xmlReader, inputSource);
            Unmarshaller jaxbUnmarshaller = jaxbContext.createUnmarshaller();
            Datafile datafile = (Datafile) jaxbUnmarshaller.unmarshal(source);
        }
    }

    //--------------------------------------------------------------------------
    // Methods
    //--------------------------------------------------------------------------

}
