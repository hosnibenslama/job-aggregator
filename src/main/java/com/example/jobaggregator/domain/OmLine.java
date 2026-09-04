package com.example.jobaggregator.domain;

/**
 * Typed view of an OM (objet marketé) line — specification section 4.4.
 *
 * <ol>
 *   <li>Type — fixed "OM"</li>
 *   <li>omId — internal OM identifier — mandatory</li>
 *   <li>businessRelationship — commercial relationship of the OM holder — mandatory</li>
 * </ol>
 */
public record OmLine(String omId, String businessRelationship) {

    public static OmLine from(ParsedLine line) {
        if (line.type() != LineType.OM) {
            throw new IllegalArgumentException("Expected OM line but got: " + line.type());
        }
        return new OmLine(
                line.field(1),  // OM identifier
                line.field(2)); // BusinessRelationship (position 3)
    }
}
