import type {
  BusinessTransaction,
  Invoice,
  InvoiceDirection,
  InvoiceFormat,
  InvoicePayment,
  Mailbox,
} from "@/lib/api";
import { addDays, isoDate } from "@/lib/dates";
import { BUSINESS_SPACE } from "@/demo/demo-business";

/**
 * The agency's invoices as the mailbox brought them in: sent ones to
 * customers, some paid by the bookings on the business account, one partly,
 * one announced by a payment advice and one overdue; received ones from
 * suppliers, one of them a scan still to review. Dates are relative to today.
 */
const AGENCY = "Pixelwerk Webdesign";
const AGENCY_IBAN = "DE44100110012620044218";
/** A paid invoice was issued this many days before the booking that paid it. */
const PAID_AFTER_DAYS = 10;

interface DemoInvoice {
  direction: InvoiceDirection;
  number: string;
  /** The customer for a sent invoice, the supplier for a received one. */
  counterparty: string;
  gross: number;
  /** Days before today it was issued; paid ones date back from their booking. */
  issuedDaysAgo: number;
  /** Days it is payable in; none for invoices without a due date. */
  payableDays?: number;
  format: InvoiceFormat;
  needsReview?: boolean;
  /** The booking that paid it: whose payer name contains this, the latest one. */
  paidBy?: string;
  /** Linked by hand rather than automatically. */
  manual?: boolean;
  /** Days before today a payment advice announced it. */
  announcedDaysAgo?: number;
  iban?: string;
  sender: string;
}

const INVOICES: DemoInvoice[] = [
  { direction: "OUTGOING", number: "RE-0141", counterparty: "Kanzlei Weber & Partner", gross: 1450, issuedDaysAgo: 30, payableDays: 14, format: "ZUGFERD", paidBy: "Weber und Partner", sender: "rechnung@pixelwerk.de" },
  { direction: "OUTGOING", number: "RE-0138", counterparty: "Autohaus Brandt GmbH", gross: 4800, issuedDaysAgo: 45, payableDays: 30, format: "XRECHNUNG", paidBy: "AUTOHAUS BRANDT", manual: true, sender: "rechnung@pixelwerk.de" },
  { direction: "OUTGOING", number: "RE-0144", counterparty: "Studio Lindgren GmbH", gross: 240, issuedDaysAgo: 18, payableDays: 14, format: "ZUGFERD", paidBy: "Studio Lindgren", sender: "rechnung@pixelwerk.de" },
  { direction: "OUTGOING", number: "RE-0146", counterparty: "Messe Nord GmbH", gross: 760, issuedDaysAgo: 16, payableDays: 30, format: "ZUGFERD", paidBy: "Messe Nord", sender: "rechnung@pixelwerk.de" },
  { direction: "OUTGOING", number: "RE-0145", counterparty: "Nordlicht Events UG", gross: 1428, issuedDaysAgo: 24, payableDays: 14, format: "XRECHNUNG", sender: "rechnung@pixelwerk.de" },
  { direction: "OUTGOING", number: "RE-0148", counterparty: "Krause Immobilien GmbH & Co. KG", gross: 416.5, issuedDaysAgo: 6, payableDays: 14, format: "ZUGFERD", announcedDaysAgo: 1, sender: "rechnung@pixelwerk.de" },
  { direction: "OUTGOING", number: "RE-0149", counterparty: "Hotel Seeblick", gross: 3451, issuedDaysAgo: 2, payableDays: 14, format: "XRECHNUNG", sender: "rechnung@pixelwerk.de" },
  { direction: "INCOMING", number: "200123987", counterparty: "Hetzner Online GmbH", gross: 64.9, issuedDaysAgo: 12, format: "ZUGFERD", paidBy: "Hetzner", iban: "DE92760700120750007700", sender: "invoice@hetzner.com" },
  { direction: "INCOMING", number: "2024-0815", counterparty: "Steuerbüro Lange", gross: 178.5, issuedDaysAgo: 25, payableDays: 10, format: "PDF", paidBy: "Steuerbüro Lange", iban: "DE02120300000000202051", sender: "kanzlei@steuerbuero-lange.de" },
  { direction: "INCOMING", number: "K-55120", counterparty: "Druckerei Kaiser", gross: 412.34, issuedDaysAgo: 9, payableDays: 14, format: "OCR", needsReview: true, iban: "DE75512108001245126199", sender: "buchhaltung@druckerei-kaiser.de" },
  { direction: "INCOMING", number: "CW-2291", counterparty: "Coworking Hafen", gross: 238, issuedDaysAgo: 20, payableDays: 14, format: "XRECHNUNG", iban: "DE21301204000000015228", sender: "billing@coworking-hafen.de" },
];

