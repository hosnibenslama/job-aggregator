package com.bnpparibas.dsibddf.ap23992.modeldevente.batch.injector.persistence;

import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

@Entity
@Table(name = "contract_marketed_objects")
public class ContractMarketedObjectEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    private String omId;
    private String businessRelationship;

    public ContractMarketedObjectEntity() {}

    public ContractMarketedObjectEntity(String omId, String businessRelationship) {
        this.omId = omId;
        this.businessRelationship = businessRelationship;
    }

    public Long getId() { return id; }
    public String getOmId() { return omId; }
    public void setOmId(String omId) { this.omId = omId; }
    public String getBusinessRelationship() { return businessRelationship; }
    public void setBusinessRelationship(String businessRelationship) { this.businessRelationship = businessRelationship; }
}
