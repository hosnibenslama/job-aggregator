package com.example.jobaggregator.domain;

/**
 * Typed view of an OFF (offre) line — specification section 4.5.
 *
 * <ol>
 *   <li>Type — fixed "OFF"</li>
 *   <li>offerId — offer identifier (e.g. OFF-0000000001090) — mandatory</li>
 *   <li>provider — provider code (e.g. AP00111) — mandatory</li>
 *   <li>personalizedLabel — personalised label — optional</li>
 * </ol>
 */
public record OffLine(String offerId, String provider, String personalizedLabel) {

    public static OffLine from(BusinessLine line) {
        if (line.type() != LineType.OFF) {
            throw new IllegalArgumentException("Expected OFF line but got: " + line.type());
        }
        return new OffLine(
                line.field(1),  // Offer ID
                line.field(2),  // Provider
                line.field(3)); // Personalized label (optional)
    }
}
