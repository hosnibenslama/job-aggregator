package com.example.jobaggregator.domain;

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
        String xB3TraceId,
        String xB3SpanId,
        String userId,
        String channel,
        String media) {}
