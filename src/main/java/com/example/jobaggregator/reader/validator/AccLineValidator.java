package com.example.jobaggregator.reader.validator;

import static com.example.jobaggregator.reader.validator.LineValidationSupport.*;

import java.util.List;
import java.util.Set;

/**
 * Validates ACC (compte facturation) line fields — specification section 4.3.
 *
 * <pre>
 * Pos  Field      Mandatory  Rule
 * ---  ---------  ---------  -----------------------
 *  1   Type       Yes        fixed "ACC"
 *  2   Sous-type  Yes        BILL (facturation) or FEE (frais)
 *  3   BIC        Yes        bank BIC
 *  4   IBAN       Yes        account IBAN
 *  5   RIB        No
 * </pre>
 *
 * Example:
 * <pre>
 * ACC;BILL;BNPAFRPP;FR76300040212400001100885705;300040212400001100885705
 * ACC;FEE;BNPAFRPP;FR76300040212400001100885705;300040212400001100885705
 * </pre>
 */
public final class AccLineValidator {

    private static final String TYPE = "ACC";

    /** BILL = facturation, FEE = frais */
    public static final Set<String> VALID_SUBTYPES = Set.of("BILL", "FEE");

    private AccLineValidator() {}

    public static void validate(List<String> fields, int lineNumber) {
        requireMinSize(fields, 4,  TYPE, lineNumber);
        requireOneOf  (fields, 1, "Sous-type", VALID_SUBTYPES, TYPE, lineNumber);
        requireNonBlank(fields, 2, "BIC",  TYPE, lineNumber);
        requireNonBlank(fields, 3, "IBAN", TYPE, lineNumber);
        // field[4] RIB is optional
    }
}
