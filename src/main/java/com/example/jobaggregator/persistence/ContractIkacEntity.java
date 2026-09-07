package com.example.jobaggregator.persistence;

import org.springframework.data.annotation.Id;
import org.springframework.data.relational.core.mapping.Table;

@Table("contract_ikac")
public class ContractIkacEntity {

    @Id
    private Long id;
    private String ikacValue;
    private String provider;

    public ContractIkacEntity() {}

    public ContractIkacEntity(String ikacValue, String provider) {
        this.ikacValue = ikacValue;
        this.provider = provider;
    }

    public Long getId() { return id; }
    public String getIkacValue() { return ikacValue; }
    public void setIkacValue(String ikacValue) { this.ikacValue = ikacValue; }
    public String getProvider() { return provider; }
    public void setProvider(String provider) { this.provider = provider; }
}
