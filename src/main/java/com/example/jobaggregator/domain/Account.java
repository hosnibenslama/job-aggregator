package com.example.jobaggregator.domain;

/**
 * Domain representation of a contract billing or fee account (compte facturation).
 */
public record Account(String subType, String bic, String iban, String rib) {}
