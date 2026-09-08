package com.bnpparibas.dsibddf.ap23992.modelevente.batch.injector.persistence;

import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;

/**
 * Spring Data JPA repository for the {@link ContractEntity} aggregate root.
 */
public interface ContractEntityRepository extends JpaRepository<ContractEntity, UUID> {
}
