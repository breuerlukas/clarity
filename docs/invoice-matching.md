# Matching payments to invoices

After every mail check, bank sync and correction, Clarity links the bookings
of the business space's accounts to its invoices. Money in pays invoices the
business sent, money out pays invoices it received.

## Rules

- **Invoice number in the reference**: the booking pays the named invoices,
  oldest first, as far as it goes. That covers several invoices in one
  transfer and part payments.
- **Early-payment discount (Skonto)**: if a booking names a single invoice
  and falls short of it by at most 3 %, the invoice counts as paid.
- **No number**: a booking is only linked when it is exactly the open amount
  of one invoice and its IBAN or counterparty name fits that invoice too.
- Bookings marked as private withdrawals or deposits are never linked.
- A link the owner removed by hand is never made again.
- A booking may be up to 7 days older than the invoice (paid ahead).

Everything else is left to the owner: the invoice dialog suggests bookings
from 30 days before the invoice on, best first, with the reasons that fit
(number, amount, IBAN, name).

## Status

The first that applies:

| Status | When |
|---|---|
| Paid | Settled by hand or by a discount, or nothing is open |
| Overdue | The due date has passed |
| Partly paid | Some of it is paid |
| Announced | A payment advice named it |
| Open | Otherwise |

## Expected versus actual

The panel compares, month by month, what was invoiced with what came in:
expected income is the gross amount of sent invoices due that month (by due
date, else invoice date), actual income the payments on them booked that
month. Expenses work the same with received invoices.