package com.example.jobaggregator.domain;

/**
 * Typed view of a TAR (tarif) line — specification section 4.8.
 * All fields beyond the type are optional. Fields 11-20 added per spec continuation.
 *
 * <pre>
 * Pos  Field                   Rule (when present)
 * ---  ----------------------  -----------------------------------------------
 *  1   Type                    fixed "TAR"
 *  2   idOpraTarif
 *  3   typeFrais               001/002/003/004/005/006/007/013/014/900
 *  4   dateCreationTarif       YYYY-MM-DDTHH:MM:SS.ssssssZ
 *  5   dateEffetTarif          YYYY-MM-DDTHH:MM:SS.ssssssZ
 *  6   deviseTarif             e.g. EUR
 *  7   indicTarifPaliers       0=simple, 1=paliers
 *  8   formatTarif             001=forfaitaire, 002=par unité, 003=taux
 *  9   periodiciteFacturation  007=mensuelle, 010=trimestrielle, 012=annuelle
 * 10   typeTaxation            001=HT, 002=TTC, 003=HT-inconnu, 004=TTC-inconnu,
 *                              005=HT DOM-TOM, 006=TTC DOM-TOM
 * 11   typeTauxTarif           Required if formatTarif=003: 001=intérêts, 002=crédit
 * 12   tauxTarif               Decimal — required if formatTarif=001 (montant forfaitaire)
 * 13   montantBase             Decimal — required if formatTarif=001 (montant forfaitaire)
 * 14   ratioTarif              Decimal — ratio
 * 15   montantUnite            Decimal — required if formatTarif=002 (montant par unité)
 * 16   typeUnite               Required if formatTarif=002: 001-022
 *                              (événement/opération/dizaine/centaine/millier/unité/effet/
 *                               facture/fichier/relance/modification/notification/ordre/
 *                               paiement/remise/token/virement/alerte/bordereau/écritures/demande)
 * 17   indicLimiteHaute        0=limite renseignée, 1=limite non renseignée
 * 18   limiteHauteMontant      Decimal
 * 19   indicLimiteBasse        0=limite renseignée, 1=limite non renseignée
 * 20   limiteBasseMontant      Decimal
 * </pre>
 */
public record TarLine(
        String idOpraTarif,
        String typeFrais,
        String dateCreationTarif,
        String dateEffetTarif,
        String deviseTarif,
        String indicTarifPaliers,
        String formatTarif,
        String periodiciteFacturation,
        String typeTaxation,
        // Fields 11-20
        String typeTauxTarif,
        String tauxTarif,
        String montantBase,
        String ratioTarif,
        String montantUnite,
        String typeUnite,
        String indicLimiteHaute,
        String limiteHauteMontant,
        String indicLimiteBasse,
        String limiteBasseMontant) {

    public static TarLine from(ParsedLine line) {
        if (line.type() != LineType.TAR) {
            throw new IllegalArgumentException("Expected TAR line but got: " + line.type());
        }
        return new TarLine(
                line.field(1),   // idOpraTarif (optional)
                line.field(2),   // typeFrais (optional)
                line.field(3),   // dateCreationTarif (optional)
                line.field(4),   // dateEffetTarif (optional)
                line.field(5),   // deviseTarif (optional)
                line.field(6),   // indicTarifPaliers (optional)
                line.field(7),   // formatTarif (optional)
                line.field(8),   // periodiciteFacturation (optional)
                line.field(9),   // typeTaxation (optional)
                line.field(10),  // typeTauxTarif (optional, required if formatTarif=003)
                line.field(11),  // tauxTarif (optional, required if formatTarif=001)
                line.field(12),  // montantBase (optional, required if formatTarif=001)
                line.field(13),  // ratioTarif (optional)
                line.field(14),  // montantUnite (optional, required if formatTarif=002)
                line.field(15),  // typeUnite (optional, required if formatTarif=002)
                line.field(16),  // indicLimiteHaute (optional)
                line.field(17),  // limiteHauteMontant (optional)
                line.field(18),  // indicLimiteBasse (optional)
                line.field(19)); // limiteBasseMontant (optional)
    }
}
