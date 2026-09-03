package com.example.jobaggregator.persistence;

import org.springframework.data.repository.CrudRepository;

/**
 * Spring Data JDBC repository for the {@link ContractEntity} aggregate root.
 *
 * <p>A single call to {@link #save(Object)} automatically:
 * <ol>
 *   <li>INSERTs one row into {@code contracts}</li>
 *   <li>Batch-INSERTs all {@code contract_lines} child rows using the generated {@code contract_id}</li>
 * </ol>
 *
 * <p>No L1 cache, no dirty checking, no sequence round-trips — Spring Data JDBC uses
 * plain JDBC under the hood, making it safe for high-volume batch workloads (9M+ contracts).
 */
public interface ContractEntityRepository extends CrudRepository<ContractEntity, Long> {
}
