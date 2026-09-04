package com.example.jobaggregator.reader;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.example.jobaggregator.domain.Contract;
import com.example.jobaggregator.domain.LineType;
import com.example.jobaggregator.domain.ParsedLine;
import com.example.jobaggregator.error.ContractFormatException;
import java.util.List;
import org.junit.jupiter.api.Test;

class ContractBlockAssemblerTest {

    @Test
    void buildsAValidContract() {
        ContractBlockAssembler assembler = new ContractBlockAssembler(line(1, LineType.CTR, "CTR"));
        assembler.accept(line(2, LineType.ACC, "ACC", "BILL"));
        assembler.accept(line(3, LineType.ROL, "ROL"));
        assembler.accept(line(4, LineType.OFF, "OFF"));
        assembler.accept(line(5, LineType.OM, "OM", "OM-001"));
        assembler.accept(line(6, LineType.OID, "OID"));
        assembler.accept(line(7, LineType.ART, "ART", "1"));
        assembler.accept(line(8, LineType.IKAC, "IKAC"));
        assembler.accept(line(9, LineType.COND, "COND"));
        assembler.accept(line(10, LineType.ACC, "ACC", "BILL-2"));
        assembler.accept(line(11, LineType.TAR, "TAR"));
        assembler.accept(line(12, LineType.AVT, "AVT"));

        Contract contract = assembler.build();

        assertThat(contract.lines()).hasSize(12);
    }

    @Test
    void rejectsAnArticleChildBeforeArticle() {
        ContractBlockAssembler assembler = new ContractBlockAssembler(line(1, LineType.CTR, "CTR"));
        assembler.accept(line(2, LineType.ACC, "ACC", "BILL"));
        assembler.accept(line(3, LineType.OM, "OM", "OM-001"));

        assertThatThrownBy(() -> assembler.accept(line(4, LineType.IKAC, "IKAC")))
                .isInstanceOf(ContractFormatException.class)
                .hasMessageContaining("ART");
    }

    @Test
    void buildsContractWithoutOptionalFields() {
        ContractBlockAssembler assembler = new ContractBlockAssembler(line(1, LineType.CTR, "CTR"));
        assembler.accept(line(2, LineType.ACC, "ACC", "BILL"));
        assembler.accept(line(3, LineType.OM, "OM", "OM-001"));
        assembler.accept(line(4, LineType.ART, "ART", "1"));

        Contract contract = assembler.build();

        assertThat(contract.lines()).hasSize(4);
    }

    @Test
    void rejectsOidBeforeAnyOm() {
        ContractBlockAssembler assembler = new ContractBlockAssembler(line(1, LineType.CTR, "CTR"));
        assembler.accept(line(2, LineType.ACC, "ACC", "BILL"));

        assertThatThrownBy(() -> assembler.accept(line(3, LineType.OID, "OID")))
                .isInstanceOf(ContractFormatException.class)
                .hasMessageContaining("OID");
    }

    @Test
    void rejectsMissingAccInBuild() {
        ContractBlockAssembler assembler = new ContractBlockAssembler(line(1, LineType.CTR, "CTR"));

        assertThatThrownBy(assembler::build)
                .isInstanceOf(ContractFormatException.class)
                .hasMessageContaining("ACC");
    }

    @Test
    void rejectsMissingOmInBuild() {
        ContractBlockAssembler assembler = new ContractBlockAssembler(line(1, LineType.CTR, "CTR"));
        assembler.accept(line(2, LineType.ACC, "ACC", "BILL"));

        assertThatThrownBy(assembler::build)
                .isInstanceOf(ContractFormatException.class)
                .hasMessageContaining("OM");
    }

    private ParsedLine line(long number, LineType type, String... fields) {
        return new ParsedLine(number, type, String.join(";", fields), List.of(fields));
    }
}
