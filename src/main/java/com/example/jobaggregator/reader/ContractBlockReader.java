package com.example.jobaggregator.reader;

import com.example.jobaggregator.domain.ContractBlock;
import com.example.jobaggregator.domain.feed.FeedRecordType;
import com.example.jobaggregator.domain.feed.FeedRecord;
import java.util.ArrayList;
import java.util.List;
import org.springframework.batch.infrastructure.item.ExecutionContext;
import org.springframework.batch.infrastructure.item.ItemReader;
import org.springframework.batch.infrastructure.item.ItemStreamException;
import org.springframework.batch.infrastructure.item.ItemStreamReader;
import org.springframework.batch.infrastructure.item.support.SingleItemPeekableItemReader;

/**
 * Spring Batch {@link ItemStreamReader} that groups individual parsed feed records into
 * {@link ContractBlock} blocks.
 *
 * <p>Grouping rule: a new block starts on every {@code CTR} record. Every following
 * record belongs to the current block until the next {@code CTR}, {@code TRL}, or EOF
 * is peeked. Header ({@code HDR}) and trailer ({@code TRL}) records are skipped.</p>
 *
 * <p>This reader is responsible <em>only</em> for grouping. Structural validation
 * (record sequencing, mandatory types, business rules) and rejection of invalid
 * blocks are handled downstream by the processor.</p>
 *
 * <p>When a {@code TRL} record is encountered its {@code NBCTR} field is saved to
 * the {@link ExecutionContext} under {@link #KEY_EXPECTED_CONTRACT_COUNT} so that
 * the step listener can verify it against the actual read count (rule 3).</p>
 */
public class ContractBlockReader implements ItemStreamReader<ContractBlock> {

    /** ExecutionContext key for the expected contract count from the TRL line. */
    public static final String KEY_EXPECTED_CONTRACT_COUNT = "contract.expected.count";

    private static final int SENTINEL_NOT_SET = -1;

    private final SingleItemPeekableItemReader<FeedRecord> delegate;

    /** NBCTR parsed from the TRL record; -1 if TRL has not been seen yet. */
    private int expectedContractCount = SENTINEL_NOT_SET;

    public ContractBlockReader(ItemReader<FeedRecord> recordReader) {
        this.delegate = new SingleItemPeekableItemReader<>(recordReader);
    }

    public ContractBlockReader(SingleItemPeekableItemReader<FeedRecord> peekableRecordReader) {
        this.delegate = peekableRecordReader;
    }

    // -----------------------------------------------------------------------
    // ItemStreamReader
    // -----------------------------------------------------------------------

    @Override
    public ContractBlock read() throws Exception {
        FeedRecord ctrRecord = skipToCtr();
        if (ctrRecord == null) {
            return null;
        }

        List<FeedRecord> records = new ArrayList<>();
        records.add(ctrRecord);
        collectUntilNextBoundary(records);
        return new ContractBlock(records);
    }

    @Override
    public void open(ExecutionContext executionContext) throws ItemStreamException {
        delegate.open(executionContext);
        if (executionContext.containsKey(KEY_EXPECTED_CONTRACT_COUNT)) {
            expectedContractCount = executionContext.getInt(KEY_EXPECTED_CONTRACT_COUNT);
        }
    }

    @Override
    public void update(ExecutionContext executionContext) throws ItemStreamException {
        delegate.update(executionContext);
        if (expectedContractCount != SENTINEL_NOT_SET) {
            executionContext.putInt(KEY_EXPECTED_CONTRACT_COUNT, expectedContractCount);
        }
    }

    @Override
    public void close() throws ItemStreamException {
        delegate.close();
    }

    // -----------------------------------------------------------------------
    // Helpers
    // -----------------------------------------------------------------------

    /**
     * Reads records until a {@code CTR} is found, skipping {@code HDR} records.
     * Returns {@code null} on {@code TRL} or EOF.
     * When a {@code TRL} is found its {@code NBCTR} field is captured.
     */
    private FeedRecord skipToCtr() throws Exception {
        FeedRecord record = delegate.read();
        while (record != null && record.type() != FeedRecordType.CTR) {
            if (record.type() == FeedRecordType.TRL) {
                captureTrlCount(record);
                return null;
            }
            record = delegate.read();
        }
        return record;
    }

    /**
     * Reads the NBCTR value from a TRL record's fields (index 1).
     * Silently ignored if the field is absent or non-numeric — field-level
     * validation in {@link com.example.jobaggregator.reader.validator.TrailerValidator}
     * already enforces the format before this point.
     */
    private void captureTrlCount(FeedRecord trl) {
        List<String> fields = trl.fields();
        if (fields.size() >= 2) {
            try {
                expectedContractCount = Integer.parseInt(fields.get(1).strip());
            } catch (NumberFormatException ignored) {
                // TrailerValidator already rejected non-numeric values
            }
        }
    }

    /**
     * Peeks successive records and appends them to the target list until
     * the next block boundary ({@code CTR} / {@code TRL}) or EOF.
     */
    private void collectUntilNextBoundary(List<FeedRecord> target) throws Exception {
        FeedRecord next = delegate.peek();
        while (next != null && !isBoundary(next)) {
            target.add(delegate.read());
            next = delegate.peek();
        }
    }

    private static boolean isBoundary(FeedRecord record) {
        return record.type() == FeedRecordType.CTR || record.type() == FeedRecordType.TRL;
    }
}
