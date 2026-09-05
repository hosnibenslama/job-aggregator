package com.example.jobaggregator.persistence;

import org.springframework.data.annotation.Id;
import org.springframework.data.relational.core.mapping.Table;

@Table("contract_external_ids")
public class ContractExternalIdEntity {

    @Id
    private Long id;
    private String externalId;

    public ContractExternalIdEntity() {}

    public ContractExternalIdEntity(String externalId) {
        this.externalId = externalId;
    }

    public Long getId() { return id; }
    public String getExternalId() { return externalId; }
    public void setExternalId(String externalId) { this.externalId = externalId; }
}
