package de.lukasbreuer.clarity.api.business.mail;

import java.util.Locale;
import java.util.regex.Pattern;

/**
 * Recognises payment advices (Zahlungsavis): mails in which a customer
 * announces a transfer, usually listing the invoices it pays. Such a mail is
 * never imported as an invoice itself, even when its attachment looks like
 * one.
 */
public final class PaymentAdvice {
  private static final Pattern ADVICE = Pattern.compile(
    "zahlungsavis|überweisungsavis|zahlungsankündigung|\\bavis\\b|" +
      "payment advice|remittance advice|payment notification",
    Pattern.CASE_INSENSITIVE | Pattern.UNICODE_CASE);

  private PaymentAdvice() {
  }

  public static boolean isAdvice(ReceivedMail mail) {
    return ADVICE.matcher(mail.subject()).find() ||
      ADVICE.matcher(mail.body().toLowerCase(Locale.GERMAN)).find();
  }
}
