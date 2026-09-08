package com.bnpparibas.dsibddf.ap23992.modelevente.batch.injector.reader.validator;

import static com.bnpparibas.dsibddf.ap23992.modelevente.batch.injector.reader.validator.FieldConstraints.*;

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
 *  3   Label personnalisé  No   e.g. Carte VISA PREMIER DI
 * </pre>
 *
 * Example: {@code OFF;OFF-0000000001090;Carte VISA PREMIER DI}
 */
public final class OfferValidator {

    private static final String TYPE = "OFF";

    private OfferValidator() {}

    public static void validate(List<String> fields, int lineNumber) {
        requireMinSize (fields, 2, TYPE, lineNumber);
        requireNonBlank(fields, 1, "Identifiant offre", TYPE, lineNumber);
        // field[2] Label personnalisé is optional
    }
}
