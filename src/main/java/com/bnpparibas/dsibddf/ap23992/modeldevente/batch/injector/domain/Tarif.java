package com.bnpparibas.dsibddf.ap23992.modeldevente.batch.injector.domain;

/**
 * Domain representation of a tarif rule applied to an article.
 */
public record Tarif(
        String idOpraTarif,
        String typeFrais,
        String dateCreationTarif,
        String dateEffetTarif,
        String deviseTarif,
        String indicTarifPaliers,
        String formatTarif,
        String periodiciteFacturation,
        String typeTaxation,
        String typeTauxTarif,
        String tauxTarif,
        String montantBase,
        String ratioTarif,
        String montantUnite,
        String typeUnite,
        String indicLimiteHaute,
        String limiteHauteMontant,
        String indicLimiteBasse,
        String limiteBasseMontant) {}
