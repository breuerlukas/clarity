package de.lukasbreuer.clarity.api.transaction.category;

import de.lukasbreuer.clarity.api.transaction.Category;
import lombok.RequiredArgsConstructor;

import java.util.List;
import java.util.Map;
import java.util.Optional;

/**
 * Built-in keywords for merchants common in Germany and the EU. Order matters:
 * the first match wins, so "amazon prime" has to come before "amazon".
 */
@RequiredArgsConstructor(staticName = "create")
public final class CategoryKeywords {
  private static final List<Map.Entry<Category, List<String>>> KEYWORDS =
    List.of(
    Map.entry(Category.SALARY, List.of("gehalt", "lohn", "salary", "bezüge")),
    Map.entry(Category.SUBSCRIPTIONS, List.of("amazon prime", "netflix",
      "spotify", "disney plus", "disney+", "apple.com/bill", "youtube",
      "audible", "dazn", "icloud", "google one", "adobe", "openai",
      "chatgpt", "patreon", "paramount")),
    Map.entry(Category.HOUSING, List.of("miete", "hausverwaltung", "vonovia",
      "nebenkosten", "wohnungsbau", "immobilien")),
    Map.entry(Category.UTILITIES, List.of("stadtwerke", "vattenfall", "e.on",
      "telekom", "vodafone", "o2 ", "telefonica", "1&1", "congstar",
      "rundfunk", "strom", "enbw", "tibber", "ostrom")),
    Map.entry(Category.GROCERIES, List.of("rewe", "edeka", "lidl", "aldi",
      "penny", "netto", "kaufland", "rossmann", "dm drogerie", "dm-drogerie",
      "globus", "tegut", "norma", "denns", "alnatura", "flink", "picnic")),
    Map.entry(Category.DINING, List.of("lieferando", "mcdonald", "burger king",
      "starbucks", "wolt", "uber eats", "restaurant", "pizza", "kfc",
      "subway", "vapiano", "backerei", "bäckerei")),
    Map.entry(Category.MOBILITY, List.of("deutsche bahn", "db vertrieb",
      "db fernverkehr", "bvg", "mvg", "hvv", "kvb", "uber", "bolt",
      "tier mobility", "lime", "aral", "esso", "shell", "flixbus", "sixt",
      "share now", "miles mobility", "tankstelle")),
    Map.entry(Category.TRAVEL, List.of("lufthansa", "ryanair", "eurowings",
      "easyjet", "condor", "booking.com", "airbnb", "expedia", "hotel")),
    Map.entry(Category.SHOPPING, List.of("amazon", "amzn", "zalando", "ikea",
      "mediamarkt", "saturn", "otto gmbh", "otto.de", "about you", "h&m",
      "zara", "decathlon", "ebay", "temu", "shein", "thalia", "galaxus")),
    Map.entry(Category.INSURANCE, List.of("versicherung", "allianz", "huk",
      "axa", "ergo", "debeka", "devk", "signal iduna", "wgv")),
    Map.entry(Category.HEALTH, List.of("apotheke", "arzt", "zahnarzt",
      "docmorris", "krankenkasse", "physio", "optik", "fielmann")),
    Map.entry(Category.LEISURE, List.of("fitness", "mcfit", "urban sports",
      "kino", "cinema", "steam", "playstation", "nintendo", "eventim",
      "museum", "theater")),
    Map.entry(Category.EDUCATION, List.of("udemy", "universität",
      "hochschule", "semesterbeitrag", "studierendenwerk", "coursera")),
    Map.entry(Category.CASH, List.of("geldautomat", "bargeld", "atm ",
      "cash withdrawal", "auszahlung")),
    Map.entry(Category.FEES, List.of("kontoführung", "entgelt", "gebühr",
      "fee ", "zinsen")),
    Map.entry(Category.SAVINGS, List.of("trade republic", "scalable capital",
      "sparplan", "tagesgeld", "bitpanda", "traderepublic")));

  public Optional<Category> find(String text) {
    return KEYWORDS.stream()
      .filter(entry -> entry.getValue().stream().anyMatch(keyword ->
        KeywordMatch.of(text, keyword).matches()))
      .map(Map.Entry::getKey)
      .findFirst();
  }
}