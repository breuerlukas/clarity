package de.lukasbreuer.clarity.api.business.invoice.reading;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;
import org.w3c.dom.Document;
import org.xml.sax.SAXException;

import javax.xml.XMLConstants;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.parsers.ParserConfigurationException;
import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.util.Optional;

/**
 * Parses XML from mails, which anyone can send. DOCTYPEs are refused
 * outright, which rules out external entities (XXE) and entity expansion
 * ("billion laughs"); no schema or stylesheet is ever fetched. No e-invoice
 * format needs a DOCTYPE.
 */
@Component
@RequiredArgsConstructor(staticName = "create")
public final class SecureXml {
  private static final String DISALLOW_DOCTYPE =
    "http://apache.org/xml/features/disallow-doctype-decl";
  private static final String EXTERNAL_GENERAL_ENTITIES =
    "http://xml.org/sax/features/external-general-entities";
  private static final String EXTERNAL_PARAMETER_ENTITIES =
    "http://xml.org/sax/features/external-parameter-entities";
  private static final String LOAD_EXTERNAL_DTD =
    "http://apache.org/xml/features/nonvalidating/load-external-dtd";

  /**
   * FindSecBugs only sees the protection when the DOCTYPE features are set
   * in the method that parses.
   */
  public Optional<Document> parse(byte[] xml) {
    try {
      var factory = DocumentBuilderFactory.newInstance();
      factory.setFeature(XMLConstants.FEATURE_SECURE_PROCESSING, true);
      factory.setFeature(DISALLOW_DOCTYPE, true);
      closeExternalAccess(factory);
      var builder = factory.newDocumentBuilder();
      builder.setErrorHandler(null);
      return Optional.of(builder.parse(new ByteArrayInputStream(xml)));
    } catch (ParserConfigurationException | SAXException | IOException
      exception) {
      return Optional.empty();
    }
  }

  private void closeExternalAccess(DocumentBuilderFactory factory)
    throws ParserConfigurationException {
    factory.setFeature(EXTERNAL_GENERAL_ENTITIES, false);
    factory.setFeature(EXTERNAL_PARAMETER_ENTITIES, false);
    factory.setFeature(LOAD_EXTERNAL_DTD, false);
    factory.setAttribute(XMLConstants.ACCESS_EXTERNAL_DTD, "");
    factory.setAttribute(XMLConstants.ACCESS_EXTERNAL_SCHEMA, "");
    factory.setXIncludeAware(false);
    factory.setExpandEntityReferences(false);
    factory.setNamespaceAware(true);
  }
}