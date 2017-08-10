package com.jsonde.gui.configuration;

import javax.xml.bind.JAXBContext;
import javax.xml.bind.JAXBException;
import javax.xml.bind.Marshaller;
import javax.xml.bind.Unmarshaller;
import javax.xml.bind.annotation.XmlAccessType;
import javax.xml.bind.annotation.XmlAccessorType;
import javax.xml.bind.annotation.XmlElement;
import javax.xml.bind.annotation.XmlRootElement;
import java.io.File;

/**
 * Commenti Javadoc
 * @author gabriele
 *
 */
@XmlAccessorType(XmlAccessType.FIELD)
@XmlRootElement(name = "session")
public class SessionConfiguration {

    @XmlElement(name = "database-file-name")
    private String databaseFileName;

    public String getDatabaseFileName() {
        return databaseFileName;
    }

    public void setDatabaseFileName(String databaseFileName) {
        this.databaseFileName = databaseFileName;
    }

    public static SessionConfiguration loadSessionConfiguration(String fileName) throws SessionConfigurationException {
        return loadSessionConfiguration(new File(fileName));
    }

    public static SessionConfiguration loadSessionConfiguration(File file) throws SessionConfigurationException {

        try {
            JAXBContext jaxbContext = JAXBContext.newInstance(SessionConfiguration.class);

            Unmarshaller unmarshaller = jaxbContext.createUnmarshaller();

            DocumentBuilderFactory dbf = DocumentBuilderFactory.newInstance();
            String FEATURE = null;
            try {
              // This is the PRIMARY defense. If DTDs (doctypes) are disallowed, almost all XML entity attacks are prevented
              // Xerces 2 only - http://xerces.apache.org/xerces2-j/features.html#disallow-doctype-decl
              FEATURE = "http://apache.org/xml/features/disallow-doctype-decl";
              dbf.setFeature(FEATURE, true);
        
              // If you can't completely disable DTDs, then at least do the following:
              // Xerces 1 - http://xerces.apache.org/xerces-j/features.html#external-general-entities
              // Xerces 2 - http://xerces.apache.org/xerces2-j/features.html#external-general-entities
              // JDK7+ - http://xml.org/sax/features/external-general-entities    
              FEATURE = "http://xml.org/sax/features/external-general-entities";
              dbf.setFeature(FEATURE, false);
        
              // Xerces 1 - http://xerces.apache.org/xerces-j/features.html#external-parameter-entities
              // Xerces 2 - http://xerces.apache.org/xerces2-j/features.html#external-parameter-entities
              // JDK7+ - http://xml.org/sax/features/external-parameter-entities    
              FEATURE = "http://xml.org/sax/features/external-parameter-entities";
              dbf.setFeature(FEATURE, false);
        
              // Disable external DTDs as well
              FEATURE = "http://apache.org/xml/features/nonvalidating/load-external-dtd";
              dbf.setFeature(FEATURE, false);
        
              // and these as well, per Timothy Morgan's 2014 paper: "XML Schema, DTD, and Entity Attacks"
              dbf.setXIncludeAware(false);
              dbf.setExpandEntityReferences(false);
         
              // And, per Timothy Morgan: "If for some reason support for inline DOCTYPEs are a requirement, then 
              // ensure the entity settings are disabled (as shown above) and beware that SSRF attacks
              // (http://cwe.mitre.org/data/definitions/918.html) and denial 
              // of service attacks (such as billion laughs or decompression bombs via "jar:") are a risk."
        
              // remaining parser logic
              ...
              } catch (ParserConfigurationException e) {
                    // This should catch a failed setFeature feature
                    logger.info("ParserConfigurationException was thrown. The feature '" +
                        FEATURE + "' is probably not supported by your XML processor.");
                    ...
                }
                catch (SAXException e) {
                    // On Apache, this should be thrown when disallowing DOCTYPE
                    logger.warning("A DOCTYPE was passed into the XML document");
                    ...
                }
                catch (IOException e) {
                    // XXE that points to a file that doesn't exist
                    logger.error("IOException occurred, XXE may still possible: " + e.getMessage());
                    ...
                }
            return sessionConfiguration;
        } catch (JAXBException e) {
            throw new SessionConfigurationException(e);
        }

    }

    public void save(String fileName) throws SessionConfigurationException {
        saveSessionConfiguration(this, fileName);
    }

    public static void saveSessionConfiguration(SessionConfiguration sessionConfiguration, String fileName) throws SessionConfigurationException {

        try {
            JAXBContext jaxbContext = JAXBContext.newInstance(SessionConfiguration.class);

            Marshaller marshaller = jaxbContext.createMarshaller();

            marshaller.marshal(sessionConfiguration, new File(fileName));
        } catch (JAXBException e) {
            throw new SessionConfigurationException(e);
        }

    }

}
