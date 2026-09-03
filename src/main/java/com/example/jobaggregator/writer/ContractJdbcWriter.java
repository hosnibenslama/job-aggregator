package com.example.jobaggregator.writer;

import com.example.jobaggregator.domain.BusinessLine;
import com.example.jobaggregator.domain.Contract;
import com.example.jobaggregator.domain.CtrLine;
import com.example.jobaggregator.persistence.ContractEntity;
import com.example.jobaggregator.persistence.ContractEntityRepository;
import com.example.jobaggregator.persistence.ContractLineEntity;
import java.util.Set;
import java.util.stream.Collectors;
import org.springframework.batch.infrastructure.item.Chunk;
import org.springframework.batch.infrastructure.item.ItemWriter;
import org.springframework.stereotype.Component;

/**
 * Spring Batch writer that persists {@link Contract} domain objects via Spring Data JDBC.
 *
 * <p>Each {@code save()} call on {@link ContractEntityRepository} inserts one row
 * into {@code contracts} and batch-inserts all child rows into {@code contract_lines}
 * in a single transaction — no manual key propagation required.
 */
@Component
public class ContractJdbcWriter implements ItemWriter<Contract> {

    private final ContractEntityRepository repository;

    public ContractJdbcWriter(ContractEntityRepository repository) {
        this.repository = repository;
    }

    @Override
    public void write(Chunk<? extends Contract> items) {
        for (Contract contract : items) {
            repository.save(toEntity(contract));
        }
    }

    // -----------------------------------------------------------------------
    // Mapping: domain model → persistence model
    // -----------------------------------------------------------------------

    private ContractEntity toEntity(Contract contract) {
        CtrLine ctr = contract.ctrLine();
        Set<ContractLineEntity> lineEntities = contract.lines().stream()
                .map(this::toLineEntity)
                .collect(Collectors.toSet());

        return new ContractEntity(
                ctr.devise(),
                ctr.state(),
                blankToNull(ctr.motif()),
                blankToNull(ctr.ouDistribution()),
                ctr.ouManagement(),
                blankToNull(ctr.addressId()),
                ctr.businessRelationship(),
                blankToNull(ctr.effectiveDate()),
                blankToNull(ctr.periodeFacturation()),
                blankToNull(ctr.datesFacturation()),
                ctr.xB3TraceId(),
                ctr.xB3SpanId(),
                ctr.userId(),
                ctr.channel(),
                ctr.media(),
                lineEntities);
    }

    private ContractLineEntity toLineEntity(BusinessLine line) {
        return new ContractLineEntity(
                line.lineNumber(),
                line.type().name(),
                line.raw());
    }

    private static String blankToNull(String value) {
        return (value == null || value.isBlank()) ? null : value;
    }
}
