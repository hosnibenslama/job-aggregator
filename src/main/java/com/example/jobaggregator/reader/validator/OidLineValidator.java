package com.example.jobaggregator.reader.validator;

import static com.example.jobaggregator.reader.validator.FieldConstraints.*;

import java.util.List;

/**
 * Validates OID (identifiant externe) line fields — specification section 5.
 *
 * <pre>
 * Pos  Field        Mandatory  Rule
 * ---  -----------  ---------  -----------------------
 *  1   Type         Yes        fixed "OID"
 *  2   Identifiant  Yes        e.g. SER-0000000000637
 * </pre>
 *
 * Example:
 * <pre>
 * OID;SER-0000000000637
 * </pre>
 */
public final class OidLineValidator {

    private static final String TYPE = "OID";

    private OidLineValidator() {}

    public static void validate(List<String> fields, int lineNumber) {
        requireMinSize(fields, 2, TYPE, lineNumber);
        requireNonBlank(fields, 1, "Identifiant", TYPE, lineNumber);
    }
}
