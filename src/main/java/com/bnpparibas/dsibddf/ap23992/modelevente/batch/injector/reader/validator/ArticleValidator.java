package com.bnpparibas.dsibddf.ap23992.modelevente.batch.injector.reader.validator;

import static com.bnpparibas.dsibddf.ap23992.modelevente.batch.injector.domain.feed.FeedRecordType.ART;
import static com.bnpparibas.dsibddf.ap23992.modelevente.batch.injector.reader.validator.FieldConstraints.*;

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
public final class ArticleValidator {

    private ArticleValidator() {}

    public static void validate(List<String> fields, int lineNumber) {
        requireMinSize    (fields, 2, ART, lineNumber);
        requirePositiveInt(fields, 1, "Index séquentiel", ART, lineNumber);
    }
}
