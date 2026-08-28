package com.example.jobaggregator.reader;

import com.example.jobaggregator.domain.BusinessLine;
import com.example.jobaggregator.domain.Contract;
import com.example.jobaggregator.domain.LineType;
import com.example.jobaggregator.error.ContractFormatException;
import java.io.BufferedReader;
import java.io.IOException;
import java.nio.charset.Charset;
import java.nio.file.Files;
import java.nio.file.Path;
import org.springframework.batch.infrastructure.item.ExecutionContext;
import org.springframework.batch.infrastructure.item.ItemStreamException;
import org.springframework.batch.infrastructure.item.ItemStreamReader;

public final class ContractFileReader implements ItemStreamReader<Contract> {

    private static final String READ_LINE_COUNT_KEY = "contract.file.reader.read.line.count";

    private final Path partitionFile;
    private final Charset charset;
    private final BusinessLineMapper mapper;

    private BufferedReader reader;
    private BusinessLine buffered;
    private long nextLineNumber;

    public ContractFileReader(Path partitionFile, Charset charset, BusinessLineMapper mapper) {
        this.partitionFile = partitionFile;
        this.charset = charset;
        this.mapper = mapper;
    }

    @Override
    public void open(ExecutionContext executionContext) throws ItemStreamException {
        try {
            long alreadyRead = executionContext.getLong(READ_LINE_COUNT_KEY, 0L);
            reader = Files.newBufferedReader(partitionFile, charset);
            buffered = null;
            nextLineNumber = 1L;

            for (long index = 0; index < alreadyRead; index++) {
                if (reader.readLine() == null) {
                    break;
                }
                nextLineNumber++;
            }
        } catch (IOException exception) {
            throw new ItemStreamException("Unable to open partition file " + partitionFile, exception);
        }
    }

    @Override
    public Contract read() throws Exception {
        BusinessLine first = nextNonEnvelopeLine();
        if (first == null) {
            return null;
        }
        if (first.type() != LineType.CTR) {
            throw new ContractFormatException(first.lineNumber(), null,
                    "Expected CTR at a contract boundary but found " + first.type());
        }

        ContractBuilder builder = new ContractBuilder(first);
        while (true) {
            BusinessLine next = nextLine();
            if (next == null || next.type() == LineType.TRL) {
                return builder.build();
            }
            if (next.type() == LineType.CTR) {
                buffered = next;
                return builder.build();
            }
            if (next.type() == LineType.HDR) {
                throw new ContractFormatException(next.lineNumber(), null,
                        "HDR is not allowed inside a contract");
            }
            builder.accept(next);
        }
    }

    @Override
    public void update(ExecutionContext executionContext) {
        long committedReadCount = buffered == null
                ? nextLineNumber - 1
                : buffered.lineNumber() - 1;
        executionContext.putLong(READ_LINE_COUNT_KEY, committedReadCount);
    }

    @Override
    public void close() throws ItemStreamException {
        if (reader == null) {
            return;
        }
        try {
            reader.close();
        } catch (IOException exception) {
            throw new ItemStreamException("Unable to close partition file " + partitionFile, exception);
        }
    }

    private BusinessLine nextNonEnvelopeLine() throws IOException {
        BusinessLine candidate;
        while ((candidate = nextLine()) != null) {
            if (candidate.type() != LineType.HDR && candidate.type() != LineType.TRL) {
                return candidate;
            }
        }
        return null;
    }

    private BusinessLine nextLine() throws IOException {
        if (buffered != null) {
            BusinessLine result = buffered;
            buffered = null;
            return result;
        }

        String raw = reader.readLine();
        if (raw == null) {
            return null;
        }

        long lineNumber = nextLineNumber++;
        return mapper.mapLine(raw, Math.toIntExact(lineNumber));
    }
}
