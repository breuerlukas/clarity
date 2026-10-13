package de.lukasbreuer.clarity.api.web.business;

import de.lukasbreuer.clarity.api.business.contract.BillingInterval;
import de.lukasbreuer.clarity.api.web.request.ApiRequestBody;
import org.json.JSONObject;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.time.LocalDate;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

final class ContractDraftTest {
  @Test
  void readsCompleteContract() {
    var draft = ContractDraft.parse(body("  Müller GmbH ", "1200.50",
      "QUARTERLY", 12, 3)).orElseThrow();
    assertEquals("Müller GmbH", draft.customer());
    assertEquals(new BigDecimal("1200.50"), draft.amount());
    assertEquals(BillingInterval.QUARTERLY, draft.interval());
    assertEquals(LocalDate.of(2026, 1, 1), draft.startDate());
  }

  @Test
  void refusesInvalidContracts() {
    assertTrue(ContractDraft.parse(body(" ", "100", "MONTHLY", 12, 3))
      .isEmpty());
    assertTrue(ContractDraft.parse(body("A", "0", "MONTHLY", 12, 3))
      .isEmpty());
    assertTrue(ContractDraft.parse(body("A", "9.999", "MONTHLY", 12, 3))
      .isEmpty());
    assertTrue(ContractDraft.parse(body("A", "100", "DAILY", 12, 3))
      .isEmpty());
    assertTrue(ContractDraft.parse(body("A", "100", "MONTHLY", 0, 0))
      .isEmpty());
    assertTrue(ContractDraft.parse(body("A", "100", "MONTHLY", 3, 6))
      .isEmpty());
  }

  private ApiRequestBody body(
    String customer, String amount, String interval, int term, int notice
  ) {
    return ApiRequestBody.create(new JSONObject()
      .put("customer", customer)
      .put("amount", amount)
      .put("interval", interval)
      .put("start_date", "2026-01-01")
      .put("term_months", term)
      .put("notice_months", notice), null);
  }
}
