package com.example.jobaggregator.writer;

import com.example.jobaggregator.domain.BusinessLine;
import com.example.jobaggregator.domain.Contract;
import jakarta.annotation.PostConstruct;
import jakarta.annotation.PreDestroy;
import java.io.BufferedWriter;
import java.io.IOException;
import java.nio.charset.Charset;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardOpenOption;
import java.util.List;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

/**
 * Writes rejected contract blocks to a flat file, one block per rejection.
 *
 * <p>Each rejected block is prefixed with a comment line that contains the rejection reason:
 * <pre>
 * # REJECTED: &lt;reason&gt;
 * CTR;EUR;16;...
 *   ACC;BILL;...
 *
 * </pre>
 *
 * <p>Rejections come from two sources:
 * <ul>
 *   <li><b>Read phase</b>: a line fails spec validation (malformed field), caught in
 *       {@link com.example.jobaggregator.reader.ContractFileReader}</li>
 *   <li><b>Process phase</b>: a structurally valid block fails business rules (e.g. missing
 *       ACC / OM / ART line), caught in
 *       {@link com.example.jobaggregator.processor.ContractProcessor}</li>
 * </ul>
 */
@Component
public final class InvalidContractFileWriter {

    private final Path rejectFile;
    private final Charset charset;
    private BufferedWriter writer;

    public InvalidContractFileWriter(
            @Value("${contract.import.invalid-file:/data/output/invalid-contracts.dat}")
            String rejectFile,
            @Value("${contract.import.charset:UTF-8}")
            String charsetName) {
        this.rejectFile = Path.of(rejectFile);
        this.charset = Charset.forName(charsetName);
    }

    @PostConstruct
    public void open() throws IOException {
        Files.createDirectories(rejectFile.getParent());
        this.writer = Files.newBufferedWriter(rejectFile, charset,
                StandardOpenOption.CREATE, StandardOpenOption.APPEND);
    }

    @PreDestroy
    public void close() throws IOException {
        if (writer != null) {
            writer.close();
        }
    }

    // -----------------------------------------------------------------------
    // Public API
    // -----------------------------------------------------------------------

    /**
     * Rejects a fully-built {@link Contract} with the given reason.
     * Used by the processor for business-rule violations.
     */
    public synchronized void reject(Contract contract, String reason) throws IOException {
        List<String> rawLines = contract.lines().stream()
                .map(BusinessLine::raw)
                .toList();
        reject(rawLines, reason);
    }

    /**
     * Rejects a list of raw text lines (partial or complete contract block) with the given reason.
     * Used by the reader when a {@code ContractFormatException} is thrown during mapping.
     */
    public synchronized void reject(List<String> rawLines, String reason) throws IOException {
        writer.write("# REJECTED: " + reason);
        writer.newLine();
        for (String raw : rawLines) {
            writer.write(raw);
            writer.newLine();
        }
        writer.newLine(); // blank line separator between blocks
        writer.flush();
    }
}
