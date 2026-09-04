package com.example.jobaggregator.reader.validator;

import static com.example.jobaggregator.reader.validator.FieldConstraints.*;

import java.util.List;

/**
 * Validates ROL (tiers commercial) line fields — specification section 4.7.
 * Represents a commercial third party ('commercialThird').
 *
 * <pre>
 * Pos  Field      Mandatory  Rule
 * ---  ---------  ---------  -------------------------------------------
 *  1   Type       Yes        fixed "ROL"
 *  2   Role       Yes        role code (e.g. 1)
 *  3   Brand      Yes        commercial brand code (e.g. 001)
 *  4   Scope      Yes        commercialThirdPartyScope (e.g. PRI)
 *  5   Holder ID  Yes        holder identifier
 *  6   IKPI       Yes        IKPI
 * </pre>
 *
 * Example: {@code ROL;1;001;PRI;01970013368500000;01970013368500002}
 */
public final class RolLineValidator {

    private static final String TYPE = "ROL";

    private RolLineValidator() {}

    public static void validate(List<String> fields, int lineNumber) {
        requireMinSize (fields, 6, TYPE, lineNumber);
        requireNonBlank(fields, 1, "Role",      TYPE, lineNumber);
        requireNonBlank(fields, 2, "Brand",     TYPE, lineNumber);
        requireNonBlank(fields, 3, "Scope",     TYPE, lineNumber);
        requireNonBlank(fields, 4, "Holder ID", TYPE, lineNumber);
        requireNonBlank(fields, 5, "IKPI",      TYPE, lineNumber);
    }
}
