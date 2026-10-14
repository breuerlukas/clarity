package de.lukasbreuer.clarity.api.business.invoice.reading;

import de.lukasbreuer.clarity.api.business.invoice.InvoiceFormat;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

import java.time.LocalDate;
import java.util.Optional;

/**
 * Tells whether a file is an invoice and reads it: XML files and PDFs with
 * an embedded e-invoice exactly, other PDFs and images from their text.
 * The text is kept as well, so payment advices can be searched for the
 * invoice numbers they announce.
 */
@Component
@RequiredArgsConstructor(staticName = "create")
public final class InvoiceRecognition {
  private final Ocr ocr;
  private final ElectronicInvoiceReader electronicReader;
  private final PdfInvoiceReader pdfReader;
  private final TextInvoiceReader textReader;

  /**
   * @param invoice The invoice the file is, if it is one
   * @param text The file's text, empty for XML and unreadable files
   */
  public record Result(Optional<RecognisedInvoice> invoice, String text) {
    static Result nothing() {
      return new Result(Optional.empty(), "");
    }
  }

  /**
   * @param received When the mail arrived, the invoice date if a text based
   *                 invoice has none
   */
  public Result recognise(InvoiceFile file, LocalDate received) {
    if (file.isXml()) {
      return recogniseXml(file);
    }
    if (file.isPdf()) {
      return recognisePdfFile(file, received);
    }
    if (file.isImage()) {
      return recogniseImage(file, received);
    }
    return Result.nothing();
  }

  private Result recogniseXml(InvoiceFile file) {
    return new Result(electronicReader.read(file.content())
      .map(fields -> new RecognisedInvoice(fields, InvoiceFormat.XRECHNUNG,
        false)), "");
  }

  private Result recognisePdfFile(InvoiceFile file, LocalDate received) {
    return pdfReader.read(file.content())
      .map(content -> recognisePdf(content, received))
      .orElseGet(Result::nothing);
  }

  private Result recogniseImage(InvoiceFile file, LocalDate received) {
    return ocr.read(file.content())
      .map(text -> fromText(text, InvoiceFormat.OCR, received))
      .orElseGet(Result::nothing);
  }

  /**
   * An embedded e-invoice wins over the PDF's text.
   */
  private Result recognisePdf(PdfContent content, LocalDate received) {
    var electronic = content.xml().flatMap(electronicReader::read);
    if (electronic.isPresent()) {
      return new Result(Optional.of(new RecognisedInvoice(electronic.get(),
        InvoiceFormat.ZUGFERD, false)), content.text());
    }
    return fromText(content.text(), content.recognised() ?
      InvoiceFormat.OCR : InvoiceFormat.PDF, received);
  }

  private Result fromText(
    String text, InvoiceFormat format, LocalDate received
  ) {
    return new Result(textReader.read(text, received)
      .map(fields -> new RecognisedInvoice(fields, format, true)), text);
  }
}