package com.example.jobaggregator.persistence;

import java.util.UUID;
import org.springframework.data.repository.CrudRepository;

/**
 * Spring Data JDBC repository for the {@link ContractEntity} aggregate root.
 */
public interface ContractEntityRepository extends CrudRepository<ContractEntity, UUID> {
}
