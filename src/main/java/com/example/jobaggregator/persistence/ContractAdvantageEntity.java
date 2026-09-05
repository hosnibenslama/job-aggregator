package com.example.jobaggregator.persistence;

import org.springframework.data.annotation.Id;
import org.springframework.data.relational.core.mapping.Table;

@Table("contract_advantages")
public class ContractAdvantageEntity {

    @Id
    private Long id;
    private String idOpraAvantage;
    private String dateDebut;
    private String dateFin;
    private String codeAvantage;
    private String valeurAvantage;
    private String deviseAvantage;

    public ContractAdvantageEntity() {}

    public ContractAdvantageEntity(String idOpraAvantage, String dateDebut, String dateFin,
                                   String codeAvantage, String valeurAvantage, String deviseAvantage) {
        this.idOpraAvantage = idOpraAvantage;
        this.dateDebut = dateDebut;
        this.dateFin = dateFin;
        this.codeAvantage = codeAvantage;
        this.valeurAvantage = valeurAvantage;
        this.deviseAvantage = deviseAvantage;
    }

    public Long getId() { return id; }
    public String getIdOpraAvantage() { return idOpraAvantage; }
    public void setIdOpraAvantage(String idOpraAvantage) { this.idOpraAvantage = idOpraAvantage; }
    public String getDateDebut() { return dateDebut; }
    public void setDateDebut(String dateDebut) { this.dateDebut = dateDebut; }
    public String getDateFin() { return dateFin; }
    public void setDateFin(String dateFin) { this.dateFin = dateFin; }
    public String getCodeAvantage() { return codeAvantage; }
    public void setCodeAvantage(String codeAvantage) { this.codeAvantage = codeAvantage; }
    public String getValeurAvantage() { return valeurAvantage; }
    public void setValeurAvantage(String valeurAvantage) { this.valeurAvantage = valeurAvantage; }
    public String getDeviseAvantage() { return deviseAvantage; }
    public void setDeviseAvantage(String deviseAvantage) { this.deviseAvantage = deviseAvantage; }
}
