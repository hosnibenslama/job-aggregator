package com.example.jobaggregator.partition;

import com.example.jobaggregator.domain.LineType;
import com.example.jobaggregator.error.ContractFormatException;
import com.example.jobaggregator.reader.SemicolonLineParser;
import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.IOException;
import java.nio.charset.Charset;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardOpenOption;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import org.springframework.batch.core.step.StepContribution;
import org.springframework.batch.core.scope.context.ChunkContext;
import org.springframework.batch.core.step.tasklet.Tasklet;
import org.springframework.batch.infrastructure.repeat.RepeatStatus;

public class ContractPartitionSplitterTasklet implements Tasklet {

    public static final String PARTITION_FILES_KEY = "contract.partition.files";

    private final Path inputFile;
    private final Path outputDirectory;
    private final int requestedPartitions;
    private final Charset charset;
    private final SemicolonLineParser mapper = new SemicolonLineParser();

    public ContractPartitionSplitterTasklet(
            Path inputFile,
            Path outputDirectory,
            int requestedPartitions,
            Charset charset) {
        this.inputFile = inputFile;
        this.outputDirectory = outputDirectory;
        this.requestedPartitions = requestedPartitions;
        this.charset = charset;
    }

    @Override
    public RepeatStatus execute(StepContribution contribution, ChunkContext chunkContext) throws Exception {
        recreateDirectory(outputDirectory);

        long targetPartitionBytes = Math.max(1L, Files.size(inputFile) / requestedPartitions);
        long bytesInCurrentPartition = 0L;
        long sourceLineNumber = 0L;
        int partitionNumber = 0;
        boolean contractsStarted = false;
        String header = null;
        String trailer = null;
        PartitionFile current = null;
        List<Path> partitionFiles = new ArrayList<>();

        try (BufferedReader source = Files.newBufferedReader(inputFile, charset)) {
            String line;
            while ((line = source.readLine()) != null) {
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
                    if (current == null || (bytesInCurrentPartition >= targetPartitionBytes
                            && partitionNumber < requestedPartitions)) {
                        if (current != null) {
                            current.close();
                        }
                        current = openPartition(++partitionNumber, header, partitionFiles);
                        bytesInCurrentPartition = 0L;
                    }
                }

                if (current == null) {
                    throw new ContractFormatException(sourceLineNumber, null,
                            "Expected CTR before " + type);
                }

                current.write(line);
                bytesInCurrentPartition += encodedLineSize(line);
            }
        } finally {
            if (current != null) {
                current.close();
            }
        }

        if (header == null) {
            throw new ContractFormatException(0, null, "Missing HDR");
        }
        if (trailer == null) {
            throw new ContractFormatException(0, null, "Missing TRL");
        }
        if (partitionFiles.isEmpty()) {
            throw new ContractFormatException(0, null, "No CTR blocks found");
        }

        for (Path partitionFile : partitionFiles) {
            Files.writeString(partitionFile, trailer + System.lineSeparator(), charset,
                    StandardOpenOption.APPEND);
        }

        chunkContext.getStepContext().getStepExecution().getJobExecution().getExecutionContext()
                .putString(PARTITION_FILES_KEY,
                        partitionFiles.stream().map(path -> path.toAbsolutePath().toString())
                                .reduce((left, right) -> left + "|" + right)
                                .orElseThrow());

        return RepeatStatus.FINISHED;
    }

    private PartitionFile openPartition(int number, String header, List<Path> partitionFiles)
            throws IOException {
        if (header == null) {
            throw new ContractFormatException(0, null, "HDR is required before CTR");
        }

        Path file = outputDirectory.resolve("contracts-part-%05d.dat".formatted(number));
        BufferedWriter writer = Files.newBufferedWriter(file, charset,
                StandardOpenOption.CREATE_NEW, StandardOpenOption.WRITE);
        writer.write(header);
        writer.newLine();
        partitionFiles.add(file);
        return new PartitionFile(writer);
    }

    private long encodedLineSize(String line) {
        return line.getBytes(charset).length + System.lineSeparator().getBytes(charset).length;
    }

    private void recreateDirectory(Path directory) throws IOException {
        if (Files.exists(directory)) {
            try (var paths = Files.walk(directory)) {
                paths.sorted(Comparator.reverseOrder())
                        .filter(path -> !path.equals(directory))
                        .forEach(path -> {
                            try {
                                Files.delete(path);
                            } catch (IOException exception) {
                                throw new IllegalStateException("Cannot delete " + path, exception);
                            }
                        });
            }
        }
        Files.createDirectories(directory);
    }

    private static final class PartitionFile {
        private final BufferedWriter writer;

        private PartitionFile(BufferedWriter writer) {
            this.writer = writer;
        }

        private void write(String line) throws IOException {
            writer.write(line);
            writer.newLine();
        }

        private void close() throws IOException {
            writer.close();
        }
    }
}
