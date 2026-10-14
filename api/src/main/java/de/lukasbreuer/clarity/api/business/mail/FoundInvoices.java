package de.lukasbreuer.clarity.api.business.mail;

import de.lukasbreuer.clarity.api.business.invoice.reading.InvoiceFile;
import lombok.RequiredArgsConstructor;

import java.util.ArrayList;
import java.util.List;

/**
 * The invoices found in the attachments of one mail. A mail may carry the
 * same invoice twice, as an XRechnung next to its PDF, say.
 */
@RequiredArgsConstructor(staticName = "of")
final class FoundInvoices {
  private final List<FoundInvoice> found;

  /**
   * One entry per invoice number; an e-invoice wins over a text read of the
   * same invoice.
   */
  List<FoundInvoice> withoutDuplicates() {
    var kept = new ArrayList<FoundInvoice>();
    found.forEach(entry -> keep(kept, entry));
    return kept;
  }

  /**
   * An XRechnung is XML nobody wants to read; if the mail also has a PDF of
   * the same invoice, that is the file kept.
   */
  InvoiceFile readableFile(FoundInvoice entry) {
    if (!entry.isXRechnung()) {
      return entry.file();
    }
    return found.stream()
      .filter(other -> other.file().isPdf())
      .filter(other -> other.hasNumber(entry.number()))
      .map(FoundInvoice::file)
      .findFirst()
      .orElse(entry.file());
  }

  private void keep(List<FoundInvoice> kept, FoundInvoice entry) {
    var twin = kept.stream()
      .filter(other -> !entry.number().isEmpty() &&
        other.hasNumber(entry.number()))
      .findFirst();
    if (twin.isEmpty()) {
      kept.add(entry);
    } else if (entry.isElectronic() && !twin.get().isElectronic()) {
      kept.set(kept.indexOf(twin.get()), entry);
    }
  }
}