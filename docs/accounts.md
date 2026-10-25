# Accounts and spaces

An account belongs to the space of its bank connection when it is created.
One bank login can bring several accounts, so a single account can be moved
to another space (`/accounts/space/`, `bank_account.space_id`). Moved back to
its connection's space, the column is cleared and it follows the connection
again. `AccountScope` decides which accounts a space has, so transactions,
fixed costs, budgets, statistics and the cash wallet's ATM withdrawals all
move with the account. The connection itself stays put; the panel shows it
in every space that holds one of its accounts. A space that received moved
accounts can't be deleted while they are there.

An account can also be hidden for a while (`/accounts/hidden/`,
`bank_account.hidden`). `AccountScope` leaves it out of every space, so it
counts nowhere; only the accounts page still lists it, greyed out, to show
it again. It keeps syncing, so nothing is missing afterwards.

## Currency

Multi-currency accounts such as PayPal report `XXX` (no currency) for the
account. On sync it takes the currency of its balance, or, when that names
none either, the one of its latest booking.