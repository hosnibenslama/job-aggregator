package com.example.jobaggregator.writer;

import com.example.jobaggregator.domain.BusinessLine;
import com.example.jobaggregator.domain.Contract;
import java.io.BufferedWriter;
import java.io.IOException;
import java.nio.charset.Charset;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardOpenOption;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import jakarta.annotation.PostConstruct;
import jakarta.annotation.PreDestroy;

@Component
public final class InvalidContractFileWriter {

    private final Path invalidFile;
    private final Charset charset;
    private BufferedWriter writer;

    public InvalidContractFileWriter(
            @Value("${contract.import.invalid-file:/data/output/invalid-contracts.dat}")
            String invalidFile,
            @Value("${contract.import.charset:UTF-8}")
            String charsetName) {
        this.invalidFile = Path.of(invalidFile);
        this.charset = Charset.forName(charsetName);
    }

    @PostConstruct
    public void open() throws IOException {
        Files.createDirectories(invalidFile.getParent());
        this.writer = Files.newBufferedWriter(invalidFile, charset,
                StandardOpenOption.CREATE, StandardOpenOption.APPEND);
    }

    @PreDestroy
    public void close() throws IOException {
        if (writer != null) {
            writer.close();
        }
    }

    public synchronized void write(Contract contract) throws IOException {
        for (BusinessLine line : contract.lines()) {
            writer.write(line.raw());
            writer.newLine();
        }
    }
}
