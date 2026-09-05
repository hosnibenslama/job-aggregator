package com.example.jobaggregator.reader.validator;

import static com.example.jobaggregator.reader.validator.FieldConstraints.*;

import java.util.List;

/**
 * Validates IKAC line fields — specification section 6.
 *
 * <p>The IKAC line must be attached to the first article.
 *
 * <pre>
 * Pos  Field       Mandatory  Rule
 * ---  ----------  ---------  -----------------------
 *  1   Type        Yes        fixed "IKAC"
 *  2   Valeur IKAC Yes        the IKAC value
 * </pre>
 *
 * Example:
 * <pre>
 * IKAC;52050000000634205
 * </pre>
 */
public final class IkacValidator {

    private static final String TYPE = "IKAC";

    private IkacValidator() {}

    public static void validate(List<String> fields, int lineNumber) {
        requireMinSize(fields, 2, TYPE, lineNumber);
        requireNonBlank(fields, 1, "Valeur IKAC", TYPE, lineNumber);
    }
}
