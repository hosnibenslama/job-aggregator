package com.bnpparibas.dsibddf.ap23992.modelevente.batch.injector.domain.feed;

import java.util.List;

/**
 * Represents an immutable semicolon-delimited feed record parsed from the raw contract input file.
 */
public record FeedRecord(
        long lineNumber,
        FeedRecordType type,
        String raw,
        List<String> fields) {

    public String getField(int index) {
        return index < fields.size() ? fields.get(index) : null;
    }

    /**
     * @deprecated Use {@link #getField(int)} instead.
     */
    @Deprecated
    public String field(int index) {
        return getField(index);
    }
}
