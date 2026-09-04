package com.example.jobaggregator.domain;

/**
 * Typed view of an ART (article/service) line — specification section 4.6.
 *
 * <ol>
 *   <li>Type — fixed "ART"</li>
 *   <li>sequentialIndex — sequential index N (1..N) — mandatory</li>
 * </ol>
 */
public record ArtLine(int sequentialIndex) {

    public static ArtLine from(ParsedLine line) {
        if (line.type() != LineType.ART) {
            throw new IllegalArgumentException("Expected ART line but got: " + line.type());
        }
        return new ArtLine(Integer.parseInt(line.field(1)));
    }
}
