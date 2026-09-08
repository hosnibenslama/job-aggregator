package com.bnpparibas.dsibddf.ap23992.modeldevente.batch.injector.persistence;

import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

@Entity
@Table(name = "contract_conditions")
public class ContractConditionEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    private String conditionId;
    private String conditionValue;

    public ContractConditionEntity() {}

    public ContractConditionEntity(String conditionId, String conditionValue) {
        this.conditionId = conditionId;
        this.conditionValue = conditionValue;
    }

    public Long getId() { return id; }
    public String getConditionId() { return conditionId; }
    public void setConditionId(String conditionId) { this.conditionId = conditionId; }
    public String getConditionValue() { return conditionValue; }
    public void setConditionValue(String conditionValue) { this.conditionValue = conditionValue; }
}
