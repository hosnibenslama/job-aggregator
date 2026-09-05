package com.example.jobaggregator.writer;

import com.example.jobaggregator.domain.ContractBlock;
import com.example.jobaggregator.domain.ParsedLine;
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
 * # ERROR: Mandatory ACC line missing
 * CTR;EUR;16;...
 * </pre>
 *
 * <p>Thread-safety: the underlying {@link BufferedWriter} is synchronized on this instance.
 */
@Component
public class RejectedContractFileWriter implements ContractRejectWriter {

    private final Path rejectFile;
    private final Charset charset;
    private BufferedWriter writer;

    public RejectedContractFileWriter(
            @Value("${contract.import.invalid-file:src/main/resources/invalid-contracts.txt}")
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
    // ContractRejectWriter implementation
    // -----------------------------------------------------------------------

    @Override
    public synchronized void reject(ContractBlock contract, String reason) throws IOException {
        List<String> rawLines = contract.lines().stream()
                .map(ParsedLine::raw)
                .toList();
        reject(rawLines, reason);
    }

    @Override
    public synchronized void reject(List<String> rawLines, String reason) throws IOException {
        writer.write("# ERROR: " + reason);
        writer.newLine();
        for (String raw : rawLines) {
            writer.write(raw);
            writer.newLine();
        }
        writer.newLine(); // blank line separator between blocks
        writer.flush();
    }
}
