package com.example.jobaggregator.reader;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.example.jobaggregator.domain.ContractBlock;
import com.example.jobaggregator.domain.LineType;
import com.example.jobaggregator.domain.ParsedLine;
import com.example.jobaggregator.error.ContractFormatException;
import java.util.List;
import org.junit.jupiter.api.Test;

class ContractBlockAssemblerTest {

    @Test
    void shouldBuildValidContractWhenAllMandatoryAndOptionalLinesAreProvided() {
        // Given: An assembler initialized with a CTR line and fed with all required and optional lines
        ContractBlockAssembler assembler = new ContractBlockAssembler(createParsedLine(1, LineType.CTR, "CTR"));
        assembler.accept(createParsedLine(2, LineType.ACC, "ACC", "BILL"));
        assembler.accept(createParsedLine(3, LineType.ROL, "ROL"));
        assembler.accept(createParsedLine(4, LineType.OFF, "OFF"));
        assembler.accept(createParsedLine(5, LineType.OM, "OM", "OM-001"));
        assembler.accept(createParsedLine(6, LineType.OID, "OID"));
        assembler.accept(createParsedLine(7, LineType.ART, "ART", "1"));
        assembler.accept(createParsedLine(8, LineType.IKAC, "IKAC"));
        assembler.accept(createParsedLine(9, LineType.COND, "COND"));
        assembler.accept(createParsedLine(10, LineType.ACC, "ACC", "BILL-2"));
        assembler.accept(createParsedLine(11, LineType.TAR, "TAR"));
        assembler.accept(createParsedLine(12, LineType.AVT, "AVT"));

        // Act: Build the assembled contract
        ContractBlock contract = assembler.build();

        // Assert: Assembled contract contains all 12 accepted lines
        assertThat(contract.lines()).hasSize(12);
        assertThat(contract.rawLines()).hasSize(12);
    }

    @Test
    void shouldThrowContractFormatExceptionWhenArticleChildEncounteredBeforeArticle() {
        // Given: An assembler containing CTR, ACC, and OM, but no preceding ART line
        ContractBlockAssembler assembler = new ContractBlockAssembler(createParsedLine(1, LineType.CTR, "CTR"));
        assembler.accept(createParsedLine(2, LineType.ACC, "ACC", "BILL"));
        assembler.accept(createParsedLine(3, LineType.OM, "OM", "OM-001"));

        // Act & Assert: Accepting an IKAC line before any ART line throws ContractFormatException
        assertThatThrownBy(() -> assembler.accept(createParsedLine(4, LineType.IKAC, "IKAC")))
                .isInstanceOf(ContractFormatException.class)
                .hasMessageContaining("ART");
    }

    @Test
    void shouldBuildValidContractWhenOnlyMandatoryLinesAreProvided() {
        // Given: An assembler initialized with only the minimum mandatory lines (CTR, ACC, OM, ART)
        ContractBlockAssembler assembler = new ContractBlockAssembler(createParsedLine(1, LineType.CTR, "CTR"));
        assembler.accept(createParsedLine(2, LineType.ACC, "ACC", "BILL"));
        assembler.accept(createParsedLine(3, LineType.OM, "OM", "OM-001"));
        assembler.accept(createParsedLine(4, LineType.ART, "ART", "1"));

        // Act: Build the assembled contract
        ContractBlock contract = assembler.build();

        // Assert: Assembled contract contains exactly the 4 mandatory lines
        assertThat(contract.lines()).hasSize(4);
        assertThat(contract.rawLines()).hasSize(4);
    }

    @Test
    void shouldThrowContractFormatExceptionWhenOidEncounteredBeforeOm() {
        // Given: An assembler containing CTR and ACC lines without any preceding OM line
        ContractBlockAssembler assembler = new ContractBlockAssembler(createParsedLine(1, LineType.CTR, "CTR"));
        assembler.accept(createParsedLine(2, LineType.ACC, "ACC", "BILL"));

        // Act & Assert: Accepting an OID line before an OM line throws ContractFormatException
        assertThatThrownBy(() -> assembler.accept(createParsedLine(3, LineType.OID, "OID")))
                .isInstanceOf(ContractFormatException.class)
                .hasMessageContaining("OID");
    }

    @Test
    void shouldThrowContractFormatExceptionWhenBuildingContractWithoutAcc() {
        // Given: An assembler with only a CTR line and missing the mandatory ACC line
        ContractBlockAssembler assembler = new ContractBlockAssembler(createParsedLine(1, LineType.CTR, "CTR"));

        // Act & Assert: Building the contract without ACC throws ContractFormatException
        assertThatThrownBy(assembler::build)
                .isInstanceOf(ContractFormatException.class)
                .hasMessageContaining("ACC");
    }

    @Test
    void shouldThrowContractFormatExceptionWhenBuildingContractWithoutOm() {
        // Given: An assembler with CTR and ACC lines but missing the mandatory OM line
        ContractBlockAssembler assembler = new ContractBlockAssembler(createParsedLine(1, LineType.CTR, "CTR"));
        assembler.accept(createParsedLine(2, LineType.ACC, "ACC", "BILL"));

        // Act & Assert: Building the contract without OM throws ContractFormatException
        assertThatThrownBy(assembler::build)
                .isInstanceOf(ContractFormatException.class)
        	.hasMessageContaining("OM");
    }

    private ParsedLine createParsedLine(long number, LineType type, String... fields) {
        return new ParsedLine(number, type, String.join(";", fields), List.of(fields));
    }
}
