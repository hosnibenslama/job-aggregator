package com.example.jobaggregator.domain;

/**
 * Typed view of a TAR (tarif) line — specification section 4.8.
 * All fields beyond the type are optional.
 *
 * <ol>
 *   <li>Type — fixed "TAR"</li>
 *   <li>idOpraTarif — OPRA tarif code — optional</li>
 *   <li>typeFrais — fee type (001-007/013/014/900) — optional</li>
 *   <li>dateCreationTarif — creation date (YYYY-MM-DDTHH:MM:SS.ssssssZ) — optional</li>
 *   <li>dateEffetTarif — effect date (YYYY-MM-DDTHH:MM:SS.ssssssZ) — optional</li>
 *   <li>deviseTarif — tarif currency (e.g. EUR) — optional</li>
 *   <li>indicTarifPaliers — tiered tarif indicator: 1=vrai, 0=faux — optional</li>
 *   <li>formatTarif — tarif format (001=forfaitaire, 002=par unité, 003=taux) — optional</li>
 *   <li>periodiciteFacturation — billing periodicity (007=mensuelle, 010=trimestrielle, 012=annuelle) — optional</li>
 *   <li>typeTaxation — taxation type (001–006) — optional</li>
 * </ol>
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
        String typeTaxation) {

    public static TarLine from(BusinessLine line) {
        if (line.type() != LineType.TAR) {
            throw new IllegalArgumentException("Expected TAR line but got: " + line.type());
        }
        return new TarLine(
                line.field(1),  // idOpraTarif (optional)
                line.field(2),  // typeFrais (optional)
                line.field(3),  // dateCreationTarif (optional)
                line.field(4),  // dateEffetTarif (optional)
                line.field(5),  // deviseTarif (optional)
                line.field(6),  // indicTarifPaliers (optional)
                line.field(7),  // formatTarif (optional)
                line.field(8),  // periodiciteFacturation (optional)
                line.field(9)); // typeTaxation (optional)
    }
}
