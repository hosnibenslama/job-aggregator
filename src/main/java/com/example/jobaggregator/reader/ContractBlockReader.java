package com.example.jobaggregator.reader;

import com.example.jobaggregator.domain.Contract;
import com.example.jobaggregator.domain.LineType;
import com.example.jobaggregator.domain.ParsedLine;
import com.example.jobaggregator.error.ContractFormatException;
import com.example.jobaggregator.writer.ContractRejectWriter;
import java.util.ArrayList;
import java.util.List;
import org.springframework.batch.infrastructure.item.ExecutionContext;
import org.springframework.batch.infrastructure.item.ItemStreamException;
import org.springframework.batch.infrastructure.item.ItemStreamReader;
import org.springframework.batch.infrastructure.item.support.SingleItemPeekableItemReader;

/**
 * Spring Batch {@link ItemStreamReader} that aggregates individual parsed lines into
 * {@link Contract} blocks.
 *
 * <h3>Error handling strategy</h3>
 * <p>All exceptions (spec validation failures from the parser, or structural errors from
 * this assembler) are caught internally. The job <em>never stops</em> due to a bad contract:
 * <ol>
 *   <li>The raw lines collected for the failing block are written to the reject writer,
 *       together with the exception message.</li>
 *   <li>The reader drains all remaining lines of that block up to the next
 *       {@code CTR} / {@code TRL} boundary (tolerating further malformed lines).</li>
 *   <li>Processing resumes with the next contract — Spring Batch sees a clean
 *       stream of valid {@link Contract} items.</li>
 * </ol>
 */
public class ContractBlockReader implements ItemStreamReader<Contract> {

    private final SingleItemPeekableItemReader<ParsedLine> lineReader;
    private final ContractRejectWriter rejectWriter;

    public ContractBlockReader(
            SingleItemPeekableItemReader<ParsedLine> lineReader,
            ContractRejectWriter rejectWriter) {
        this.lineReader = lineReader;
        this.rejectWriter = rejectWriter;
    }

    // -----------------------------------------------------------------------
    // ItemStreamReader
    // -----------------------------------------------------------------------

    @Override
    public Contract read() throws Exception {
        while (true) {
            ParsedLine ctrLine = findNextCtrOrEnd();
            if (ctrLine == null) {
                return null; // TRL reached or EOF
            }

            List<String> rawLines = new ArrayList<>();
            rawLines.add(ctrLine.raw());

            try {
                ContractBlockAssembler assembler = new ContractBlockAssembler(ctrLine);
                aggregateRestOfBlock(assembler, rawLines);
                return assembler.build();
            } catch (Exception e) {
                String reason = e.getMessage() != null ? e.getMessage() : e.getClass().getSimpleName();
                rejectWriter.reject(rawLines, reason);
                drainToNextBoundary();
                // loop: attempt to read the next contract
            }
        }
    }

    @Override
    public void open(ExecutionContext executionContext) throws ItemStreamException {
        lineReader.open(executionContext);
    }

    @Override
    public void update(ExecutionContext executionContext) throws ItemStreamException {
        lineReader.update(executionContext);
    }

    @Override
    public void close() throws ItemStreamException {
        lineReader.close();
    }

    // -----------------------------------------------------------------------
    // Helpers
    // -----------------------------------------------------------------------

    /**
     * Scans forward until a {@code CTR} line (start of a contract block), a {@code TRL}
     * line (end of file marker), or physical EOF ({@code null} from the reader).
     */
    private ParsedLine findNextCtrOrEnd() throws Exception {
        while (true) {
            ParsedLine line;
            try {
                line = lineReader.read();
            } catch (ContractFormatException e) {
                // Malformed line at the top level — write as single-line reject and keep scanning
                rejectWriter.reject(List.of("# [malformed line]"), e.getMessage());
                continue;
            }

            if (line == null) return null;

            switch (line.type()) {
                case HDR  -> { /* silently skip header */ }
                case TRL  -> { return null; }
                case CTR  -> { return line; }
                default   -> {
                    // Unexpected line type at the contract boundary
                    rejectWriter.reject(List.of(line.raw()), "Unexpected line type at contract boundary: " + line.type());
                }
            }
        }
    }

    /**
     * Peeks at successive lines and appends them to the assembler and the raw-line list
     * until the next contract boundary ({@code CTR} / {@code TRL}) or EOF.
     */
    private void aggregateRestOfBlock(ContractBlockAssembler assembler, List<String> rawLines)
            throws Exception {
        while (true) {
            ParsedLine peeked = lineReader.peek();
            if (peeked == null) break;

            LineType nextType = peeked.type();
            if (nextType == LineType.CTR || nextType == LineType.TRL) break;
            if (nextType == LineType.HDR) {
                throw new ContractFormatException(peeked.lineNumber(), null,
                        "HDR is not allowed inside a contract block");
            }

            ParsedLine line = lineReader.read();
            rawLines.add(line.raw());
            assembler.accept(line);
        }
    }

    /**
     * Consumes lines until the reader is positioned at the next contract boundary.
     */
    private void drainToNextBoundary() {
        while (true) {
            try {
                ParsedLine peeked = lineReader.peek();
                if (peeked == null) return;
                LineType t = peeked.type();
                if (t == LineType.CTR || t == LineType.TRL) return;
                lineReader.read(); // consume the non-boundary line
            } catch (Exception e) {
                // Mapping failure: underlying reader already advanced; try next line
            }
        }
    }
}
