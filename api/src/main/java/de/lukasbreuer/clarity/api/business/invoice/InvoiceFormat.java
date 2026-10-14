package de.lukasbreuer.clarity.api.business.invoice;

/**
 * Where the fields of an invoice were read from. Only the two e-invoice
 * formats are exact: XRECHNUNG is an XML file (UBL or CII), ZUGFERD a PDF
 * with the XML inside (also Factur-X). PDF is the text of an ordinary PDF,
 * OCR the text recognised in a scan; both are guesses until confirmed.
 */
public enum InvoiceFormat {
  XRECHNUNG,
  ZUGFERD,
  PDF,
  OCR;

  public boolean isElectronic() {
    return this == XRECHNUNG || this == ZUGFERD;
  }
}