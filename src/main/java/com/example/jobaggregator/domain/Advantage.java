package com.example.jobaggregator.domain;

/**
 * Domain representation of a personalized advantage (avantage personnalisé) linked to a contract.
 */
public record Advantage(
        String idOpraAvantage,
        String dateDebut,
        String dateFin,
        String codeAvantage,
        String valeurAvantage,
        String deviseAvantage) {}
