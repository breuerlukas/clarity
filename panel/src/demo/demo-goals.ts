import { ApiError, type Connection, type SavingsGoal } from "@/lib/api";

const ok = (data: object = {}) => ({ success: true, ...data });

/** Checked like the api does, so the demo answers with the same codes. */
function draftOf(body: Record<string, unknown>, connections: Connection[], space: string) {
  const name = String(body.name ?? "").trim().slice(0, 60);
  const target = String(body.target ?? "");
  const targetDate = String(body.target_date ?? "");
  const account = String(body.account ?? "");
  if (!name || !/^\d{1,9}(\.\d{1,2})?$/.test(target) || Number(target) <= 0 || (targetDate && !/^\d{4}-\d{2}-\d{2}$/.test(targetDate))) {
    throw new ApiError(400, 8201);
  }
  const owned = connections.filter((connection) => connection.space === space).flatMap((connection) => connection.accounts);
  if (account && !owned.some((entry) => entry.id === account)) {
    throw new ApiError(400, 8202);
  }
  return { name, icon: String(body.icon), target: Number(target), target_date: targetDate || null, account: account || null };
}

function find(goals: SavingsGoal[], id: unknown) {
  const goal = goals.find((entry) => entry.id === id);
  if (!goal) {
    throw new ApiError(400, 8200);
  }
  return goal;
}

export function createGoal(goals: SavingsGoal[], connections: Connection[], body: Record<string, unknown>) {
  const space = String(body.space);
  const goal: SavingsGoal = { id: crypto.randomUUID(), space, ...draftOf(body, connections, space), saved: 0, created_at: Date.now() };
  goals.push(goal);
  return ok({ goal });
}

export function updateGoal(goals: SavingsGoal[], connections: Connection[], body: Record<string, unknown>) {
  const goal = find(goals, body.id);
  Object.assign(goal, draftOf(body, connections, goal.space));
  return ok({ goal });
}

export function depositToGoal(goals: SavingsGoal[], body: Record<string, unknown>) {
  const goal = find(goals, body.id);
  const amount = Number(body.amount);
  if (!amount || goal.saved + amount < 0) {
    throw new ApiError(400, 8203);
  }
  goal.saved = Math.round((goal.saved + amount) * 100) / 100;
  return ok({ goal });
}

export function deleteGoal(goals: SavingsGoal[], id: unknown) {
  goals.splice(goals.indexOf(find(goals, id)), 1);
  return ok();
}
