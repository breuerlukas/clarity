# Categories

Every transaction has exactly one category. Categories live in the `category`
table and are shared by all spaces, like the remembered counterparty rules.

## Built-in and own categories

The 19 built-in categories ship in
`api/src/main/resources/default-categories.json`. On every start
`CategorySetup` adds the ones the database doesn't have yet, so a new
built-in reaches existing installations; stored ones keep the owner's
changes. A built-in category can be renamed, recolored, given another icon
and other keywords, hidden from the pickers, and reset to how it shipped. An
empty name means the panel shows the translated label from the locale files.

Own categories get a random UUID as key. They can be deleted; the owner picks
the category their transactions (hand-picked ones included) and remembered
rules move to, so nothing is left without one.

## System categories

The logic depends on six keys, so they can be renamed but neither hidden
nor deleted, and their kind stays fixed:

| Key | Used by |
|---|---|
| `SALARY` | payday and the room left until it (`budget.ts`) |
| `TRANSFER` | own transfers; left out of statistics and the heatmap |
| `CASH` | ATM withdrawals: moved into the cash wallet, not spent |
| `SUBSCRIPTIONS` | the subscriptions group on the fixed costs page |
| `INCOME` | fallback of the categorizer for unknown money in |
| `OTHER` | fallback of the categorizer for unknown money out |

## How a booking gets its category

On import, first match wins: a move between own accounts is a transfer, then
the remembered counterparty rules, then the keywords. A keyword matches at
the start of a word, ignoring case ("rewe" matches "REWE Markt", "tier" does
not match "Kuriertier"); a trailing space asks for the word to end there
("atm "). Own categories' keywords are tried before the built-in ones, which
go in their shipped order ("amazon prime" before "amazon"). Hidden categories
take no part. Changed keywords apply to new bookings; the ones already stored
keep their category.

## Moving from the enum

Before the table existed, categories were a Java enum stored by name, and
Hibernate guarded both category columns (`transactions.category`,
`category_rule.category`) with a check against those names. The built-in
keys are the old enum names, so stored transactions and rules stay valid as
they are; `CategorySetup` drops the two checks on start so own categories fit
in.

## Budgets

A spending category can get a budget: the most the owner wants to spend on
it per pay cycle. Categories are shared by every space, budgets are not
(`category_budget`, one row per space and category), so a household plans
its groceries apart from the personal ones. Removing the limit deletes the
row; deleting an own category deletes its budgets, since the target category
may have one of its own.

The api only stores the limits (`/budgets/`, `/budgets/update/`, error
`3007` for an invalid limit or a category that isn't a spending one). The
panel works out what was spent (`budgetUsage` in `budget.ts`) over the same
period as the room until payday: from the last salary to the next one, or
the calendar month without a known salary. Split parts and cash entries
count in the category they are assigned to; refunds don't lower the figure,
as in the statistics. `CASH` and `TRANSFER` get no budget, since they are
never counted as spending. The panel offers budgets in personal spaces only.