package com.example.jobaggregator.reader;

import static org.assertj.core.api.Assertions.assertThat;

import com.example.jobaggregator.domain.ContractBlock;
import com.example.jobaggregator.domain.feed.LineType;
import com.example.jobaggregator.domain.feed.ParsedLine;
import java.util.List;
import org.junit.jupiter.api.Test;
import org.springframework.batch.infrastructure.item.support.ListItemReader;
import org.springframework.batch.infrastructure.item.support.SingleItemPeekableItemReader;

/**
 * Unit tests for {@link ContractBlockReader}: pure line-grouping behaviour.
 */
class ContractBlockReaderTest {

    private static ContractBlockReader readerFor(List<ParsedLine> lines) {
        SingleItemPeekableItemReader<ParsedLine> peekableReader =
                new SingleItemPeekableItemReader<>(new ListItemReader<>(lines));
        return new ContractBlockReader(peekableReader);
    }

    // -------------------------------------------------------------------------
    // Grouping
    // -------------------------------------------------------------------------

    @Test
    void shouldGroupLinesIntoContractAndIgnoreHeaderAndTrailer() throws Exception {
        // Given: Input lines containing HDR, a complete contract (CTR, ACC, OM, ART), and TRL
        List<ParsedLine> lines = List.of(
                new ParsedLine(1, LineType.HDR, "HDR;20260415", List.of("HDR", "20260415")),
                new ParsedLine(2, LineType.CTR, "CTR", List.of("CTR")),
                new ParsedLine(3, LineType.ACC, "ACC;BILL", List.of("ACC", "BILL")),
                new ParsedLine(4, LineType.OM,  "OM;001",  List.of("OM",  "001")),
                new ParsedLine(5, LineType.ART, "ART;1",   List.of("ART", "1")),
                new ParsedLine(6, LineType.TRL, "TRL;1;5", List.of("TRL", "1", "5"))
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
        List<ParsedLine> lines = List.of(
                new ParsedLine(1,  LineType.HDR, "HDR",   List.of("HDR")),
                new ParsedLine(2,  LineType.CTR, "CTR",   List.of("CTR")),
                new ParsedLine(3,  LineType.ACC, "ACC;1", List.of("ACC", "1")),
                new ParsedLine(4,  LineType.OM,  "OM;1",  List.of("OM",  "1")),
                new ParsedLine(5,  LineType.ART, "ART;1", List.of("ART", "1")),
                new ParsedLine(6,  LineType.CTR, "CTR",   List.of("CTR")),
                new ParsedLine(7,  LineType.ACC, "ACC;2", List.of("ACC", "2")),
                new ParsedLine(8,  LineType.OM,  "OM;2",  List.of("OM",  "2")),
                new ParsedLine(9,  LineType.ART, "ART;2", List.of("ART", "2")),
                new ParsedLine(10, LineType.TRL, "TRL",   List.of("TRL"))
        );
        ContractBlockReader reader = readerFor(lines);

        // Act: Read the two contract blocks sequentially
        ContractBlock first = reader.read();
        ContractBlock second = reader.read();
        ContractBlock third = reader.read();

        // Assert: Both contracts are grouped correctly by CTR boundaries, and third read is null
        assertThat(first).isNotNull();
        assertThat(first.lines()).hasSize(4);
        assertThat(first.lines().getFirst().type()).isEqualTo(LineType.CTR);

        assertThat(second).isNotNull();
        assertThat(second.lines()).hasSize(4);
        assertThat(second.lines().getFirst().lineNumber()).isEqualTo(6);

        assertThat(third).isNull();
    }

    @Test
    void shouldReturnNullWhenInputContainsOnlyTrailerLine() throws Exception {
        // Given: Reader initialized with only a TRL line
        ContractBlockReader reader = readerFor(List.of(
                new ParsedLine(1, LineType.TRL, "TRL", List.of("TRL"))
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
        List<ParsedLine> lines = List.of(
                new ParsedLine(1, LineType.HDR, "HDR", List.of("HDR")),
                new ParsedLine(2, LineType.CTR, "CTR", List.of("CTR")),
                new ParsedLine(3, LineType.ACC, "ACC;BILL", List.of("ACC", "BILL")),
                new ParsedLine(4, LineType.OM,  "OM;001",   List.of("OM",  "001")),
                new ParsedLine(5, LineType.ART, "ART;1",    List.of("ART", "1"))
        );
        ContractBlockReader reader = readerFor(lines);

        // Act: Read the first contract block
        ContractBlock contract = reader.read();

        // Assert: Contract begins with CTR and does not contain the HDR line
        assertThat(contract).isNotNull();
        assertThat(contract.lines().getFirst().type()).isEqualTo(LineType.CTR);
        assertThat(contract.lines()).noneMatch(l -> l.type() == LineType.HDR);
    }
}
