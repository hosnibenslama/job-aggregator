package com.example.jobaggregator.domain;

/**
 * Typed view of an AVT (avantage personnalisé) line — specification section 6.
 *
 * <ol>
 *   <li>Type — fixed "AVT"</li>
 *   <li>idOpraAvantage — optional/conditional identifier in OPRA</li>
 *   <li>dateDebut — start validity date (ISO-8601) — mandatory</li>
 *   <li>dateFin — end validity date (ISO-8601) — optional (valid for life if empty)</li>
 *   <li>codeAvantage — advantage type code (1, 2, 3, or 4) — mandatory</li>
 *   <li>valeurAvantage — numerical value — optional/conditional</li>
 *   <li>deviseAvantage — currency (e.g. EUR) — optional/conditional</li>
 * </ol>
 */
public record AvtLine(
        String idOpraAvantage,
        String dateDebut,
        String dateFin,
        String codeAvantage,
        String valeurAvantage,
        String deviseAvantage) {

    public static AvtLine from(ParsedLine line) {
        if (line.type() != LineType.AVT) {
            throw new IllegalArgumentException("Expected AVT line but got: " + line.type());
        }
        return new AvtLine(
                line.field(1),
                line.field(2),
                line.field(3),
                line.field(4),
                line.field(5),
                line.field(6));
    }
}
