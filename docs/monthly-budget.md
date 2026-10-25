# Monthly budget and payday

The big number on the overview is what is left of the money meant for the
current pay cycle, not what lies on the account. Spending everything that
is on the account is rarely the plan.

## The pay cycle

A pay cycle runs from one salary to the next, the calendar month when no
salary is known (`spendingRoom` in `lib/budget.ts`). A salary is a booking
in the `SALARY` category, or, when the bank's text doesn't say "Gehalt",
the largest income of at least 300 € that arrives about monthly from the
same payer: its latest bookings 25 to 36 days apart (`regularIncome`).

## The budget

The owner sets a budget per space (`space.monthly_budget`,
`/spaces/budget/`): what to spend per pay cycle on top of the fixed costs.
Without one, the panel suggests the last salary minus the monthly share of
all running fixed costs (savings plans included). Without a budget and
without a known salary, the card asks for one.

`monthlyBudget` counts only variable spending since the cycle began: every
debit except the bookings of running fixed costs, transfers and ATM
withdrawals (cash counts once spent from the wallet). Split parts count
like the statistics do. Left and per day follow from that.

## The accounts as context

Next to it the card shows the everyday accounts minus fixed costs still due
before payday and money set aside for goals. When that is less than what
the budget still allows, it warns: the budget is a plan, the accounts are
what is there.