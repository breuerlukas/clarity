package de.lukasbreuer.clarity.api.business.invoice.reading;

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
public final class SecureXml {
  private SecureXml() {
  }

  public static Optional<Document> parse(byte[] xml) {
    try {
      var factory = DocumentBuilderFactory.newInstance();
      factory.setFeature(XMLConstants.FEATURE_SECURE_PROCESSING, true);
      factory.setFeature(
        "http://apache.org/xml/features/disallow-doctype-decl", true);
      factory.setFeature(
        "http://xml.org/sax/features/external-general-entities", false);
      factory.setFeature(
        "http://xml.org/sax/features/external-parameter-entities", false);
      factory.setFeature(
        "http://apache.org/xml/features/nonvalidating/load-external-dtd",
        false);
      factory.setAttribute(XMLConstants.ACCESS_EXTERNAL_DTD, "");
      factory.setAttribute(XMLConstants.ACCESS_EXTERNAL_SCHEMA, "");
      factory.setXIncludeAware(false);
      factory.setExpandEntityReferences(false);
      factory.setNamespaceAware(true);
      var builder = factory.newDocumentBuilder();
      builder.setErrorHandler(null);
      return Optional.of(builder.parse(new ByteArrayInputStream(xml)));
    } catch (ParserConfigurationException | SAXException | IOException
      exception) {
      return Optional.empty();
    }
  }
}
