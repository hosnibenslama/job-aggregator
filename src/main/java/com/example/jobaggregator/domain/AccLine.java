package com.example.jobaggregator.domain;

/**
 * Typed view of an ACC (compte facturation) line — specification section 4.3.
 *
 * <ol>
 *   <li>Type — fixed "ACC"</li>
 *   <li>subType — BILL (facturation) or FEE (frais) — mandatory</li>
 *   <li>bic — bank BIC — mandatory</li>
 *   <li>iban — account IBAN — mandatory</li>
 *   <li>rib — associated RIB — optional</li>
 * </ol>
 */
public record AccLine(String subType, String bic, String iban, String rib) {

    public static AccLine from(BusinessLine line) {
        if (line.type() != LineType.ACC) {
            throw new IllegalArgumentException("Expected ACC line but got: " + line.type());
        }
        return new AccLine(
                line.field(1),  // subType (BILL/FEE)
                line.field(2),  // BIC
                line.field(3),  // IBAN
                line.field(4)); // RIB (optional)
    }
}
