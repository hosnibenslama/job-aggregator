package com.example.jobaggregator.reader.validator;

import static com.example.jobaggregator.reader.validator.LineValidationSupport.*;

import java.util.List;

/**
 * Validates ART (article/service) line fields — specification section 4.6.
 * Represents an article/service attached to the current OM.
 *
 * <pre>
 * Pos  Field               Mandatory  Rule
 * ---  ------------------  ---------  -------------------
 *  1   Type                Yes        fixed "ART"
 *  2   Index séquentiel N  Yes        positive integer (1..N)
 * </pre>
 *
 * Example: {@code ART;1}, {@code ART;2}, {@code ART;3}
 */
public final class ArtLineValidator {

    private static final String TYPE = "ART";

    private ArtLineValidator() {}

    public static void validate(List<String> fields, int lineNumber) {
        requireMinSize    (fields, 2, TYPE, lineNumber);
        requirePositiveInt(fields, 1, "Index séquentiel", TYPE, lineNumber);
    }
}
