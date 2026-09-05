package com.example.jobaggregator.reader.validator;

import static com.example.jobaggregator.reader.validator.FieldConstraints.*;

import java.util.List;

/**
 * Validates OM (objet marketé) line fields — specification section 4.4.
 * Represents the 'products' level of the JSON source, attached to the current contract.
 *
 * <pre>
 * Pos  Field                  Mandatory
 * ---  ---------------------  ---------
 *  1   Type                   Yes  fixed "OM"
 *  2   OM identifier          Yes  internal reference (e.g. 00058680432692016)
 *  3   BusinessRelationship   Yes  commercial relationship of the OM holder
 * </pre>
 *
 * Example: {@code OM;00058680432692016;000058680432692016}
 */
public final class OmValidator {

    private static final String TYPE = "OM";

    private OmValidator() {}

    public static void validate(List<String> fields, int lineNumber) {
        requireMinSize (fields, 3, TYPE, lineNumber);
        requireNonBlank(fields, 1, "OM identifier",        TYPE, lineNumber);
        requireNonBlank(fields, 2, "BusinessRelationship", TYPE, lineNumber);
    }
}
