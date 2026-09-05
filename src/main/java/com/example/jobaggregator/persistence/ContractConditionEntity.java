package com.example.jobaggregator.persistence;

import org.springframework.data.annotation.Id;
import org.springframework.data.relational.core.mapping.Table;

@Table("contract_conditions")
public class ContractConditionEntity {

    @Id
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
