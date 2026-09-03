package com.example.jobaggregator.reader;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.example.jobaggregator.domain.BusinessLine;
import com.example.jobaggregator.domain.Contract;
import com.example.jobaggregator.domain.LineType;
import com.example.jobaggregator.error.ContractFormatException;
import java.util.List;
import org.junit.jupiter.api.Test;
import org.springframework.batch.infrastructure.item.support.ListItemReader;
import org.springframework.batch.infrastructure.item.support.SingleItemPeekableItemReader;

class ContractFileReaderTest {

    @Test
    void aggregatesLinesIntoContract() throws Exception {
        List<BusinessLine> lines = List.of(
                new BusinessLine(1, LineType.HDR, "HDR;20260415", List.of("HDR", "20260415")),
                new BusinessLine(2, LineType.CTR, "CTR", List.of("CTR")),
                new BusinessLine(3, LineType.ACC, "ACC;BILL", List.of("ACC", "BILL")),
                new BusinessLine(4, LineType.OM, "OM;001", List.of("OM", "001")),
                new BusinessLine(5, LineType.ART, "ART;1", List.of("ART", "1")),
                new BusinessLine(6, LineType.TRL, "TRL;1;5", List.of("TRL", "1", "5"))
        );

        SingleItemPeekableItemReader<BusinessLine> peekableReader =
                new SingleItemPeekableItemReader<>(new ListItemReader<>(lines));
        ContractFileReader reader = new ContractFileReader(peekableReader);

        Contract contract = reader.read();
        assertThat(contract).isNotNull();
        assertThat(contract.firstPhysicalLine()).isEqualTo(2);
        assertThat(contract.lastPhysicalLine()).isEqualTo(5);
        assertThat(contract.lines()).hasSize(4);

        // Next read encounters TRL and returns null (EOF)
        assertThat(reader.read()).isNull();
    }

    @Test
    void aggregatesMultipleContracts() throws Exception {
        List<BusinessLine> lines = List.of(
                new BusinessLine(1, LineType.HDR, "HDR", List.of("HDR")),
                new BusinessLine(2, LineType.CTR, "CTR", List.of("CTR")),
                new BusinessLine(3, LineType.ACC, "ACC;1", List.of("ACC", "1")),
                new BusinessLine(4, LineType.OM, "OM;1", List.of("OM", "1")),
                new BusinessLine(5, LineType.ART, "ART;1", List.of("ART", "1")),
                new BusinessLine(6, LineType.CTR, "CTR", List.of("CTR")),
                new BusinessLine(7, LineType.ACC, "ACC;2", List.of("ACC", "2")),
                new BusinessLine(8, LineType.OM, "OM;2", List.of("OM", "2")),
                new BusinessLine(9, LineType.ART, "ART;2", List.of("ART", "2")),
                new BusinessLine(10, LineType.TRL, "TRL", List.of("TRL"))
        );

        SingleItemPeekableItemReader<BusinessLine> peekableReader =
                new SingleItemPeekableItemReader<>(new ListItemReader<>(lines));
        ContractFileReader reader = new ContractFileReader(peekableReader);

        Contract first = reader.read();
        assertThat(first).isNotNull();
        assertThat(first.firstPhysicalLine()).isEqualTo(2);

        Contract second = reader.read();
        assertThat(second).isNotNull();
        assertThat(second.firstPhysicalLine()).isEqualTo(6);

        assertThat(reader.read()).isNull();
    }
}
