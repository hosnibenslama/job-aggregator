package com.example.jobaggregator.reader;

import com.example.jobaggregator.domain.Contract;
import com.example.jobaggregator.domain.LineType;
import com.example.jobaggregator.domain.ParsedLine;
import java.util.ArrayList;
import java.util.List;
import org.springframework.batch.infrastructure.item.ExecutionContext;
import org.springframework.batch.infrastructure.item.ItemReader;
import org.springframework.batch.infrastructure.item.ItemStreamException;
import org.springframework.batch.infrastructure.item.ItemStreamReader;
import org.springframework.batch.infrastructure.item.support.SingleItemPeekableItemReader;

/**
 * Spring Batch {@link ItemStreamReader} that groups individual parsed lines into
 * {@link Contract} blocks.
 *
 * <p>Grouping rule: a new block starts on every {@code CTR} line. Every following
 * line belongs to the current block until the next {@code CTR}, {@code TRL}, or EOF
 * is peeked. Header ({@code HDR}) and trailer ({@code TRL}) lines are skipped.</p>
 *
 * <p>This reader is responsible <em>only</em> for grouping. Structural validation
 * (line sequencing, mandatory types, business rules) and rejection of invalid
 * blocks are handled downstream by the processor.</p>
 *
 * <p>When a {@code TRL} line is encountered its {@code NBCTR} field is saved to
 * the {@link ExecutionContext} under {@link #KEY_EXPECTED_CONTRACT_COUNT} so that
 * the step listener can verify it against the actual read count (rule 3).</p>
 */
public class ContractBlockReader implements ItemStreamReader<Contract> {

    /** ExecutionContext key for the expected contract count from the TRL line. */
    public static final String KEY_EXPECTED_CONTRACT_COUNT = "contract.expected.count";

    private static final int SENTINEL_NOT_SET = -1;

    private final SingleItemPeekableItemReader<ParsedLine> delegate;

    /** NBCTR parsed from the TRL line; -1 if TRL has not been seen yet. */
    private int expectedContractCount = SENTINEL_NOT_SET;

    public ContractBlockReader(ItemReader<ParsedLine> lineReader) {
        this.delegate = new SingleItemPeekableItemReader<>(lineReader);
    }

    public ContractBlockReader(SingleItemPeekableItemReader<ParsedLine> peekableLineReader) {
        this.delegate = peekableLineReader;
    }

    // -----------------------------------------------------------------------
    // ItemStreamReader
    // -----------------------------------------------------------------------

    @Override
    public Contract read() throws Exception {
        ParsedLine ctrLine = skipToCtr();
        if (ctrLine == null) {
            return null;
        }

        List<ParsedLine> lines = new ArrayList<>();
        lines.add(ctrLine);
        collectUntilNextBoundary(lines);
        return new Contract(lines);
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
     * Reads lines until a {@code CTR} is found, skipping {@code HDR} lines.
     * Returns {@code null} on {@code TRL} or EOF.
     * When a {@code TRL} is found its {@code NBCTR} field is captured.
     */
    private ParsedLine skipToCtr() throws Exception {
        ParsedLine line = delegate.read();
        while (line != null && line.type() != LineType.CTR) {
            if (line.type() == LineType.TRL) {
                captureTrlCount(line);
                return null;
            }
            line = delegate.read();
        }
        return line;
    }

    /**
     * Reads the NBCTR value from a TRL line's fields (index 1).
     * Silently ignored if the field is absent or non-numeric — field-level
     * validation in {@link com.example.jobaggregator.reader.validator.TrlLineValidator}
     * already enforces the format before this point.
     */
    private void captureTrlCount(ParsedLine trl) {
        List<String> fields = trl.fields();
        if (fields.size() >= 2) {
            try {
                expectedContractCount = Integer.parseInt(fields.get(1).strip());
            } catch (NumberFormatException ignored) {
                // TrlLineValidator already rejected non-numeric values
            }
        }
    }

    /**
     * Peeks successive lines and appends them to the target list until
     * the next block boundary ({@code CTR} / {@code TRL}) or EOF.
     */
    private void collectUntilNextBoundary(List<ParsedLine> target) throws Exception {
        ParsedLine next = delegate.peek();
        while (next != null && !isBoundary(next)) {
            target.add(delegate.read());
            next = delegate.peek();
        }
    }

    private static boolean isBoundary(ParsedLine line) {
        return line.type() == LineType.CTR || line.type() == LineType.TRL;
    }
}
