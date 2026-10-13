package de.lukasbreuer.clarity.api.web.business;

import de.lukasbreuer.clarity.api.business.contract.BillingInterval;
import de.lukasbreuer.clarity.api.business.contract.Contract;
import de.lukasbreuer.clarity.api.web.request.ApiRequestBody;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.Optional;
import java.util.UUID;

/**
 * The fields of a contract as the panel sends them, checked: a customer, an
 * amount above zero with at most cents, a term of one to 120 months and a
 * notice period no longer than the term.
 */
record ContractDraft(
  String customer, BigDecimal amount, BillingInterval interval,
  LocalDate startDate, int termMonths, int noticeMonths
) {
  private static final int MAXIMUM_TERM_MONTHS = 120;
  private static final BigDecimal MAXIMUM_AMOUNT = new BigDecimal("1000000000");

  static Optional<ContractDraft> parse(ApiRequestBody body) {
    try {
      var draft = new ContractDraft(body.getString("customer", 120).trim(),
        new BigDecimal(body.getString("amount", 24)),
        BillingInterval.find(body.getString("interval", 16)).orElseThrow(),
        LocalDate.parse(body.getString("start_date", 10)),
        (int) body.getLong("term_months"), (int) body.getLong("notice_months"));
      return draft.isValid() ? Optional.of(draft) : Optional.empty();
    } catch (Exception exception) {
      return Optional.empty();
    }
  }

  private boolean isValid() {
    return !customer.isEmpty() && amount.signum() > 0 &&
      amount.stripTrailingZeros().scale() <= 2 &&
      amount.compareTo(MAXIMUM_AMOUNT) < 0 &&
      termMonths >= 1 && termMonths <= MAXIMUM_TERM_MONTHS &&
      noticeMonths >= 0 && noticeMonths <= termMonths;
  }

  Contract create(UUID spaceId) {
    return Contract.create(UUID.randomUUID(), spaceId, customer, amount,
      interval,
      startDate, termMonths, noticeMonths, null, System.currentTimeMillis());
  }

  void applyTo(Contract contract) {
    contract.edit(customer, amount, interval, startDate, termMonths,
      noticeMonths);
  }
}
