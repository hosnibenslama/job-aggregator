package com.example.jobaggregator.reader;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;

import com.example.jobaggregator.domain.BusinessLine;
import com.example.jobaggregator.domain.Contract;
import com.example.jobaggregator.domain.LineType;
import com.example.jobaggregator.writer.InvalidContractFileWriter;
import java.util.List;
import org.junit.jupiter.api.Test;
import org.springframework.batch.infrastructure.item.support.ListItemReader;
import org.springframework.batch.infrastructure.item.support.SingleItemPeekableItemReader;

/**
 * Unit tests for {@link ContractFileReader}: contract aggregation and skip-and-reject behaviour.
 */
class ContractFileReaderTest {

    private static ContractFileReader readerFor(List<BusinessLine> lines) {
        SingleItemPeekableItemReader<BusinessLine> peekableReader =
                new SingleItemPeekableItemReader<>(new ListItemReader<>(lines));
        return new ContractFileReader(peekableReader, mock(InvalidContractFileWriter.class));
    }

    private static ContractFileReader readerFor(
            List<BusinessLine> lines, InvalidContractFileWriter rejectWriter) {
        SingleItemPeekableItemReader<BusinessLine> peekableReader =
                new SingleItemPeekableItemReader<>(new ListItemReader<>(lines));
        return new ContractFileReader(peekableReader, rejectWriter);
    }

    // -------------------------------------------------------------------------
    // Normal aggregation
    // -------------------------------------------------------------------------

    @Test
    void aggregatesLinesIntoContract() throws Exception {
        List<BusinessLine> lines = List.of(
                new BusinessLine(1, LineType.HDR, "HDR;20260415", List.of("HDR", "20260415")),
                new BusinessLine(2, LineType.CTR, "CTR", List.of("CTR")),
                new BusinessLine(3, LineType.ACC, "ACC;BILL", List.of("ACC", "BILL")),
                new BusinessLine(4, LineType.OM,  "OM;001",  List.of("OM",  "001")),
                new BusinessLine(5, LineType.ART, "ART;1",   List.of("ART", "1")),
                new BusinessLine(6, LineType.TRL, "TRL;1;5", List.of("TRL", "1", "5"))
        );

        ContractFileReader reader = readerFor(lines);
        Contract contract = reader.read();

        assertThat(contract).isNotNull();
        assertThat(contract.lines()).hasSize(4); // CTR + ACC + OM + ART
        assertThat(reader.read()).isNull();       // TRL → EOF
    }

    @Test
    void aggregatesMultipleContracts() throws Exception {
        List<BusinessLine> lines = List.of(
                new BusinessLine(1,  LineType.HDR, "HDR",   List.of("HDR")),
                new BusinessLine(2,  LineType.CTR, "CTR",   List.of("CTR")),
                new BusinessLine(3,  LineType.ACC, "ACC;1", List.of("ACC", "1")),
                new BusinessLine(4,  LineType.OM,  "OM;1",  List.of("OM",  "1")),
                new BusinessLine(5,  LineType.ART, "ART;1", List.of("ART", "1")),
                new BusinessLine(6,  LineType.CTR, "CTR",   List.of("CTR")),
                new BusinessLine(7,  LineType.ACC, "ACC;2", List.of("ACC", "2")),
                new BusinessLine(8,  LineType.OM,  "OM;2",  List.of("OM",  "2")),
                new BusinessLine(9,  LineType.ART, "ART;2", List.of("ART", "2")),
                new BusinessLine(10, LineType.TRL, "TRL",   List.of("TRL"))
        );

        ContractFileReader reader = readerFor(lines);

        Contract first = reader.read();
        assertThat(first).isNotNull();
        assertThat(first.lines().getFirst().type()).isEqualTo(LineType.CTR);

        Contract second = reader.read();
        assertThat(second).isNotNull();
        assertThat(second.lines().getFirst().lineNumber()).isEqualTo(6);

        assertThat(reader.read()).isNull();
    }

