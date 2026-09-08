package com.bnpparibas.dsibddf.ap23992.modeldevente.batch.injector.persistence;

import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

@Entity
@Table(name = "contract_ikac")
public class ContractIkacEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
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
