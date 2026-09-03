package com.example.jobaggregator.persistence;

import org.springframework.data.annotation.Id;
import org.springframework.data.relational.core.mapping.Table;

/**
 * Spring Data JDBC child entity representing one raw business line within a contract.
 * Mapped to {@code contract_lines} via the {@code contract_id} foreign key set by the
 * parent {@link ContractEntity} aggregate.
 */
@Table("contract_lines")
public class ContractLineEntity {

    @Id
    private Long id;

    private long lineNumber;
    private String lineType;
    private String rawLine;

    public ContractLineEntity(long lineNumber, String lineType, String rawLine) {
        this.lineNumber = lineNumber;
        this.lineType = lineType;
        this.rawLine = rawLine;
    }

    public Long getId() { return id; }
    public long getLineNumber() { return lineNumber; }
    public String getLineType() { return lineType; }
    public String getRawLine() { return rawLine; }
}
