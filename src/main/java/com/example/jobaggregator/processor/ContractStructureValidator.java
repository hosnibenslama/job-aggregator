package com.example.jobaggregator.processor;

import com.example.jobaggregator.domain.Contract;
import com.example.jobaggregator.domain.LineType;
import com.example.jobaggregator.writer.ContractRejectWriter;
import java.util.ArrayList;
import java.util.List;
import org.springframework.batch.infrastructure.item.ItemProcessor;
import org.springframework.stereotype.Component;

/**
 * Validates a {@link Contract} against structural business rules and routes invalid contracts
 * to the reject writer instead of propagating them downstream.
 *
 * <p>A contract is considered valid when it contains at least one of each mandatory
 * line type: {@code ACC}, {@code OM}, and {@code ART}.
 *
 * <p>Returning {@code null} causes Spring Batch to silently skip the item for writing.
 */
@Component
public final class ContractStructureValidator implements ItemProcessor<Contract, Contract> {

    private final ContractRejectWriter rejectWriter;

    public ContractStructureValidator(ContractRejectWriter rejectWriter) {
        this.rejectWriter = rejectWriter;
    }

    @Override
    public Contract process(Contract item) throws Exception {
        List<String> violations = collectViolations(item);
        if (!violations.isEmpty()) {
            String reason = "Missing required line type(s): " + String.join(", ", violations);
            rejectWriter.reject(item, reason);
            return null; // filtered out — not forwarded to the writer
        }
        return item;
    }

    // -----------------------------------------------------------------------
    // Validation
    // -----------------------------------------------------------------------

    private List<String> collectViolations(Contract contract) {
        List<String> missing = new ArrayList<>();

        boolean hasAcc = contract.lines().stream().anyMatch(l -> l.type() == LineType.ACC);
        boolean hasOm  = contract.lines().stream().anyMatch(l -> l.type() == LineType.OM);
        boolean hasArt = contract.lines().stream().anyMatch(l -> l.type() == LineType.ART);

        if (!hasAcc) missing.add("ACC");
        if (!hasOm)  missing.add("OM");
        if (!hasArt) missing.add("ART");

        return missing;
    }
}
