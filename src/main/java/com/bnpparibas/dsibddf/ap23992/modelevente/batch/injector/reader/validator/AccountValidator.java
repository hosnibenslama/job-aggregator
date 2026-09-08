package com.bnpparibas.dsibddf.ap23992.modelevente.batch.injector.reader.validator;

import static com.bnpparibas.dsibddf.ap23992.modelevente.batch.injector.domain.feed.FeedRecordType.ACC;
import static com.bnpparibas.dsibddf.ap23992.modelevente.batch.injector.reader.validator.FieldConstraints.*;

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
public final class AccountValidator {

    /** BILL = facturation, FEE = frais */
    public static final Set<String> VALID_SUBTYPES = Set.of("BILL", "FEE");

    private AccountValidator() {}

    public static void validate(List<String> fields, int lineNumber) {
        requireMinSize(fields, 4,  ACC, lineNumber);
        requireOneOf  (fields, 1, "Sous-type", VALID_SUBTYPES, ACC, lineNumber);
        requireNonBlank(fields, 2, "BIC",  ACC, lineNumber);
        requireNonBlank(fields, 3, "IBAN", ACC, lineNumber);
        // field[4] RIB is optional
    }
}
