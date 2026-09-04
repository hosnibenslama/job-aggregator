package com.example.jobaggregator.processor;

import com.example.jobaggregator.domain.Contract;
import com.example.jobaggregator.domain.LineType;
import com.example.jobaggregator.domain.ParsedLine;
import com.example.jobaggregator.error.ContractFormatException;
import com.example.jobaggregator.reader.ContractBlockAssembler;
import com.example.jobaggregator.writer.ContractRejectWriter;
import java.util.List;
import org.springframework.batch.infrastructure.item.ItemProcessor;
import org.springframework.stereotype.Component;

/**
 * Validates a {@link Contract} block against sequencing grammar and structural
 * business rules, and routes invalid contracts to the reject writer.
 *
 * <p>Validation is delegated to {@link ContractBlockAssembler}, which enforces:
 * <ul>
 *   <li>Line sequencing grammar (which line types may follow a given type)</li>
 *   <li>Structural prerequisites (e.g. OID requires OM, TAR requires ART)</li>
 *   <li>Mandatory block content (at least one ACC, OM, ART per contract)</li>
 * </ul>
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
        try {
            checkForUnknownLines(item);
            validateContract(item);
            return item;
        } catch (ContractFormatException e) {
            rejectWriter.reject(item, e.getMessage());
            return null;
        }
    }

    // -----------------------------------------------------------------------
    // Validation
    // -----------------------------------------------------------------------

    /**
     * Rejects contracts containing lines that failed to parse (marked as UNKNOWN).
     * This ensures that contracts with typos like "CTTR" instead of "CTR" are
     * rejected to the file rather than silently skipped.
     */
    private void checkForUnknownLines(Contract contract) {
        for (ParsedLine line : contract.lines()) {
            if (line.type() == LineType.UNKNOWN) {
                throw new ContractFormatException(line.lineNumber(), null,
                        "Unparseable line: " + line.raw());
            }
        }
    }

    /**
     * Replays the block's lines through the assembler to enforce sequencing,
     * prerequisites, and mandatory-type rules.
     */
    private void validateContract(Contract contract) {
        List<ParsedLine> lines = contract.lines();
        ContractBlockAssembler assembler = new ContractBlockAssembler(lines.getFirst());
        for (int i = 1; i < lines.size(); i++) {
            assembler.accept(lines.get(i));
        }
        assembler.build(); // validates mandatory ACC, OM, ART
    }
}
