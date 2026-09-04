package com.example.jobaggregator.reader.validator;

import java.util.List;

/**
 * Strategy interface for validating the parsed fields of a specific business line type.
 * Implementations are registered per {@link com.example.jobaggregator.domain.LineType}
 * in {@link com.example.jobaggregator.reader.BusinessLineMapper}.
 */
@FunctionalInterface
public interface LineValidator {
    void validate(List<String> fields, int lineNumber);
}
