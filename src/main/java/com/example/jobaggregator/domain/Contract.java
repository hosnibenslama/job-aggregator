package com.example.jobaggregator.domain;

import java.util.List;

public record Contract(List<ParsedLine> lines) {

    /**
     * Returns the typed representation of the CTR (contract root) line.
     * CTR is always the first line by construction — accessing it directly
     * avoids an unnecessary linear scan.
     */
    public CtrLine ctrLine() {
        return CtrLine.from(lines.getFirst());
    }
}
