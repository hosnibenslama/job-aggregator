package com.bnpparibas.dsibddf.ap23992.modeldevente.batch.injector.processor;

import com.bnpparibas.dsibddf.ap23992.modeldevente.batch.injector.domain.ContractBlock;
import com.bnpparibas.dsibddf.ap23992.modeldevente.batch.injector.domain.feed.FeedRecordType;
import com.bnpparibas.dsibddf.ap23992.modeldevente.batch.injector.domain.feed.FeedRecord;
import com.bnpparibas.dsibddf.ap23992.modeldevente.batch.injector.error.ContractFormatException;
import com.bnpparibas.dsibddf.ap23992.modeldevente.batch.injector.reader.ContractBlockAssembler;
import com.bnpparibas.dsibddf.ap23992.modeldevente.batch.injector.writer.ContractRejectWriter;
import java.util.List;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
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
 * <p>The assembler's {@code build()} method produces a fully-validated
 * {@link ContractBlock}, which is returned as the processor output. This avoids
 * double-parsing: the reader produces a leniently-assembled block, and this
 * processor produces the strictly-validated one.</p>
 *
 * <p>Returning {@code null} causes Spring Batch to silently skip the item for writing.
 */
@Component
public final class ContractStructureValidator implements ItemProcessor<ContractBlock, ContractBlock> {

    private static final Logger log = LoggerFactory.getLogger(ContractStructureValidator.class);

    private final ContractRejectWriter rejectWriter;

    public ContractStructureValidator(ContractRejectWriter rejectWriter) {
        this.rejectWriter = rejectWriter;
    }

    @Override
    public ContractBlock process(ContractBlock item) throws Exception {
        try {
            checkForUnknownRecords(item);
            return validateAndAssemble(item);
        } catch (ContractFormatException e) {
            log.warn("Contract {} rejected: {}", item.id(), e.getReason());
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
    private void checkForUnknownRecords(ContractBlock contract) {
        for (FeedRecord record : contract.records()) {
            if (record.type() == FeedRecordType.UNKNOWN) {
                throw new ContractFormatException(record.lineNumber(), null,
                        "Unparseable line: " + record.raw());
            }
        }
    }

    /**
     * Replays the block's records through the assembler to enforce sequencing,
     * prerequisites, and mandatory-type rules. Returns the validated block produced
     * by {@link ContractBlockAssembler#build()}.
     */
    private ContractBlock validateAndAssemble(ContractBlock contract) {
        List<FeedRecord> records = contract.records();
        ContractBlockAssembler assembler = new ContractBlockAssembler(contract.id(), records.get(0));
        for (int i = 1; i < records.size(); i++) {
            assembler.accept(records.get(i));
        }
        return assembler.build();
    }
}
