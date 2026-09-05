package com.example.jobaggregator.persistence;

import org.springframework.data.annotation.Id;
import org.springframework.data.relational.core.mapping.Table;

@Table("contract_offers")
public class ContractOfferEntity {

    @Id
    private Long id;
    private String offerId;
    private String provider;
    private String personalizedLabel;

    public ContractOfferEntity() {}

    public ContractOfferEntity(String offerId, String provider, String personalizedLabel) {
        this.offerId = offerId;
        this.provider = provider;
        this.personalizedLabel = personalizedLabel;
    }

    public Long getId() { return id; }
    public String getOfferId() { return offerId; }
    public void setOfferId(String offerId) { this.offerId = offerId; }
    public String getProvider() { return provider; }
    public void setProvider(String provider) { this.provider = provider; }
    public String getPersonalizedLabel() { return personalizedLabel; }
    public void setPersonalizedLabel(String personalizedLabel) { this.personalizedLabel = personalizedLabel; }
}
