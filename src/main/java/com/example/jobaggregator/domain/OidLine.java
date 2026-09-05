package com.example.jobaggregator.domain;

/**
 * Typed view of an OID (identifiant externe) line — specification section 5.
 *
 * <ol>
 *   <li>Type — fixed "OID"</li>
 *   <li>externalId — external identifier (e.g. SER-0000000000637) — mandatory</li>
 * </ol>
 */
public record OidLine(String externalId) {

    public static OidLine from(ParsedLine line) {
        if (line.type() != LineType.OID) {
            throw new IllegalArgumentException("Expected OID line but got: " + line.type());
        }
        return new OidLine(line.field(1));
    }
}
