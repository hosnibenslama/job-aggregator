package com.bnpparibas.dsibddf.ap23992.modeldevente.batch.injector.reader.validator;

import com.bnpparibas.dsibddf.ap23992.modeldevente.batch.injector.domain.feed.FeedRecordType;
import com.bnpparibas.dsibddf.ap23992.modeldevente.batch.injector.error.ContractFormatException;
import java.util.List;
import java.util.Set;
import java.util.regex.Pattern;

/**
 * Static validation constraints shared by all per-type {@link LineFieldValidator} implementations.
 * Not meant to be instantiated.
 */
public final class FieldConstraints {

    private static final Pattern HEX_16 = Pattern.compile("[0-9a-fA-F]{16}");

    private FieldConstraints() {}

    // -----------------------------------------------------------------------
    // Presence helpers
    // -----------------------------------------------------------------------

    public static String getField(List<String> fields, int index) {
        return index < fields.size() ? fields.get(index) : null;
    }

    public static boolean isPresent(List<String> fields, int index) {
        String val = getField(fields, index);
        return val != null && !val.isBlank();
    }

    // -----------------------------------------------------------------------
    // Mandatory field validators
    // -----------------------------------------------------------------------

    public static void requireMinSize(List<String> fields, int min, FeedRecordType recordType, int lineNumber) {
        if (fields.size() < min) {
            throw new ContractFormatException(lineNumber, null,
                    recordType + " requires at least " + min + " fields, got " + fields.size());
        }
    }

    public static void requireNonBlank(List<String> fields, int index,
                                        String fieldName, FeedRecordType recordType, int lineNumber) {
        if (!isPresent(fields, index)) {
            throw new ContractFormatException(lineNumber, null,
                    getFieldLabel(recordType, index, fieldName) + " is required and must not be blank");
        }
    }

    public static void requireHex16(List<String> fields, int index,
                                     String fieldName, FeedRecordType recordType, int lineNumber) {
        String val = getField(fields, index);
        if (val == null || !HEX_16.matcher(val).matches()) {
            throw new ContractFormatException(lineNumber, null,
                    getFieldLabel(recordType, index, fieldName)
                            + " must be exactly 16 hexadecimal characters, got: " + val);
        }
    }

    public static void requireOneOf(List<String> fields, int index,
                                     String fieldName, Set<String> validValues,
                                     FeedRecordType recordType, int lineNumber) {
        String val = getField(fields, index);
        if (val == null || !validValues.contains(val)) {
            throw new ContractFormatException(lineNumber, null,
                    getFieldLabel(recordType, index, fieldName)
                            + " must be one of " + validValues + ", got: " + val);
        }
    }

    public static void requirePositiveInt(List<String> fields, int index,
                                           String fieldName, FeedRecordType recordType, int lineNumber) {
        requireNonBlank(fields, index, fieldName, recordType, lineNumber);
        String val = getField(fields, index);
        try {
            if (Integer.parseInt(val) <= 0) {
                throw new ContractFormatException(lineNumber, null,
                        getFieldLabel(recordType, index, fieldName) + " must be positive, got: " + val);
            }
        } catch (NumberFormatException e) {
            throw new ContractFormatException(lineNumber, null,
                    getFieldLabel(recordType, index, fieldName) + " must be an integer, got: " + val, e);
        }
    }

    // -----------------------------------------------------------------------
    // Optional field validators (only enforced when field is non-blank)
    // -----------------------------------------------------------------------

    public static void requireOneOfIfPresent(List<String> fields, int index,
                                              String fieldName, Set<String> validValues,
                                              FeedRecordType recordType, int lineNumber) {
        String val = getField(fields, index);
        if (val != null && !val.isBlank() && !validValues.contains(val)) {
            throw new ContractFormatException(lineNumber, null,
                    getFieldLabel(recordType, index, fieldName)
                            + " must be one of " + validValues + " when present, got: " + val);
        }
    }

    // -----------------------------------------------------------------------
    // Internal helpers
    // -----------------------------------------------------------------------

    /** Builds a consistent field label: {@code "CTR field 2 (idContrat)"}. */
    private static String getFieldLabel(FeedRecordType recordType, int index, String fieldName) {
        return recordType + " field " + (index + 1) + " (" + fieldName + ")";
    }
}
