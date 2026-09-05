package com.example.jobaggregator.reader.validator;

import java.util.List;

/**
 * Strategy interface for validating the parsed fields of a specific line type.
 * Implementations are registered per {@link com.example.jobaggregator.domain.LineType}
 * in {@link com.example.jobaggregator.reader.ContractLineMapper}.
 */
@FunctionalInterface
public interface LineFieldValidator {
    void validate(List<String> fields, int lineNumber);
}
