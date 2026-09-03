package com.example.jobaggregator.domain;

import java.util.List;

public record Contract(List<BusinessLine> lines) {

    /**
     * Returns the typed representation of the CTR (contract root) line.
     * Assumes the contract was built correctly and always contains exactly one CTR line at index 0.
     */
    public CtrLine ctrLine() {
        return lines.stream()
                .filter(l -> l.type() == LineType.CTR)
                .findFirst()
                .map(CtrLine::from)
                .orElseThrow(() -> new IllegalStateException("Contract has no CTR line"));
    }
}
