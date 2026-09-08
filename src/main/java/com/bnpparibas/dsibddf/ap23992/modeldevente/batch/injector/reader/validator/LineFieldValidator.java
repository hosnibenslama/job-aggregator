package com.bnpparibas.dsibddf.ap23992.modeldevente.batch.injector.reader.validator;

import java.util.List;

/**
 * Strategy interface for validating the parsed fields of a specific record type.
 * Implementations are registered per {@link com.bnpparibas.dsibddf.ap23992.modeldevente.batch.injector.domain.feed.FeedRecordType}
 * in {@link com.bnpparibas.dsibddf.ap23992.modeldevente.batch.injector.reader.ContractLineMapper}.
 */
@FunctionalInterface
public interface LineFieldValidator {
    void validate(List<String> fields, int lineNumber);
}
