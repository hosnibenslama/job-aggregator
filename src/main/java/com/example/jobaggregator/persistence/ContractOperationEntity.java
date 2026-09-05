package com.example.jobaggregator.persistence;

import org.springframework.data.annotation.Id;
import org.springframework.data.relational.core.mapping.Table;

@Table("contract_operations")
public class ContractOperationEntity {

    @Id
    private Long id;
    private String omId;
    private String businessRelationship;

    public ContractOperationEntity() {}

    public ContractOperationEntity(String omId, String businessRelationship) {
        this.omId = omId;
        this.businessRelationship = businessRelationship;
    }

    public Long getId() { return id; }
    public String getOmId() { return omId; }
    public void setOmId(String omId) { this.omId = omId; }
    public String getBusinessRelationship() { return businessRelationship; }
    public void setBusinessRelationship(String businessRelationship) { this.businessRelationship = businessRelationship; }
}
