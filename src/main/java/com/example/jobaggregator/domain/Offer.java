package com.example.jobaggregator.domain;

/**
 * Domain representation of a commercial offer (offre) linked to a contract.
 */
public record Offer(String offerId, String provider, String personalizedLabel) {}
