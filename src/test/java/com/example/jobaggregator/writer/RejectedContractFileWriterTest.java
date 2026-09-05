package com.example.jobaggregator.writer;

import static org.assertj.core.api.Assertions.assertThat;

import com.example.jobaggregator.domain.ContractBlock;
import com.example.jobaggregator.domain.feed.FeedRecordType;
import com.example.jobaggregator.domain.feed.FeedRecord;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

class RejectedContractFileWriterTest {

    @TempDir
    Path tempDir;

    private Path rejectFile;
    private RejectedContractFileWriter writer;

    @BeforeEach
    void setUp() throws IOException {
        rejectFile = tempDir.resolve("invalid-contracts.txt");
        writer = new RejectedContractFileWriter(rejectFile.toString(), "UTF-8");
        writer.open();
    }

    @AfterEach
    void tearDown() throws IOException {
        writer.close();
    }

    @Test
    void shouldWriteErrorCommentAndRawLinesWhenRejectingList() throws IOException {
        // Given: A list of raw contract lines and an error reason
        List<String> rawLines = List.of("CTR;EUR;16;ABC", "ACC;001;XYZ");
        String reason = "Missing mandatory field";

        // Act: Reject the contract lines
        writer.reject(rawLines, reason);

        // Assert: The file contains the # ERROR: prefix and the raw lines
        String content = Files.readString(rejectFile);
        assertThat(content).contains("# ERROR: Missing mandatory field");
        assertThat(content).contains("CTR;EUR;16;ABC");
        assertThat(content).contains("ACC;001;XYZ");
    }

    @Test
    void shouldWriteErrorCommentWhenRejectingContractBlock() throws IOException {
        // Given: A ContractBlock with parsed lines and an error reason
        FeedRecord ctrLine = new FeedRecord(1, FeedRecordType.CTR, "CTR;EUR;16;ABC", List.of("CTR", "EUR", "16", "ABC"));
        FeedRecord ikacLine = new FeedRecord(2, FeedRecordType.IKAC, "IKAC;001;DATA", List.of("IKAC", "001", "DATA"));
        ContractBlock contractBlock = new ContractBlock(List.of(ctrLine, ikacLine));
        String reason = "Invalid contract structure";

        // Act: Reject the ContractBlock
        writer.reject(contractBlock, reason);

        // Assert: The file contains the # ERROR: prefix and the raw lines
        String content = Files.readString(rejectFile);
        assertThat(content).contains("# ERROR: Invalid contract structure");
        assertThat(content).contains("CTR;EUR;16;ABC");
        assertThat(content).contains("IKAC;001;DATA");
    }
}
