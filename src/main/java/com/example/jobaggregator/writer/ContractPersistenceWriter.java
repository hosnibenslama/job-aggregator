package com.example.jobaggregator.writer;

import com.example.jobaggregator.domain.Contract;
import com.example.jobaggregator.domain.CtrLine;
import com.example.jobaggregator.domain.ParsedLine;
import com.example.jobaggregator.persistence.ContractEntity;
import com.example.jobaggregator.persistence.ContractEntityRepository;
import com.example.jobaggregator.persistence.ContractLineEntity;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;
import org.springframework.batch.infrastructure.item.Chunk;
import org.springframework.batch.infrastructure.item.ItemWriter;
import org.springframework.stereotype.Component;

/**
 * Spring Batch writer that persists {@link Contract} domain objects via Spring Data JDBC.
 *
 * <p>Each chunk is saved in a single {@code saveAll()} call, allowing Spring Data JDBC
 * to batch the underlying INSERT statements within the chunk's transaction.
 */
@Component
public class ContractPersistenceWriter implements ItemWriter<Contract> {

    private final ContractEntityRepository repository;

    public ContractPersistenceWriter(ContractEntityRepository repository) {
        this.repository = repository;
    }

    @Override
    public void write(Chunk<? extends Contract> items) {
        List<ContractEntity> entities = items.getItems().stream()
                .map(this::toEntity)
                .toList();
        repository.saveAll(entities);
    }

    // -----------------------------------------------------------------------
    // Mapping: domain model → persistence model
    // -----------------------------------------------------------------------

    private ContractEntity toEntity(Contract contract) {
        CtrLine ctr = contract.ctrLine();
        Set<ContractLineEntity> lineEntities = contract.lines().stream()
                .map(this::toLineEntity)
                .collect(Collectors.toSet());

        ContractEntity entity = new ContractEntity();
        entity.setDevise(ctr.devise());
        entity.setState(ctr.state());
        entity.setMotif(blankToNull(ctr.motif()));
        entity.setOuDistribution(blankToNull(ctr.ouDistribution()));
        entity.setOuManagement(ctr.ouManagement());
        entity.setAddressId(blankToNull(ctr.addressId()));
        entity.setBusinessRelationship(ctr.businessRelationship());
        entity.setEffectiveDate(blankToNull(ctr.effectiveDate()));
        entity.setPeriodeFacturation(blankToNull(ctr.periodeFacturation()));
        entity.setDatesFacturation(blankToNull(ctr.datesFacturation()));
        entity.setXB3TraceId(ctr.xB3TraceId());
        entity.setXB3SpanId(ctr.xB3SpanId());
        entity.setUserId(ctr.userId());
        entity.setChannel(ctr.channel());
        entity.setMedia(ctr.media());
        entity.setLines(lineEntities);
        return entity;
    }

    private ContractLineEntity toLineEntity(ParsedLine line) {
        return new ContractLineEntity(line.lineNumber(), line.type().name(), line.raw());
    }

    private static String blankToNull(String value) {
        return (value == null || value.isBlank()) ? null : value;
    }
}
