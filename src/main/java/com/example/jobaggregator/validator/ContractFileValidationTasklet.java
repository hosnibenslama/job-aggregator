package com.example.jobaggregator.validator;

import com.example.jobaggregator.domain.LineType;
import com.example.jobaggregator.error.ContractFormatException;
import com.example.jobaggregator.reader.BusinessLineMapper;
import java.io.BufferedReader;
import java.nio.charset.Charset;
import java.nio.file.Files;
import java.nio.file.Path;
import org.springframework.batch.core.scope.context.ChunkContext;
import org.springframework.batch.core.step.StepContribution;
import org.springframework.batch.core.step.tasklet.Tasklet;
import org.springframework.batch.infrastructure.repeat.RepeatStatus;

public class ContractFileValidationTasklet implements Tasklet {

    private final Path inputFile;
    private final Charset charset;
    private final BusinessLineMapper mapper = new BusinessLineMapper();

    public ContractFileValidationTasklet(Path inputFile, Charset charset) {
        this.inputFile = inputFile;
        this.charset = charset;
    }

    @Override
    public RepeatStatus execute(StepContribution contribution, ChunkContext chunkContext) throws Exception {
        if (!Files.exists(inputFile)) {
            throw new ContractFormatException(0, null, "Input file does not exist: " + inputFile);
        }

        long sourceLineNumber = 0L;
        boolean contractsStarted = false;
        long ctrCount = 0L;
        String header = null;
        String trailer = null;

        try (BufferedReader reader = Files.newBufferedReader(inputFile, charset)) {
            String line;
            while ((line = reader.readLine()) != null) {
                sourceLineNumber++;
                LineType type = mapper.mapLine(line, Math.toIntExact(sourceLineNumber)).type();

                if (type == LineType.HDR) {
                    if (header != null || contractsStarted) {
                        throw new ContractFormatException(sourceLineNumber, null,
                                "HDR must appear exactly once before the first CTR");
                    }
                    header = line;
                    continue;
                }

                if (type == LineType.TRL) {
                    if (trailer != null) {
                        throw new ContractFormatException(sourceLineNumber, null, "TRL must appear once");
                    }
                    trailer = line;
                    break;
                }

                if (type == LineType.CTR) {
                    contractsStarted = true;
                    ctrCount++;
                }

                if (!contractsStarted) {
                    throw new ContractFormatException(sourceLineNumber, null,
                            "Expected CTR before " + type);
                }
            }

            if (trailer != null) {
                String trailingLine;
                while ((trailingLine = reader.readLine()) != null) {
                    sourceLineNumber++;
                    if (!trailingLine.isBlank()) {
                        throw new ContractFormatException(sourceLineNumber, null,
                                "No content allowed after TRL");
                    }
                }
            }
        }

        if (header == null) {
            throw new ContractFormatException(0, null, "Missing HDR");
        }
        if (trailer == null) {
            throw new ContractFormatException(0, null, "Missing TRL");
        }
        if (ctrCount == 0) {
            throw new ContractFormatException(0, null, "No CTR blocks found");
        }

        return RepeatStatus.FINISHED;
    }
}
