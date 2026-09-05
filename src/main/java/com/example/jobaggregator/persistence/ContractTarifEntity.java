package com.example.jobaggregator.persistence;

import org.springframework.data.annotation.Id;
import org.springframework.data.relational.core.mapping.Table;

@Table("contract_tarifs")
public class ContractTarifEntity {

    @Id
    private Long id;
    private String idOpraTarif;
    private String typeFrais;
    private String dateCreationTarif;
    private String dateEffetTarif;
    private String deviseTarif;
    private String indicTarifPaliers;
    private String formatTarif;
    private String periodiciteFacturation;
    private String typeTaxation;
    private String typeTauxTarif;
    private String tauxTarif;
    private String montantBase;
    private String ratioTarif;
    private String montantUnite;
    private String typeUnite;
    private String indicLimiteHaute;
    private String limiteHauteMontant;
    private String indicLimiteBasse;
    private String limiteBasseMontant;

    public ContractTarifEntity() {}

    public Long getId() { return id; }
    public String getIdOpraTarif() { return idOpraTarif; }
    public void setIdOpraTarif(String idOpraTarif) { this.idOpraTarif = idOpraTarif; }
    public String getTypeFrais() { return typeFrais; }
    public void setTypeFrais(String typeFrais) { this.typeFrais = typeFrais; }
    public String getDateCreationTarif() { return dateCreationTarif; }
    public void setDateCreationTarif(String dateCreationTarif) { this.dateCreationTarif = dateCreationTarif; }
    public String getDateEffetTarif() { return dateEffetTarif; }
    public void setDateEffetTarif(String dateEffetTarif) { this.dateEffetTarif = dateEffetTarif; }
    public String getDeviseTarif() { return deviseTarif; }
    public void setDeviseTarif(String deviseTarif) { this.deviseTarif = deviseTarif; }
    public String getIndicTarifPaliers() { return indicTarifPaliers; }
    public void setIndicTarifPaliers(String indicTarifPaliers) { this.indicTarifPaliers = indicTarifPaliers; }
    public String getFormatTarif() { return formatTarif; }
    public void setFormatTarif(String formatTarif) { this.formatTarif = formatTarif; }
    public String getPeriodiciteFacturation() { return periodiciteFacturation; }
    public void setPeriodiciteFacturation(String periodiciteFacturation) { this.periodiciteFacturation = periodiciteFacturation; }
    public String getTypeTaxation() { return typeTaxation; }
    public void setTypeTaxation(String typeTaxation) { this.typeTaxation = typeTaxation; }
    public String getTypeTauxTarif() { return typeTauxTarif; }
    public void setTypeTauxTarif(String typeTauxTarif) { this.typeTauxTarif = typeTauxTarif; }
    public String getTauxTarif() { return tauxTarif; }
    public void setTauxTarif(String tauxTarif) { this.tauxTarif = tauxTarif; }
    public String getMontantBase() { return montantBase; }
    public void setMontantBase(String montantBase) { this.montantBase = montantBase; }
    public String getRatioTarif() { return ratioTarif; }
    public void setRatioTarif(String ratioTarif) { this.ratioTarif = ratioTarif; }
    public String getMontantUnite() { return montantUnite; }
    public void setMontantUnite(String montantUnite) { this.montantUnite = montantUnite; }
    public String getTypeUnite() { return typeUnite; }
    public void setTypeUnite(String typeUnite) { this.typeUnite = typeUnite; }
    public String getIndicLimiteHaute() { return indicLimiteHaute; }
    public void setIndicLimiteHaute(String indicLimiteHaute) { this.indicLimiteHaute = indicLimiteHaute; }
    public String getLimiteHauteMontant() { return limiteHauteMontant; }
    public void setLimiteHauteMontant(String limiteHauteMontant) { this.limiteHauteMontant = limiteHauteMontant; }
    public String getIndicLimiteBasse() { return indicLimiteBasse; }
    public void setIndicLimiteBasse(String indicLimiteBasse) { this.indicLimiteBasse = indicLimiteBasse; }
    public String getLimiteBasseMontant() { return limiteBasseMontant; }
    public void setLimiteBasseMontant(String limiteBasseMontant) { this.limiteBasseMontant = limiteBasseMontant; }
}
