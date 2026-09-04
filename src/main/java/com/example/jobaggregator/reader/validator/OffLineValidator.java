package com.example.jobaggregator.reader.validator;

import static com.example.jobaggregator.reader.validator.FieldConstraints.*;

import java.util.List;

/**
 * Validates OFF (offre) line fields — specification section 4.5.
 * Represents an offer reference ('offer').
 *
 * <pre>
 * Pos  Field               Mandatory
 * ---  ------------------  ---------
 *  1   Type                Yes  fixed "OFF"
 *  2   Identifiant offre   Yes  e.g. OFF-0000000001090
 *  3   Provider            Yes  e.g. AP00111
 *  4   Label personnalisé  No   e.g. Carte VISA PREMIER DI
 * </pre>
 *
 * Example: {@code OFF;OFF-0000000001090;AP00111;Carte VISA PREMIER DI}
 */
public final class OffLineValidator {

    private static final String TYPE = "OFF";

    private OffLineValidator() {}

    public static void validate(List<String> fields, int lineNumber) {
        requireMinSize (fields, 3, TYPE, lineNumber);
        requireNonBlank(fields, 1, "Identifiant offre", TYPE, lineNumber);
        requireNonBlank(fields, 2, "Provider",          TYPE, lineNumber);
        // field[3] Label personnalisé is optional
    }
}
