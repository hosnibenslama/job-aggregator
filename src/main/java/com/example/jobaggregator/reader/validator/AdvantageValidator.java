package com.example.jobaggregator.reader.validator;

import static com.example.jobaggregator.reader.validator.FieldConstraints.*;

import com.example.jobaggregator.error.ContractFormatException;
import java.util.List;
import java.util.Set;

/**
 * Validates AVT (avantage personnalisé) line fields — specification section 6.
 *
 * <pre>
 * Pos  Field           Mandatory  Rule
 * ---  --------------  ---------  -----------------------------------------------
 *  1   Type            Yes        fixed "AVT"
 *  2   idOpraAvantage  Cond.      required when codeAvantage = 1
 *  3   dateDebut       Yes        ISO-8601 instant (YYYY-MM-DDThh:mm:ss.ssssssZ)
 *  4   dateFin         No         ISO-8601 instant; blank = valid for life
 *  5   codeAvantage    Yes        1, 2, 3 or 4
 *  6   valeurAvantage  Cond.      required when codeAvantage = 2, 3 or 4
 *  7   deviseAvantage  Cond.      required when valeurAvantage is present (max 3 chars)
 * </pre>
 *
 * Examples:
 * <pre>
 * AVT;OPRA-000000000001;2026-01-01T00:00:00.000000Z;;1;;
 * AVT;;2026-01-01T00:00:00.000000Z;2026-12-31T23:59:59.999999Z;2;50.00;EUR
 * AVT;;2026-01-01T00:00:00.000000Z;2026-06-30T23:59:59.999999Z;3;15.00;EUR
 * AVT;;2026-01-01T00:00:00.000000Z;;4;10.50;EUR
 * </pre>
 */
public final class AdvantageValidator {

    private static final String TYPE = "AVT";

    private static final Set<String> VALID_CODES = Set.of("1", "2", "3", "4");
    /** Codes that require a valeurAvantage */
    private static final Set<String> CODES_REQUIRING_VALEUR = Set.of("2", "3", "4");

    private AdvantageValidator() {}

    public static void validate(List<String> fields, int lineNumber) {
        requireMinSize(fields, 5, TYPE, lineNumber);

        // Field 3 — dateDebut (mandatory)
        requireNonBlank(fields, 2, "dateDebut", TYPE, lineNumber);

        // Field 5 — codeAvantage (mandatory, must be 1/2/3/4)
        requireOneOf(fields, 4, "codeAvantage", VALID_CODES, TYPE, lineNumber);

        String codeAvantage = field(fields, 4);

        // Field 2 — idOpraAvantage: required when codeAvantage = 1
        if ("1".equals(codeAvantage) && !isPresent(fields, 1)) {
            throw new ContractFormatException(lineNumber, null,
                    TYPE + " field 2 (idOpraAvantage) is required when codeAvantage = 1");
        }

        // Field 6 — valeurAvantage: required when codeAvantage = 2, 3 or 4
        boolean valeurRequired = CODES_REQUIRING_VALEUR.contains(codeAvantage);
        if (valeurRequired && !isPresent(fields, 5)) {
            throw new ContractFormatException(lineNumber, null,
                    TYPE + " field 6 (valeurAvantage) is required when codeAvantage = " + codeAvantage);
        }

        // Field 7 — deviseAvantage: required when valeurAvantage is present
        boolean valeurPresent = isPresent(fields, 5);
        if (valeurPresent && !isPresent(fields, 6)) {
            throw new ContractFormatException(lineNumber, null,
                    TYPE + " field 7 (deviseAvantage) is required when valeurAvantage is present");
        }

        // deviseAvantage max 3 chars
        if (isPresent(fields, 6)) {
            String devise = field(fields, 6);
            if (devise.length() > 3) {
                throw new ContractFormatException(lineNumber, null,
                        TYPE + " field 7 (deviseAvantage) must not exceed 3 characters, got: " + devise);
            }
        }
    }
}
