package com.example.jobaggregator.persistence;

import org.springframework.data.annotation.Id;
import org.springframework.data.relational.core.mapping.Table;

@Table("contract_articles")
public class ContractArticleEntity {

    @Id
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
