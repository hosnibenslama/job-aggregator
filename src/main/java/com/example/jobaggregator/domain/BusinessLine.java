package com.example.jobaggregator.domain;

import java.util.List;

public record BusinessLine(
        long lineNumber,
        LineType type,
        String raw,
        List<String> fields) {

    public String field(int index) {
        return index < fields.size() ? fields.get(index) : null;
    }
}
