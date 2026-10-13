package de.lukasbreuer.clarity.api.space;

import de.lukasbreuer.clarity.api.banking.connection.BankConnection;
import de.lukasbreuer.clarity.api.banking.connection.BankConnectionRepository;
import de.lukasbreuer.clarity.api.log.Log;
import lombok.RequiredArgsConstructor;
import org.springframework.boot.context.event.ApplicationReadyEvent;
import org.springframework.context.event.EventListener;
import org.springframework.stereotype.Component;

import java.util.Comparator;
import java.util.List;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;

/**
 * Makes sure there is always a space to open: on start, an instance without
 * one gets an unnamed personal space, and bank connections made before
 * spaces existed move into the oldest personal space.
 */
@Component
@RequiredArgsConstructor
public final class SpaceSetup {
  public static final String DEFAULT_SLUG = "home";
  public static final String DEFAULT_ICON = "wallet";

  private final Log log;
  private final SpaceRepository spaceRepository;
  private final BankConnectionRepository connectionRepository;

  @EventListener(ApplicationReadyEvent.class)
  void prepare() {
    defaultSpace()
      .thenCompose(space -> connectionRepository.findAll()
        .thenCompose(connections -> adopt(connections, space.id())))
      .join();
  }

  private CompletableFuture<Space> defaultSpace() {
    return spaceRepository.findByKind(SpaceKind.PERSONAL)
      .thenCompose(spaces -> spaces.stream()
        .min(Comparator.comparing(Space::createdAt))
        .map(CompletableFuture::completedFuture)
        .orElseGet(this::createDefault));
  }

  private CompletableFuture<Space> createDefault() {
    log.info("Creating the default space");
    return spaceRepository.save(Space.create(UUID.randomUUID(), DEFAULT_SLUG,
      "", DEFAULT_ICON, SpaceKind.PERSONAL, System.currentTimeMillis()));
  }

  private CompletableFuture<Void> adopt(
    List<BankConnection> connections, UUID spaceId
  ) {
    return CompletableFuture.allOf(connections.stream()
      .filter(connection -> connection.spaceId() == null)
      .peek(connection -> connection.moveTo(spaceId))
      .map(connectionRepository::save)
      .toArray(CompletableFuture[]::new));
  }
}
