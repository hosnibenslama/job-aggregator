package com.bnpparibas.dsibddf.ap23992.modeldevente.batch.injector.persistence;

import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

@Entity
@Table(name = "contract_offers")
public class ContractOfferEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    private String offerId;
    private String personalizedLabel;

    public ContractOfferEntity() {}

    public ContractOfferEntity(String offerId, String personalizedLabel) {
        this.offerId = offerId;
        this.personalizedLabel = personalizedLabel;
    }

    public Long getId() { return id; }
    public String getOfferId() { return offerId; }
    public void setOfferId(String offerId) { this.offerId = offerId; }
    public String getPersonalizedLabel() { return personalizedLabel; }
    public void setPersonalizedLabel(String personalizedLabel) { this.personalizedLabel = personalizedLabel; }
}
