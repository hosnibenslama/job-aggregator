package com.bnpparibas.dsibddf.ap23992.modeldevente.batch.injector.domain;

/**
 * Typed domain representation of contract root header attributes.
 */
public record ContractHeader(
        String devise,
        String state,
        String motif,
        String ouDistribution,
        String ouManagement,
        String addressId,
        String businessRelationship,
        String effectiveDate,
        String periodeFacturation,
        String datesFacturation,
        String clientType,
        String opSdo,
        String closingDate,
        String context,
        String channel,
        String media) {}
