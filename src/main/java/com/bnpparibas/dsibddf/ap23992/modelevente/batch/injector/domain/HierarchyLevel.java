package com.bnpparibas.dsibddf.ap23992.modelevente.batch.injector.domain;

/**
 * Discriminator for the hierarchical level at which a child entity
 * (tarif, advantage, role, account, external ID) is attached within
 * the contract tree: Contract → Marketed Object (OM) → Article.
 */
public enum HierarchyLevel {
    CONTRACT,
    OM,
    ARTICLE;

    /**
     * Returns the database column value for this level.
     */
    public String getValue() {
        return name();
    }
}
