package com.example.jobaggregator.reader;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.example.jobaggregator.domain.BusinessLine;
import com.example.jobaggregator.domain.Contract;
import com.example.jobaggregator.domain.LineType;
import com.example.jobaggregator.error.ContractFormatException;
import java.util.List;
import org.junit.jupiter.api.Test;

class ContractBuilderTest {

    @Test
    void buildsAValidContract() {
        ContractBuilder builder = new ContractBuilder(line(1, LineType.CTR, "CTR"));
        builder.accept(line(2, LineType.ACC, "ACC", "BILL"));
        builder.accept(line(3, LineType.ROL, "ROL"));
        builder.accept(line(4, LineType.OFF, "OFF"));
        builder.accept(line(5, LineType.OM, "OM", "OM-001"));
        builder.accept(line(6, LineType.OID, "OID"));
        builder.accept(line(7, LineType.ART, "ART", "1"));
        builder.accept(line(8, LineType.IKAC, "IKAC"));
        builder.accept(line(9, LineType.COND, "COND"));
        builder.accept(line(10, LineType.ACC, "ACC", "BILL-2"));
        builder.accept(line(11, LineType.TAR, "TAR"));
        builder.accept(line(12, LineType.AVT, "AVT"));

        Contract contract = builder.build();

        assertThat(contract.lines()).hasSize(12);
        assertThat(contract.firstPhysicalLine()).isEqualTo(1);
        assertThat(contract.lastPhysicalLine()).isEqualTo(12);
    }

    @Test
    void rejectsAnArticleChildBeforeArticle() {
        ContractBuilder builder = new ContractBuilder(line(1, LineType.CTR, "CTR"));
        builder.accept(line(2, LineType.ACC, "ACC", "BILL"));
        builder.accept(line(3, LineType.OM, "OM", "OM-001"));

        assertThatThrownBy(() -> builder.accept(line(4, LineType.IKAC, "IKAC")))
                .isInstanceOf(ContractFormatException.class)
                .hasMessageContaining("ART");
    }

    @Test
    void buildsContractWithoutOptionalFields() {
        ContractBuilder builder = new ContractBuilder(line(1, LineType.CTR, "CTR"));
        builder.accept(line(2, LineType.ACC, "ACC", "BILL"));
        builder.accept(line(3, LineType.OM, "OM", "OM-001"));
        builder.accept(line(4, LineType.ART, "ART", "1"));

        Contract contract = builder.build();

        assertThat(contract.lines()).hasSize(4);
    }

    private BusinessLine line(long number, LineType type, String... fields) {
        return new BusinessLine(number, type, String.join(";", fields), List.of(fields));
    }
}
