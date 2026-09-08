package com.bnpparibas.dsibddf.ap23992.modeldevente.batch.injector.persistence;

import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

@Entity
@Table(name = "contract_articles")
public class ContractArticleEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    private int sequentialIndex;

    public ContractArticleEntity() {}

    public ContractArticleEntity(int sequentialIndex) {
        this.sequentialIndex = sequentialIndex;
    }

    public Long getId() { return id; }
    public int getSequentialIndex() { return sequentialIndex; }
    public void setSequentialIndex(int sequentialIndex) { this.sequentialIndex = sequentialIndex; }
}
