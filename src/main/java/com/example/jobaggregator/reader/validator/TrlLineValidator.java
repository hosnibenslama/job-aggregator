package com.example.jobaggregator.reader.validator;

import static com.example.jobaggregator.reader.validator.FieldConstraints.*;

import java.util.List;

/**
 * Validates TRL (trailer) line fields — specification section 7.
 *
 * <pre>
 * Pos  Field  Mandatory  Rule
 * ---  -----  ---------  -----------------------------------
 *  1   Type   Yes        fixed "TRL"
 *  2   NBCTR  Yes        number of CTR lines in the file (positive integer)
 * </pre>
 *
 * Example:
 * <pre>
 * TRL:1000
 * </pre>
 */
public final class TrlLineValidator {

    private static final String TYPE = "TRL";

    private TrlLineValidator() {}

    public static void validate(List<String> fields, int lineNumber) {
        requireMinSize(fields, 2, TYPE, lineNumber);
        requirePositiveInt(fields, 1, "NBCTR", TYPE, lineNumber);
    }
}
