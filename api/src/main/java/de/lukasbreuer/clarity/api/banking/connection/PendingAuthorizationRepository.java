package de.lukasbreuer.clarity.api.banking.connection;

import de.lukasbreuer.clarity.api.database.DatabaseRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface PendingAuthorizationRepository
  extends DatabaseRepository<PendingAuthorization, String> {
}