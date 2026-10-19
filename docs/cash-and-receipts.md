# Cash and receipts

The bank only knows that money left at the ATM, and a card payment is one
line with one category even when the receipt mixes groceries and drugstore
items. The cash wallet and receipts close that gap.

## Cash wallet

Every space has a wallet, an account of its own next to the bank accounts.
It starts the first time the space is opened after the update.

- **Entries**: cash spent or received, with date, amount, counterparty,
  category and a note. They are transactions on the wallet's id (external id
  `cash-<id>`), so lists, statistics, the heatmap and fixed costs see them
  like bank bookings. A bank sync never reads, stores or deletes them.
- **Withdrawals**: a booking in the `CASH` category moves money from the bank
  into the wallet. It counts as a transfer everywhere, so the cash is counted
  once, when it is spent. Withdrawals before the wallet started are taken
  as spent and do not fill it.
- **Balance**: the wallet's entries plus the withdrawals since its start
  (minus cash paid in at the bank, a positive `CASH` booking).
- **Counting**: the owner enters the cash they hold; the difference is booked
  as a correction entry without counterparty in the `TRANSFER` category. It
  shows in the history but is neither spending nor income.
- The room until payday counts the wallet as everyday money; the balance
  chart stays with the bank accounts.

## Receipts

A photo (camera on the phone, file on the desktop) or a PDF is uploaded as
base64 JSON to `/receipts/scan/`, at most 10 MB; the panel shrinks photos to
2000 pixels first. Tesseract reads it like a scanned invoice, and the guess
waits unassigned until the owner has reviewed it.

What is read (`ReceiptText`, words in `receipt-vocabulary.json`):

- **Merchant**: the first line with at least three letters that is no tax,
  payment or register line.
- **Date**: the first date printed, in the formats invoices use.
- **Total**: the amount after the first total label in the vocabulary's
  order ("zu zahlen", "summe", ... "total").
- **Items**: lines above the total that end in an amount, optionally
  followed by a tax class ("BANANEN 1,99 B"). Quantity lines ("2 x 0,99")
  and tax, payment and change lines are skipped.

Then the owner assigns it:

- **Card**: the suggestions are bank bookings of exactly the total, from one
  day before to five days after the purchase, without a receipt yet,
  closest first.
- **Cash**: a cash entry over the total with the merchant as counterparty.
- **Split**: the items get a category each; the parts per category, plus the
  rest of the total (discounts, deposits, rounding) in the main category,
  are stored as the transaction's split. Parts have the transaction's sign
  and add up to it exactly. Statistics count the parts, lists show the
  transaction with its main category.

## Storage and privacy

Receipt files live in `[receipts] directory` (in Docker `/app/data/receipts`
on the `api-data` volume), one file per receipt named by its id, never in the
database and never loaded from anywhere else. They are served only to the
owner, as a download with `Content-Security-Policy: sandbox`. Back the volume
up together with the database dump, see the README.

Removing a bank connection leaves its receipts unassigned. Deleting a receipt
leaves its transaction. A space with cash entries or receipts cannot be
deleted.