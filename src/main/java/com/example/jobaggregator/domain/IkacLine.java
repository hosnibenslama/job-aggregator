package com.example.jobaggregator.domain;

/**
 * Typed view of an IKAC line — specification section 6.
 * Attached to the first article of a contract.
 *
 * <ol>
 *   <li>Type — fixed "IKAC"</li>
 *   <li>ikacValue — the IKAC value — mandatory</li>
 * </ol>
 */
public record IkacLine(String ikacValue) {

    public static IkacLine from(ParsedLine line) {
        if (line.type() != LineType.IKAC) {
            throw new IllegalArgumentException("Expected IKAC line but got: " + line.type());
        }
        return new IkacLine(line.field(1));
    }
}
