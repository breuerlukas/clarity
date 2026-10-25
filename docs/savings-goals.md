# Savings goals

A savings goal belongs to a space (`savings_goal`, package `space/goal`): a
name, an icon, a target and an optional target date. What counts as saved
comes from one of two places:

- **By hand:** the owner sets money aside (`/goals/deposit/`, a negative
  amount takes some back, never more than was set aside). The money stays on
  the everyday accounts; only its purpose changes.
- **An account:** the balance of a linked account of the same space counts,
  and the manual amount is ignored while it is linked.

## The spending room

Money set aside by hand still lies on the everyday accounts, so the room
until payday would show it as free. `setAsideForGoals` (`lib/goals.ts`) adds
up what has to come off: every manual amount, and the balance of a linked
*everyday* account up to the goal's target. A linked savings account needs
nothing, since it never counted towards the room.

## Progress

`goalProgress` gives what is saved, the share of the target (at most 100 %),
what is left and, with a target date, what it takes per month: the rest
spread over the whole months until then, at least one. A target date in the
past asks for the rest at once.

A space with goals can't be deleted, like one with receipts or contracts.
Error codes: `8200` unknown goal, `8201` invalid goal, `8202` account of
another space, `8203` invalid amount.