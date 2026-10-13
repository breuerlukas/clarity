package de.lukasbreuer.clarity.api.business.contract;

import de.lukasbreuer.clarity.api.database.DatabaseRepository;
import org.springframework.stereotype.Repository;

import java.util.UUID;

@Repository
public interface ContractRepository
  extends DatabaseRepository<Contract, UUID> {
}
