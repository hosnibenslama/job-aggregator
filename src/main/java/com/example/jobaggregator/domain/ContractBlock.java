package com.example.jobaggregator.domain;

import java.util.List;

/**
 * Represents an aggregated block of parsed lines constituting a single contract.
 * The block begins with a CTR line (the root) followed by all its child lines.
 */
public record ContractBlock(List<ParsedLine> lines) {

    /**
     * Returns the typed representation of the CTR (contract root) line.
     * CTR is always the first line by construction — accessing it directly
     * avoids an unnecessary linear scan.
     */
    public CtrLine ctrLine() {
        return CtrLine.from(lines.getFirst());
    }
}
