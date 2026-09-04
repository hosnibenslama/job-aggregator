package com.example.jobaggregator.persistence;

import org.springframework.data.annotation.Id;
import org.springframework.data.relational.core.mapping.Table;

/**
 * Spring Data JDBC child entity representing one raw business line within a contract.
 * Mapped to {@code contract_lines} via the {@code contract_id} foreign key set by the
 * parent {@link ContractEntity} aggregate.
 */
@Table("contract_lines")
public record ContractLineEntity(@Id Long id, long lineNumber, String lineType, String rawLine) {

    /** Convenience constructor for creating new (unpersisted) line entities. */
    public ContractLineEntity(long lineNumber, String lineType, String rawLine) {
        this(null, lineNumber, lineType, rawLine);
    }
}