    // -------------------------------------------------------------------------
    // Skip-and-reject behaviour
    // -------------------------------------------------------------------------

    @Test
    void skipsInvalidBlockAndContinuesToNextContract() throws Exception {
        /*
         * File layout:
         *   HDR
         *   CTR  ← good contract 1
         *   ACC
         *   OM
         *   ART
         *   CTR  ← bad contract (no ACC/OM/ART — handled by processor, but
         *           here we simulate it having an explicit bad line that the
         *           reader's structure check catches via HDR inside block)
         *   HDR  ← triggers "HDR not allowed inside block" exception
         *   CTR  ← good contract 2
         *   ACC
         *   OM
         *   ART
         *   TRL
         */
        List<BusinessLine> lines = List.of(
                new BusinessLine(1,  LineType.HDR, "HDR",   List.of("HDR")),
                // Contract 1 — valid
                new BusinessLine(2,  LineType.CTR, "CTR",   List.of("CTR")),
                new BusinessLine(3,  LineType.ACC, "ACC;1", List.of("ACC", "1")),
                new BusinessLine(4,  LineType.OM,  "OM;1",  List.of("OM",  "1")),
                new BusinessLine(5,  LineType.ART, "ART;1", List.of("ART", "1")),
                // Contract 2 — invalid (HDR inside block triggers exception)
                new BusinessLine(6,  LineType.CTR, "CTR",   List.of("CTR")),
                new BusinessLine(7,  LineType.HDR, "HDR",   List.of("HDR")),  // bad!
                // Contract 3 — valid (reader should resume here)
                new BusinessLine(8,  LineType.CTR, "CTR",   List.of("CTR")),
                new BusinessLine(9,  LineType.ACC, "ACC;3", List.of("ACC", "3")),
                new BusinessLine(10, LineType.OM,  "OM;3",  List.of("OM",  "3")),
                new BusinessLine(11, LineType.ART, "ART;3", List.of("ART", "3")),
                new BusinessLine(12, LineType.TRL, "TRL",   List.of("TRL"))
        );

        InvalidContractFileWriter rejectWriter = mock(InvalidContractFileWriter.class);
        ContractFileReader reader = readerFor(lines, rejectWriter);

        // Contract 1 read successfully
        Contract first = reader.read();
        assertThat(first).isNotNull();
        assertThat(first.lines().getFirst().lineNumber()).isEqualTo(2);

        // Contract 2 was rejected internally — reader returns contract 3
        Contract third = reader.read();
        assertThat(third).isNotNull();
        assertThat(third.lines().getFirst().lineNumber()).isEqualTo(8);

        // EOF
        assertThat(reader.read()).isNull();

        // Verify reject writer was called exactly once (for contract 2)
        verify(rejectWriter).reject(any(List.class), anyString());
    }

    @Test
    void returnsNullOnEmptyFile() throws Exception {
        ContractFileReader reader = readerFor(List.of(
                new BusinessLine(1, LineType.TRL, "TRL", List.of("TRL"))
        ));
        assertThat(reader.read()).isNull();
    }

    @Test
    void validContractNeverCallsRejectWriter() throws Exception {
        List<BusinessLine> lines = List.of(
                new BusinessLine(1, LineType.CTR, "CTR",      List.of("CTR")),
                new BusinessLine(2, LineType.ACC, "ACC;BILL", List.of("ACC", "BILL")),
                new BusinessLine(3, LineType.OM,  "OM;001",   List.of("OM",  "001")),
                new BusinessLine(4, LineType.ART, "ART;1",    List.of("ART", "1")),
                new BusinessLine(5, LineType.TRL, "TRL",      List.of("TRL"))
        );
        InvalidContractFileWriter rejectWriter = mock(InvalidContractFileWriter.class);
        ContractFileReader reader = readerFor(lines, rejectWriter);

        reader.read(); // returns the contract
        reader.read(); // returns null (TRL/EOF)

        verify(rejectWriter, never()).reject(any(List.class), anyString());
    }
}
