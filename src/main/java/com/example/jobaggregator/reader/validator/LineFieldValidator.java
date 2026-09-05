package com.example.jobaggregator.reader.validator;

import java.util.List;

/**
 * Strategy interface for validating the parsed fields of a specific record type.
 * Implementations are registered per {@link com.example.jobaggregator.domain.feed.FeedRecordType}
 * in {@link com.example.jobaggregator.reader.ContractLineMapper}.
 */
@FunctionalInterface
public interface LineFieldValidator {
    void validate(List<String> fields, int lineNumber);
}
