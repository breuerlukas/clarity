package de.lukasbreuer.clarity.api.web.settings;

import de.lukasbreuer.clarity.api.owner.session.OwnerSession;
import de.lukasbreuer.clarity.api.owner.session.OwnerSessionRepository;
import de.lukasbreuer.clarity.api.owner.session.OwnerSessionStatus;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;

@Component
@RequiredArgsConstructor
public final class SessionClosing {
  private final OwnerSessionRepository sessionRepository;

  public CompletableFuture<List<OwnerSession>> activeSessions() {
    return sessionRepository.findByStatus(OwnerSessionStatus.ACTIVE)
      .thenApply(sessions -> sessions.stream()
        .filter(OwnerSession::isValid).toList());
  }

  public CompletableFuture<Integer> closeAllExcept(UUID keptSessionId) {
    return activeSessions()
      .thenApply(sessions -> sessions.stream()
        .filter(session -> !session.id().equals(keptSessionId))
        .toList())
      .thenCompose(this::close);
  }

  public CompletableFuture<Integer> close(List<OwnerSession> sessions) {
    sessions.forEach(OwnerSession::close);
    return sessionRepository.saveAll(sessions).thenApply(List::size);
  }
}
