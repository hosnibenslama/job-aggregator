package com.bnpparibas.dsibddf.ap23992.modeldevente.batch.injector.reader.validator;

import static com.bnpparibas.dsibddf.ap23992.modeldevente.batch.injector.domain.feed.FeedRecordType.ACC;
import static com.bnpparibas.dsibddf.ap23992.modeldevente.batch.injector.reader.validator.FieldConstraints.*;

import java.util.List;

/**
 * Validates ACC (compte facturation) line fields — specification section 4.3.
 *
 * <pre>
 * Pos  Field      Mandatory  Rule
 * ---  ---------  ---------  -----------------------
 *  1   Type       Yes        fixed "ACC"
 *  2   BIC        Yes        bank BIC
 *  3   IBAN       Yes        account IBAN
 *  4   RIB        No
 * </pre>
 *
 * Example:
 * <pre>
 * ACC;BNPAFRPP;FR76300040212400001100885705;300040212400001100885705
 * </pre>
 */
public final class AccountValidator {

    private AccountValidator() {}

    public static void validate(List<String> fields, int lineNumber) {
        requireMinSize(fields, 3,  ACC, lineNumber);
        requireNonBlank(fields, 1, "BIC",  ACC, lineNumber);
        requireNonBlank(fields, 2, "IBAN", ACC, lineNumber);
        // field[3] RIB is optional
    }
}
