package de.lukasbreuer.clarity.api.business.invoice.reading;

import lombok.RequiredArgsConstructor;
import org.w3c.dom.Element;
import org.w3c.dom.Node;

import java.math.BigDecimal;
import java.util.List;
import java.util.Objects;
import java.util.Optional;
import java.util.stream.Stream;

/**
 * An element of an e-invoice. Elements are found by local name, so
 * namespace prefixes do not matter.
 */
@RequiredArgsConstructor(staticName = "of")
final class XmlElement {
  private final Element element;

  String localName() {
    return element.getLocalName();
  }

  String attribute(String name) {
    return element.getAttribute(name);
  }

  Optional<XmlElement> child(String localName) {
    return children(localName).stream().findFirst();
  }

  List<XmlElement> children(String localName) {
    return Stream.iterate(element.getFirstChild(), Objects::nonNull,
        Node::getNextSibling)
      .filter(Element.class::isInstance)
      .map(Element.class::cast)
      .filter(child -> localName.equals(child.getLocalName()))
      .map(XmlElement::of)
      .toList();
  }

  /**
   * The first element of that name anywhere below this one.
   */
  Optional<XmlElement> descendant(String localName) {
    var elements = element.getElementsByTagNameNS("*", localName);
    return elements.getLength() == 0 ? Optional.empty() :
      Optional.of(XmlElement.of((Element) elements.item(0)));
  }

  Optional<String> text() {
    return Optional.ofNullable(element.getTextContent()).map(String::trim)
      .filter(value -> !value.isEmpty());
  }

  Optional<BigDecimal> amount() {
    return text().map(BigDecimal::new);
  }

  Optional<String> childText(String localName) {
    return child(localName).flatMap(XmlElement::text);
  }

  Optional<BigDecimal> childAmount(String localName) {
    return child(localName).flatMap(XmlElement::amount);
  }
}