package de.lukasbreuer.clarity.api.banking.connection;

import de.lukasbreuer.clarity.api.database.DatabaseRepository;
import org.springframework.stereotype.Repository;

import java.util.UUID;

@Repository
public interface BankConnectionRepository
  extends DatabaseRepository<BankConnection, UUID> {
}