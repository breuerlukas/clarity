# Fixed costs

Fixed costs are not stored: `RecurringPaymentDetector` finds them in the
last 800 days of bookings on every request. Debits to the same counterparty
(grouped by its lowercased name, the payment's `key`) count when their most
recent gaps fit one `RecurringCycle` and their amounts stay within 15 % of
the latest one.

## Missing and ended

A booking is overdue a week after the longest gap its cycle allows (35 days
for monthly). From then on the payment is `MISSING`; a full cycle later it is
`ENDED`, and one more cycle later it drops out of the list. 800 days of
history are needed so a yearly payment can go missing and end at all.

Ended payments leave everything that looks ahead: the spending room, the
upcoming payments and every total (`useRecurringPayments` filters them out).
Only the fixed costs page shows them, in their own list. Missing ones still
count, as the money may well leave the account in a few days.

## What the owner decides

Detection runs anew each time, so the owner's decisions live apart in
`recurring_setting`, one row per space and payment key:

| Column | Effect |
|---|---|
| `cancelled_on` | ended once the next booking would fall on or after it |
| `dismissed_last_date` | "still running": active while the last booking is this one |
| `notice_days` | `cancel_by` = next booking minus these days |

A dismissal only covers the gap it was made for; once the payment books
again and stops later, it goes missing anew. The overview reminds of every
running payment whose `cancel_by` lies within the next two weeks, until the
notifications of #22 take that over.