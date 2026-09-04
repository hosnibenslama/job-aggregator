package com.example.jobaggregator.writer;

import com.example.jobaggregator.domain.Contract;
import java.io.IOException;
import java.util.List;

/**
 * Interface defining contract rejection operations (Dependency Inversion Principle).
 */
public interface ContractRejectWriter {

    /**
     * Rejects a list of raw text lines (partial or complete block) with the given reason.
     */
    void reject(List<String> rawLines, String reason) throws IOException;

    /**
     * Rejects a fully-built {@link Contract} with the given reason.
     */
    void reject(Contract contract, String reason) throws IOException;
}