/** Paid, open and status the way the api works them out. */
export function refreshInvoice(invoice: Invoice, today: string): Invoice {
  const paid = Math.round(invoice.payments.reduce((sum, payment) => sum + payment.amount, 0) * 100) / 100;
  const open = invoice.settled ? 0 : Math.max(0, Math.round((invoice.gross_amount - paid) * 100) / 100);
  invoice.paid_amount = paid;
  invoice.open_amount = open;
  invoice.status =
    open <= 0.01
      ? "PAID"
      : invoice.due_date && invoice.due_date < today
        ? "OVERDUE"
        : paid > 0
          ? "PARTIAL"
          : invoice.announced_at
            ? "ANNOUNCED"
            : "OPEN";
  return invoice;
}

export function demoInvoices(today: Date, transactions: BusinessTransaction[]): Invoice[] {
  const todayIso = isoDate(today);
  const year = today.getFullYear();
  return INVOICES.map((entry, index) => {
    const outgoing = entry.direction === "OUTGOING";
    const booking = entry.paidBy
      ? transactions.find(
          (transaction) => transaction.counterparty.includes(entry.paidBy!) && transaction.amount > 0 === outgoing,
        )
      : undefined;
    const issued = booking
      ? addDays(booking.date, -PAID_AFTER_DAYS)
      : addDays(todayIso, -entry.issuedDaysAgo);
    const payments: InvoicePayment[] = booking
      ? [
          {
            id: `payment-${index + 1}`,
            transaction: booking.id,
            amount: Math.min(Math.abs(booking.amount), entry.gross),
            date: booking.date,
            automatic: !entry.manual,
          },
        ]
      : [];
    const gross = entry.gross;
    const net = Math.round((gross / 1.19) * 100) / 100;
    return refreshInvoice(
      {
        id: `invoice-${index + 1}`,
        space: BUSINESS_SPACE,
        direction: entry.direction,
        number: outgoing ? `${entry.number.slice(0, 3)}${year}-${entry.number.slice(3)}` : entry.number,
        issuer: outgoing ? AGENCY : entry.counterparty,
        recipient: outgoing ? entry.counterparty : AGENCY,
        iban: outgoing ? AGENCY_IBAN : (entry.iban ?? null),
        currency: "EUR",
        net_amount: entry.format === "OCR" ? null : net,
        tax_amount: entry.format === "OCR" ? null : Math.round((gross - net) * 100) / 100,
        gross_amount: gross,
        invoice_date: issued,
        due_date: entry.payableDays === undefined ? null : addDays(issued, entry.payableDays),
        format: entry.format,
        needs_review: Boolean(entry.needsReview),
        settled: false,
        announced_at: entry.announcedDaysAgo === undefined ? null : addDays(todayIso, -entry.announcedDaysAgo),
        announced_amount: entry.announcedDaysAgo === undefined ? null : gross,
        mail: {
          subject: outgoing ? `Rechnung ${entry.number}` : `Ihre Rechnung ${entry.number}`,
          sender: entry.sender,
          received_at: new Date(`${issued}T09:12:00`).getTime(),
        },
        document: null,
        payments,
        paid_amount: 0,
        open_amount: gross,
        status: "OPEN",
        created_at: new Date(`${issued}T09:15:00`).getTime(),
      },
      todayIso,
    );
  });
}

export function demoMailboxes(today: Date): Mailbox[] {
  return [
    {
      id: "mailbox-1",
      space: BUSINESS_SPACE,
      host: "imap.mailbox.org",
      port: 993,
      security: "SSL",
      username: "rechnung@pixelwerk.de",
      incoming_folder: "INBOX",
      outgoing_folder: "Sent",
      last_checked_at: today.getTime() - 1000 * 60 * 6,
      last_error: null,
      created_at: today.getTime() - 1000 * 60 * 60 * 24 * 90,
    },
  ];
}
