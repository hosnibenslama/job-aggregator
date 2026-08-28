package com.example.jobaggregator.domain;

import java.util.List;

public record Contract(
        String contractId,
        long firstPhysicalLine,
        long lastPhysicalLine,
        List<BusinessLine> lines) {
}
