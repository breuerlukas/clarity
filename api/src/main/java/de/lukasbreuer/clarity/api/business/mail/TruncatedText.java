package de.lukasbreuer.clarity.api.business.mail;

import lombok.RequiredArgsConstructor;

/**
 * A text cut to the length its database column holds.
 */
@RequiredArgsConstructor(staticName = "of")
final class TruncatedText {
  private final String text;
  private final int maximumLength;

  String value() {
    return text.substring(0, Math.min(text.length(), maximumLength));
  }
}