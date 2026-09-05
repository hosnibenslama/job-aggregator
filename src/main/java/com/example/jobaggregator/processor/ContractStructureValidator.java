package com.example.jobaggregator.processor;

import com.example.jobaggregator.domain.ContractBlock;
import com.example.jobaggregator.domain.feed.FeedRecordType;
import com.example.jobaggregator.domain.feed.FeedRecord;
import com.example.jobaggregator.error.ContractFormatException;
import com.example.jobaggregator.reader.ContractBlockAssembler;
import com.example.jobaggregator.writer.ContractRejectWriter;
import java.util.List;
import org.springframework.batch.infrastructure.item.ItemProcessor;
import org.springframework.stereotype.Component;

/**
 * Validates a {@link ContractBlock} against sequencing grammar and structural
 * business rules, and routes invalid contracts to the reject writer.
 *
 * <p>Validation is delegated to {@link ContractBlockAssembler}, which enforces:
 * <ul>
 *   <li>Record sequencing grammar (which record types may follow a given type)</li>
 *   <li>Structural prerequisites (e.g. OID requires OM, TAR requires ART)</li>
 *   <li>Mandatory block content (at least one ACC, OM, ART per contract)</li>
 * </ul>
 *
 * <p>Returning {@code null} causes Spring Batch to silently skip the item for writing.
 */
@Component
public final class ContractStructureValidator implements ItemProcessor<ContractBlock, ContractBlock> {

    private final ContractRejectWriter rejectWriter;

    public ContractStructureValidator(ContractRejectWriter rejectWriter) {
        this.rejectWriter = rejectWriter;
    }

    @Override
    public ContractBlock process(ContractBlock item) throws Exception {
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
     * Rejects contracts containing records that failed to parse (marked as UNKNOWN).
     * This ensures that contracts with typos like "CTTR" instead of "CTR" are
     * rejected to the file rather than silently skipped.
     */
    private void checkForUnknownLines(ContractBlock contract) {
        for (FeedRecord record : contract.records()) {
            if (record.type() == FeedRecordType.UNKNOWN) {
                throw new ContractFormatException(record.lineNumber(), null,
                        "Unparseable line: " + record.raw());
            }
        }
    }

    /**
     * Replays the block's records through the assembler to enforce sequencing,
     * prerequisites, and mandatory-type rules.
     */
    private void validateContract(ContractBlock contract) {
        List<FeedRecord> records = contract.records();
        ContractBlockAssembler assembler = new ContractBlockAssembler(contract.id(), records.getFirst());
        for (int i = 1; i < records.size(); i++) {
            assembler.accept(records.get(i));
        }
        assembler.build(); // validates mandatory ACC, OM, ART
    }
}
