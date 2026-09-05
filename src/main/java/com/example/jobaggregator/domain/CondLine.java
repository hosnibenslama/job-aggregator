package com.example.jobaggregator.domain;

/**
 * Typed view of a COND (condition commerciale) line — specification section 4.9.
 *
 * <ol>
 *   <li>Type — fixed "COND"</li>
 *   <li>conditionId — condition identifier (e.g. PAR-0000006879471) — mandatory</li>
 *   <li>conditionValue — condition value — mandatory</li>
 * </ol>
 */
public record CondLine(String conditionId, String conditionValue) {

    public static CondLine from(ParsedLine line) {
        if (line.type() != LineType.COND) {
            throw new IllegalArgumentException("Expected COND line but got: " + line.type());
        }
        return new CondLine(
                line.field(1),
                line.field(2));
    }
}
