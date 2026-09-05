package com.example.jobaggregator.domain;

/**
 * Domain representation of a commercial role (tiers commercial) associated with a contract.
 */
public record Role(String role, String brand, String scope, String holderId, String ikpi) {}
