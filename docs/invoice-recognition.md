# Reading invoices

Every attachment a mailbox brings in (see [mailboxes.md](mailboxes.md)) goes
through the same steps; the first one that finds an invoice wins.

1. **E-invoice as XML** (XRechnung): read exactly, both syntaxes, UN/CEFACT
   CII and OASIS UBL.
2. **PDF with an embedded e-invoice** (ZUGFeRD, Factur-X): the embedded XML
   is read exactly.
3. **PDF with text**: number, dates, amounts, IBAN and parties are taken from
   the text by labels in German and English ("Rechnungsnummer", "Invoice
   no.", "Gesamtbetrag", "Fällig bis", …).
4. **Scan** (a PDF without text, or an image): Tesseract recognises the text
   of the first three pages, then step 3 runs on it. OCR runs on your own
   server; no invoice leaves it. Without Tesseract installed, scans are
   skipped.

The words these labels are looked for by live in
`api/src/main/resources/invoice-vocabulary.json`: headings, the labels in
front of number, amounts and dates, payment terms ("innerhalb von {days}
Tagen") and month names. A space in a label also matches a hyphen or no
space, so `rechnungs nr` finds "Rechnungsnr." and "Rechnungs-Nr."; amount
labels are tried in the order of the file. A label an invoice uses that is
missing goes there.

Invoices from steps 3 and 4 are marked *to review* until the owner confirms
or corrects them, since labels and layouts vary.

An invoice from the incoming folder is one the business pays, one from the
sent folder one the business sent. The original file is stored with the
invoice (deduplicated by its SHA-256) and can be downloaded from the panel;
it is served as an attachment in a sandbox, never rendered by the panel.

XML is parsed with DOCTYPE declarations refused, so no external entities or
entity expansion are possible.

## Payment advices

A mail whose subject or text says *Zahlungsavis*, *Überweisungsavis*,
*Zahlungsankündigung*, *Avis*, *payment advice*, *remittance advice* or
*payment notification* is a payment advice, never an invoice. The invoice
numbers it names are marked as announced (with the date and, if given, the
amount), until the money arrives.

See [invoice-matching.md](invoice-matching.md) for how payments are found.