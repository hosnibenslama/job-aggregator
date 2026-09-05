package com.example.jobaggregator.domain.feed;

import java.util.List;

/**
 * Represents an immutable semicolon-delimited feed record parsed from the raw contract input file.
 */
public record FeedRecord(
        long lineNumber,
        FeedRecordType type,
        String raw,
        List<String> fields) {

    public String field(int index) {
        return index < fields.size() ? fields.get(index) : null;
    }
}
