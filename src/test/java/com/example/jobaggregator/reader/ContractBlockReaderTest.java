package com.example.jobaggregator.reader;

import static org.assertj.core.api.Assertions.assertThat;

import com.example.jobaggregator.domain.Contract;
import com.example.jobaggregator.domain.LineType;
import com.example.jobaggregator.domain.ParsedLine;
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
    void groupsLinesIntoContract() throws Exception {
        List<ParsedLine> lines = List.of(
                new ParsedLine(1, LineType.HDR, "HDR;20260415", List.of("HDR", "20260415")),
                new ParsedLine(2, LineType.CTR, "CTR", List.of("CTR")),
                new ParsedLine(3, LineType.ACC, "ACC;BILL", List.of("ACC", "BILL")),
                new ParsedLine(4, LineType.OM,  "OM;001",  List.of("OM",  "001")),
                new ParsedLine(5, LineType.ART, "ART;1",   List.of("ART", "1")),
                new ParsedLine(6, LineType.TRL, "TRL;1;5", List.of("TRL", "1", "5"))
        );

        ContractBlockReader reader = readerFor(lines);
        Contract contract = reader.read();

        assertThat(contract).isNotNull();
        assertThat(contract.lines()).hasSize(4); // CTR + ACC + OM + ART
        assertThat(reader.read()).isNull();       // TRL → EOF
    }

    @Test
    void groupsMultipleContracts() throws Exception {
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

        Contract first = reader.read();
        assertThat(first).isNotNull();
        assertThat(first.lines()).hasSize(4);
        assertThat(first.lines().getFirst().type()).isEqualTo(LineType.CTR);

        Contract second = reader.read();
        assertThat(second).isNotNull();
        assertThat(second.lines()).hasSize(4);
        assertThat(second.lines().getFirst().lineNumber()).isEqualTo(6);

        assertThat(reader.read()).isNull();
    }

    @Test
    void returnsNullOnTrlOnly() throws Exception {
        ContractBlockReader reader = readerFor(List.of(
                new ParsedLine(1, LineType.TRL, "TRL", List.of("TRL"))
        ));
        assertThat(reader.read()).isNull();
    }

    @Test
    void returnsNullOnEmptyInput() throws Exception {
        ContractBlockReader reader = readerFor(List.of());
        assertThat(reader.read()).isNull();
    }

    @Test
    void skipsHeaderLine() throws Exception {
        List<ParsedLine> lines = List.of(
                new ParsedLine(1, LineType.HDR, "HDR", List.of("HDR")),
                new ParsedLine(2, LineType.CTR, "CTR", List.of("CTR")),
                new ParsedLine(3, LineType.ACC, "ACC;BILL", List.of("ACC", "BILL")),
                new ParsedLine(4, LineType.OM,  "OM;001",   List.of("OM",  "001")),
                new ParsedLine(5, LineType.ART, "ART;1",    List.of("ART", "1"))
        );

        ContractBlockReader reader = readerFor(lines);
        Contract contract = reader.read();

        assertThat(contract).isNotNull();
        assertThat(contract.lines().getFirst().type()).isEqualTo(LineType.CTR);
        assertThat(contract.lines()).noneMatch(l -> l.type() == LineType.HDR);
    }
}
