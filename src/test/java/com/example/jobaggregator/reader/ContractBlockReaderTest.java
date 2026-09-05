package com.example.jobaggregator.reader;

import static org.assertj.core.api.Assertions.assertThat;

import com.example.jobaggregator.domain.ContractBlock;
import com.example.jobaggregator.domain.feed.FeedRecordType;
import com.example.jobaggregator.domain.feed.FeedRecord;
import java.util.List;
import org.junit.jupiter.api.Test;
import org.springframework.batch.infrastructure.item.support.ListItemReader;
import org.springframework.batch.infrastructure.item.support.SingleItemPeekableItemReader;

/**
 * Unit tests for {@link ContractBlockReader}: pure line-grouping behaviour.
 */
class ContractBlockReaderTest {

    private static ContractBlockReader readerFor(List<FeedRecord> lines) {
        SingleItemPeekableItemReader<FeedRecord> peekableReader =
                new SingleItemPeekableItemReader<>(new ListItemReader<>(lines));
        return new ContractBlockReader(peekableReader);
    }

    // -------------------------------------------------------------------------
    // Grouping
    // -------------------------------------------------------------------------

    @Test
    void shouldGroupLinesIntoContractAndIgnoreHeaderAndTrailer() throws Exception {
        // Given: Input lines containing HDR, a complete contract (CTR, ACC, OM, ART), and TRL
        List<FeedRecord> lines = List.of(
                new FeedRecord(1, FeedRecordType.HDR, "HDR;20260415", List.of("HDR", "20260415")),
                new FeedRecord(2, FeedRecordType.CTR, "CTR", List.of("CTR")),
                new FeedRecord(3, FeedRecordType.ACC, "ACC;BILL", List.of("ACC", "BILL")),
                new FeedRecord(4, FeedRecordType.OM,  "OM;001",  List.of("OM",  "001")),
                new FeedRecord(5, FeedRecordType.ART, "ART;1",   List.of("ART", "1")),
                new FeedRecord(6, FeedRecordType.TRL, "TRL;1;5", List.of("TRL", "1", "5"))
        );
        ContractBlockReader reader = readerFor(lines);

        // Act: Read the first contract block and subsequent read
        ContractBlock contract = reader.read();
        ContractBlock next = reader.read();

        // Assert: Assembled contract contains only the 4 contract lines, and next read signals EOF (null)
        assertThat(contract).isNotNull();
        assertThat(contract.lines()).hasSize(4); // CTR + ACC + OM + ART
        assertThat(next).isNull();               // TRL -> EOF
    }

    @Test
    void shouldGroupMultipleConsecutiveContracts() throws Exception {
        // Given: Input lines containing two distinct contract blocks separated by CTR
        List<FeedRecord> lines = List.of(
                new FeedRecord(1,  FeedRecordType.HDR, "HDR",   List.of("HDR")),
                new FeedRecord(2,  FeedRecordType.CTR, "CTR",   List.of("CTR")),
                new FeedRecord(3,  FeedRecordType.ACC, "ACC;1", List.of("ACC", "1")),
                new FeedRecord(4,  FeedRecordType.OM,  "OM;1",  List.of("OM",  "1")),
                new FeedRecord(5,  FeedRecordType.ART, "ART;1", List.of("ART", "1")),
                new FeedRecord(6,  FeedRecordType.CTR, "CTR",   List.of("CTR")),
                new FeedRecord(7,  FeedRecordType.ACC, "ACC;2", List.of("ACC", "2")),
                new FeedRecord(8,  FeedRecordType.OM,  "OM;2",  List.of("OM",  "2")),
                new FeedRecord(9,  FeedRecordType.ART, "ART;2", List.of("ART", "2")),
                new FeedRecord(10, FeedRecordType.TRL, "TRL",   List.of("TRL"))
        );
        ContractBlockReader reader = readerFor(lines);

        // Act: Read the two contract blocks sequentially
        ContractBlock first = reader.read();
        ContractBlock second = reader.read();
        ContractBlock third = reader.read();

        // Assert: Both contracts are grouped correctly by CTR boundaries, and third read is null
        assertThat(first).isNotNull();
        assertThat(first.lines()).hasSize(4);
        assertThat(first.lines().getFirst().type()).isEqualTo(FeedRecordType.CTR);

        assertThat(second).isNotNull();
        assertThat(second.lines()).hasSize(4);
        assertThat(second.lines().getFirst().lineNumber()).isEqualTo(6);

        assertThat(third).isNull();
    }

    @Test
    void shouldReturnNullWhenInputContainsOnlyTrailerLine() throws Exception {
        // Given: Reader initialized with only a TRL line
        ContractBlockReader reader = readerFor(List.of(
                new FeedRecord(1, FeedRecordType.TRL, "TRL", List.of("TRL"))
        ));

        // Act: Read contract block
        ContractBlock contract = reader.read();

        // Assert: Reader immediately returns null
        assertThat(contract).isNull();
    }

    @Test
    void shouldReturnNullWhenInputIsEmpty() throws Exception {
        // Given: Reader initialized with an empty list of lines
        ContractBlockReader reader = readerFor(List.of());

        // Act: Read contract block
        ContractBlock contract = reader.read();

        // Assert: Reader returns null
        assertThat(contract).isNull();
    }

    @Test
    void shouldSkipHeaderLineWhenReadingFirstContract() throws Exception {
        // Given: Input lines starting with HDR followed by a contract
        List<FeedRecord> lines = List.of(
                new FeedRecord(1, FeedRecordType.HDR, "HDR", List.of("HDR")),
                new FeedRecord(2, FeedRecordType.CTR, "CTR", List.of("CTR")),
                new FeedRecord(3, FeedRecordType.ACC, "ACC;BILL", List.of("ACC", "BILL")),
                new FeedRecord(4, FeedRecordType.OM,  "OM;001",   List.of("OM",  "001")),
                new FeedRecord(5, FeedRecordType.ART, "ART;1",    List.of("ART", "1"))
        );
        ContractBlockReader reader = readerFor(lines);

        // Act: Read the first contract block
        ContractBlock contract = reader.read();

        // Assert: Contract begins with CTR and does not contain the HDR line
        assertThat(contract).isNotNull();
        assertThat(contract.lines().getFirst().type()).isEqualTo(FeedRecordType.CTR);
        assertThat(contract.lines()).noneMatch(l -> l.type() == FeedRecordType.HDR);
    }
}
