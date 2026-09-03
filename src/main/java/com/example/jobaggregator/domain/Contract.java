package com.example.jobaggregator.domain;

import java.util.List;

public record Contract(
        long firstPhysicalLine,
        long lastPhysicalLine,
        List<BusinessLine> lines) {
}
