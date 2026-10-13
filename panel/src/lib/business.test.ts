import { describe, expect, it } from "vitest";
import type { BusinessTransaction, Contract } from "@/lib/api";
import {
  contractStatus,
  contractsByCustomer,
  matchPayment,
  monthlyValue,
  nextExit,
  nextRenewal,
  personalIbans,
  revenueHistory,
  upcomingEvents,
} from "@/lib/business";
import { addMonths, daysBetween } from "@/lib/dates";

const contract = (overrides: Partial<Contract> = {}): Contract => ({
  id: crypto.randomUUID(),
  customer: "Müller GmbH",
  amount: 1200,
  interval: "MONTHLY",
  start_date: "2026-01-01",
  term_months: 12,
  notice_months: 3,
  end_date: null,
  created_at: 0,
  ...overrides,
});

const payment = (overrides: Partial<BusinessTransaction> = {}): BusinessTransaction => ({
  id: crypto.randomUUID(),
  account: "business",
  date: "2026-10-05",
  amount: 1200,
  currency: "EUR",
  counterparty: "MUELLER GMBH",
  counterparty_iban: "DE11 1111",
  description: "Rechnung 2026-10",
  category: "INCOME",
  assignment: null,
  ...overrides,
});

describe("dates", () => {
  it("keeps the day where the month has it", () => {
    expect(addMonths("2026-01-31", 1)).toBe("2026-02-28");
    expect(addMonths("2026-03-15", -3)).toBe("2025-12-15");
    expect(daysBetween("2026-10-13", "2026-11-12")).toBe(30);
  });
});

describe("contract terms", () => {
  it("spreads the invoice over the months it covers", () => {
    expect(monthlyValue(contract({ amount: 3000, interval: "QUARTERLY" }))).toBe(1000);
    expect(monthlyValue(contract({ amount: 6000, interval: "YEARLY" }))).toBe(500);
  });

  it("finds the next deadline and the renewal of the running term", () => {
    const yearly = contract();
    expect(nextExit(yearly, "2026-10-13")).toEqual({ deadline: "2027-09-30", end: "2027-12-31" });
    expect(nextExit(yearly, "2026-09-30")).toEqual({ deadline: "2026-09-30", end: "2026-12-31" });
    expect(nextRenewal(yearly, "2026-10-13")).toBe("2027-01-01");
  });

  it("tells planned, running, cancelled and ended contracts apart", () => {
    expect(contractStatus(contract({ start_date: "2026-11-01" }), "2026-10-13")).toBe("planned");
    expect(contractStatus(contract(), "2026-10-13")).toBe("active");
    expect(contractStatus(contract({ end_date: "2026-12-31" }), "2026-10-13")).toBe("cancelled");
    expect(contractStatus(contract({ end_date: "2026-06-30" }), "2026-10-13")).toBe("ended");
  });

  it("lists deadlines, renewals and ends that come up soon", () => {
    const events = upcomingEvents(
      [
        contract({ id: "running", start_date: "2026-01-15", term_months: 12, notice_months: 3 }),
        contract({ id: "ending", end_date: "2026-11-30" }),
        contract({ id: "later" }),
      ],
      "2026-10-13",
    );
    expect(events.map((event) => [event.contract.id, event.kind, event.date])).toEqual([
      ["running", "deadline", "2026-10-14"],
      ["ending", "end", "2026-11-30"],
      ["later", "renewal", "2027-01-01"],
    ]);
  });

  it("groups contracts per customer, the biggest first", () => {
    const groups = contractsByCustomer(
      [
        contract({ customer: "Müller GmbH", amount: 100 }),
        contract({ customer: "Beta AG", amount: 900 }),
        contract({ customer: "MUELLER GMBH", amount: 300 }),
      ],
      "2026-10-13",
    );
    expect(groups.map((group) => [group.contracts.length, group.monthly])).toEqual([
      [1, 900],
      [2, 400],
    ]);
  });
});

describe("revenueHistory", () => {
  it("derives MRR, ARR, customers and churn from the contracts", () => {
    const history = revenueHistory(
      [
        contract({ customer: "A", amount: 1000, start_date: "2026-07-01" }),
        contract({ customer: "B", amount: 3000, interval: "QUARTERLY", start_date: "2026-08-10" }),
        contract({ customer: "C", amount: 500, start_date: "2026-07-01", end_date: "2026-09-30" }),
      ],
      ["2026-07", "2026-08", "2026-09", "2026-10"],
      new Date(2026, 9, 13),
    );
    expect(history.map((point) => point.mrr)).toEqual([1500, 2500, 2500, 2000]);
    expect(history[3].arr).toBe(24000);
    expect(history.map((point) => point.customers)).toEqual([2, 3, 3, 2]);
    expect(history.map((point) => point.churn)).toEqual([null, 0, 0, 1 / 3]);
  });
});

describe("matchPayment", () => {
  const own = personalIbans(["DE89 3704 0044 0532 0130 00", null]);

  it("matches a customer's payment despite spelling and legal form", () => {
    const muller = contract();
    expect(matchPayment(payment(), [muller], own)).toEqual({
      kind: "CONTRACT",
      contract: muller,
      automatic: true,
    });
  });

  it("prefers the contract with the same amount", () => {
    const small = contract({ amount: 200 });
    const large = contract({ amount: 1200 });
    expect(matchPayment(payment(), [small, large], own).contract).toBe(large);
  });

  it("recognizes private withdrawals and deposits by the owner's own iban", () => {
    const iban = "DE89370400440532013000";
    expect(matchPayment(payment({ amount: -2500, counterparty_iban: iban }), [], own).kind).toBe("WITHDRAWAL");
    expect(matchPayment(payment({ amount: 5000, counterparty_iban: iban }), [], own).kind).toBe("DEPOSIT");
  });

  it("leaves unknown payers and contracts long over unmatched", () => {
    expect(matchPayment(payment({ counterparty: "Someone Else" }), [contract()], own).kind).toBe("NONE");
    expect(matchPayment(payment(), [contract({ end_date: "2026-03-31" })], own).kind).toBe("NONE");
  });

  it("lets the owner's choice win", () => {
    const other = contract({ customer: "Other" });
    const manual = payment({ assignment: { kind: "CONTRACT", contract: other.id } });
    expect(matchPayment(manual, [contract(), other], own)).toEqual({
      kind: "CONTRACT",
      contract: other,
      automatic: false,
    });
    expect(matchPayment(payment({ assignment: { kind: "NONE", contract: null } }), [contract()], own).kind).toBe("NONE");
  });
});
