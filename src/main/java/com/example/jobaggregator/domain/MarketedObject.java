package com.example.jobaggregator.domain;

import java.util.List;

/**
 * Domain representation of a marketed object (objet marketé - OM) under a contract.
 * Holds OM-level external IDs, commercial roles, tariffs, advantages, and attached articles.
 */
public record MarketedObject(
        String omId,
        String businessRelationship,
        List<ExternalId> externalIds,
        List<Role> roles,
        List<Tarif> tarifs,
        List<Advantage> advantages,
        List<Article> articles) {

    public MarketedObject(String omId, String businessRelationship) {
        this(omId, businessRelationship, List.of(), List.of(), List.of(), List.of(), List.of());
    }
}
