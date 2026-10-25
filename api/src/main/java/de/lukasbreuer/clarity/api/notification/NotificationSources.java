package de.lukasbreuer.clarity.api.notification;

import de.lukasbreuer.clarity.api.banking.account.AccountScope;
import de.lukasbreuer.clarity.api.banking.account.BankAccount;
import de.lukasbreuer.clarity.api.banking.account.BankAccountRepository;
import de.lukasbreuer.clarity.api.banking.connection.BankConnection;
import de.lukasbreuer.clarity.api.banking.connection.BankConnectionRepository;
import de.lukasbreuer.clarity.api.space.Space;
import de.lukasbreuer.clarity.api.space.SpaceRepository;
import de.lukasbreuer.clarity.api.transaction.TransactionRepository;
import de.lukasbreuer.clarity.api.transaction.recurring.RecurringPayment;
import de.lukasbreuer.clarity.api.transaction.recurring.RecurringPaymentDetector;
import de.lukasbreuer.clarity.api.transaction.recurring.RecurringSetting;
import de.lukasbreuer.clarity.api.transaction.recurring.RecurringSettingRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

import java.time.LocalDate;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;
import java.util.function.Function;
import java.util.stream.Collectors;
import java.util.stream.Stream;

/**
 * Gathers what NotificationEvents looks at: every bank connection and
 * account, and the fixed costs of every space, found the same way as for
 * the fixed costs page; those the owner said are none stay out.
 */
@Component
@RequiredArgsConstructor
public final class NotificationSources {
  private static final int HISTORY_DAYS = 400;

  private final BankConnectionRepository connectionRepository;
  private final BankAccountRepository accountRepository;
  private final SpaceRepository spaceRepository;
  private final AccountScope accountScope;
  private final TransactionRepository transactionRepository;
  private final RecurringSettingRepository settingRepository;

  public CompletableFuture<List<Notification>> collect(
    NotificationEvents events, LocalDate today
  ) {
    return connectionRepository.findAll()
      .thenCombine(accountRepository.findAll(), (connections, accounts) ->
        bankEvents(events, connections, accounts))
      .thenCombine(priceEvents(events, today), this::joined);
  }

  private List<Notification> bankEvents(
    NotificationEvents events, List<BankConnection> connections,
    List<BankAccount> accounts
  ) {
    var byId = connections.stream().collect(Collectors.toMap(
      BankConnection::id, Function.identity()));
    return joined(events.consents(connections),
      events.failingSyncs(accounts, byId));
  }

  private CompletableFuture<List<Notification>> priceEvents(
    NotificationEvents events, LocalDate today
  ) {
    return spaceRepository.findAll()
      .thenCompose(spaces -> joined(spaces.stream().map(Space::id)
        .map(spaceId -> priceEvents(events, spaceId, today)).toList()));
  }

  private CompletableFuture<List<Notification>> priceEvents(
    NotificationEvents events, UUID spaceId, LocalDate today
  ) {
    return accountScope.accountIds(spaceId)
      .thenCompose(accountIds -> transactionRepository.findByAccountsBetween(
        accountIds, today.minusDays(HISTORY_DAYS), today))
      .thenApply(RecurringPaymentDetector.create(today)::detect)
      .thenCombine(settingRepository.findBySpaceId(spaceId),
        (payments, settings) -> events.priceChanges(spaceId,
          withoutIgnored(payments, settings), names(settings)));
  }

  private Map<String, String> names(List<RecurringSetting> settings) {
    return settings.stream()
      .filter(setting -> setting.displayName() != null)
      .collect(Collectors.toMap(RecurringSetting::paymentKey,
        RecurringSetting::displayName));
  }

  private List<RecurringPayment> withoutIgnored(
    List<RecurringPayment> payments, List<RecurringSetting> settings
  ) {
    var ignored = settings.stream().filter(RecurringSetting::isIgnored)
      .map(RecurringSetting::paymentKey).collect(Collectors.toSet());
    return payments.stream()
      .filter(payment -> !ignored.contains(payment.key())).toList();
  }

  private CompletableFuture<List<Notification>> joined(
    List<CompletableFuture<List<Notification>>> futures
  ) {
    return CompletableFuture.allOf(futures.toArray(CompletableFuture[]::new))
      .thenApply(_ -> futures.stream()
        .flatMap(future -> future.join().stream()).toList());
  }

  private List<Notification> joined(
    List<Notification> first, List<Notification> second
  ) {
    return Stream.concat(first.stream(), second.stream()).toList();
  }
}
