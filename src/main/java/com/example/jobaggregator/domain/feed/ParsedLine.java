package com.example.jobaggregator.domain.feed;

import java.util.List;

/**
 * Represents an immutable semicolon-delimited input line parsed from the raw feed.
 */
public record ParsedLine(
        long lineNumber,
        LineType type,
        String raw,
        List<String> fields) {

    public String field(int index) {
        return index < fields.size() ? fields.get(index) : null;
    }
}
