package com.example.jobaggregator.persistence;

import org.springframework.data.annotation.Id;
import org.springframework.data.relational.core.mapping.Table;

@Table("contract_accounts")
public class ContractAccountEntity {

    @Id
    private Long id;
    private String subType;
    private String bic;
    private String iban;
    private String rib;

    public ContractAccountEntity() {}

    public ContractAccountEntity(String subType, String bic, String iban, String rib) {
        this.subType = subType;
        this.bic = bic;
        this.iban = iban;
        this.rib = rib;
    }

    public Long getId() { return id; }
    public String getSubType() { return subType; }
    public void setSubType(String subType) { this.subType = subType; }
    public String getBic() { return bic; }
    public void setBic(String bic) { this.bic = bic; }
    public String getIban() { return iban; }
    public void setIban(String iban) { this.iban = iban; }
    public String getRib() { return rib; }
    public void setRib(String rib) { this.rib = rib; }
}
