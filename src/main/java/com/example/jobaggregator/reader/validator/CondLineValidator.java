package com.example.jobaggregator.reader.validator;

import static com.example.jobaggregator.reader.validator.FieldConstraints.*;

import java.util.List;

/**
 * Validates COND (condition commerciale) line fields — specification section 4.9.
 *
 * <pre>
 * Pos  Field                   Mandatory  Rule
 * ---  ----------------------  ---------  -----------------------
 *  1   Type                    Yes        fixed "COND"
 *  2   Identifiant condition   Yes        e.g. PAR-0000006879471
 *  3   Valeur                  Yes        condition value
 * </pre>
 *
 * Examples:
 * <pre>
 * COND;PAR-0000006879471;DI
 * COND;PAR-0000007160920;123456
 * </pre>
 */
public final class CondLineValidator {

    private static final String TYPE = "COND";

    private CondLineValidator() {}

    public static void validate(List<String> fields, int lineNumber) {
        requireMinSize(fields, 3, TYPE, lineNumber);
        requireNonBlank(fields, 1, "Identifiant condition", TYPE, lineNumber);
        requireNonBlank(fields, 2, "Valeur", TYPE, lineNumber);
    }
}
