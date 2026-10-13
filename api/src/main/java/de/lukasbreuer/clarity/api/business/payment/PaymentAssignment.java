package de.lukasbreuer.clarity.api.business.payment;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import lombok.AccessLevel;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.experimental.Accessors;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

/**
 * What the owner said a business booking is, overriding the automatic match.
 * The account is kept so removing a bank removes its assignments too.
 */
@Entity
@Table(name = "payment_assignment")
@Getter
@Accessors(fluent = true)
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@AllArgsConstructor(staticName = "create")
public final class PaymentAssignment {
  @Id
  @Column(name = "transaction_id", nullable = false, updatable = false)
  private UUID transactionId;
  @Column(name = "account_id", nullable = false)
  private UUID accountId;
  @Enumerated(EnumType.STRING)
  @Column(name = "kind", nullable = false)
  private PaymentKind kind;
  @Column(name = "contract_id")
  private UUID contractId;

  public Map<String, Object> information() {
    var information = new HashMap<String, Object>();
    information.put("kind", kind.name());
    information.put("contract", contractId);
    return information;
  }
}
