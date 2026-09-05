package com.example.jobaggregator.domain;

import java.util.List;

/**
 * Domain representation of a commercial article (article / service) attached to a marketed object (OM).
 * Contains technical attributes (IKAC), conditions, tariffs, external IDs, and article-level roles/accounts.
 */
public record Article(
        int sequentialIndex,
        List<ExternalId> externalIds,
        List<Ikac> ikacs,
        List<Condition> conditions,
        List<Account> accounts,
        List<Role> roles,
        List<Tarif> tarifs,
        List<Advantage> advantages) {

    public Article(int sequentialIndex) {
        this(sequentialIndex, List.of(), List.of(), List.of(), List.of(), List.of(), List.of(), List.of());
    }
}
