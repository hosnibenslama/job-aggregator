package com.example.jobaggregator.domain;

/**
 * Typed view of a ROL (tiers commercial) line — specification section 4.7.
 *
 * <ol>
 *   <li>Type — fixed "ROL"</li>
 *   <li>role — role code (e.g. 1) — mandatory</li>
 *   <li>brand — commercial brand code (e.g. 001) — mandatory</li>
 *   <li>scope — commercialThirdPartyScope (e.g. PRI) — mandatory</li>
 *   <li>holderId — holder identifier — mandatory</li>
 *   <li>ikpi — IKPI — mandatory</li>
 * </ol>
 *
 * Example: {@code ROL;1;001;PRI;01970013368500000;01970013368500002}
 */
public record RolLine(String role, String brand, String scope, String holderId, String ikpi) {

    public static RolLine from(BusinessLine line) {
        if (line.type() != LineType.ROL) {
            throw new IllegalArgumentException("Expected ROL line but got: " + line.type());
        }
        return new RolLine(
                line.field(1),  // Role
                line.field(2),  // Brand
                line.field(3),  // Scope
                line.field(4),  // Holder ID
                line.field(5)); // IKPI
    }
}
