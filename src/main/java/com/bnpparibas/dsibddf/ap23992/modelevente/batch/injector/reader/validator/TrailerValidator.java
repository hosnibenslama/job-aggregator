package com.bnpparibas.dsibddf.ap23992.modelevente.batch.injector.reader.validator;

import static com.bnpparibas.dsibddf.ap23992.modelevente.batch.injector.domain.feed.FeedRecordType.TRL;
import static com.bnpparibas.dsibddf.ap23992.modelevente.batch.injector.reader.validator.FieldConstraints.*;

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
public final class TrailerValidator {

    private TrailerValidator() {}

    public static void validate(List<String> fields, int lineNumber) {
        requireMinSize(fields, 2, TRL, lineNumber);
        requirePositiveInt(fields, 1, "NBCTR", TRL, lineNumber);
    }
}
