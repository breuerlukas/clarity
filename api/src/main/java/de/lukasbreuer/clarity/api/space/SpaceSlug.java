package de.lukasbreuer.clarity.api.space;

import lombok.RequiredArgsConstructor;

import java.text.Normalizer;
import java.util.Locale;
import java.util.Set;

/**
 * Turns a space's name into its part of the address: "Café Müller" becomes
 * "cafe-mueller". A slug another space already has gets a number ("-2").
 */
@RequiredArgsConstructor(staticName = "of")
public final class SpaceSlug {
  private static final int MAXIMUM_LENGTH = 40;
  private static final String FALLBACK = "space";

  private final Set<String> taken;

  public String forName(String name) {
    var base = base(name);
    var slug = base;
    for (var number = 2; taken.contains(slug); number++) {
      slug = base + "-" + number;
    }
    return slug;
  }

  private String base(String name) {
    var spelledOut = name.toLowerCase(Locale.ROOT)
      .replace("ä", "ae").replace("ö", "oe").replace("ü", "ue")
      .replace("ß", "ss");
    var ascii = Normalizer.normalize(spelledOut, Normalizer.Form.NFKD)
      .replaceAll("\\p{M}", "");
    var slug = ascii.replaceAll("[^a-z0-9]+", "-").replaceAll("^-|-$", "");
    if (slug.length() > MAXIMUM_LENGTH) {
      slug = slug.substring(0, MAXIMUM_LENGTH).replaceAll("-$", "");
    }
    return slug.isEmpty() ? FALLBACK : slug;
  }
}
