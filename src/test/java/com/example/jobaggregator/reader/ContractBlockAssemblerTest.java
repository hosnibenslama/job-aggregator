package com.example.jobaggregator.reader;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.example.jobaggregator.domain.Article;
import com.example.jobaggregator.domain.ContractBlock;
import com.example.jobaggregator.domain.MarketedObject;
import com.example.jobaggregator.domain.feed.FeedRecordType;
import com.example.jobaggregator.domain.feed.FeedRecord;
import com.example.jobaggregator.error.ContractFormatException;
import java.util.List;
import org.junit.jupiter.api.Test;

class ContractBlockAssemblerTest {

    @Test
    void shouldBuildValidContractWhenAllMandatoryAndOptionalLinesAreProvided() {
        // Given: An assembler initialized with a CTR line and fed with all required and optional lines
        ContractBlockAssembler assembler = new ContractBlockAssembler(createFeedRecord(1, FeedRecordType.CTR, "CTR"));
        assembler.accept(createFeedRecord(2, FeedRecordType.ACC, "ACC", "BILL"));
        assembler.accept(createFeedRecord(3, FeedRecordType.ROL, "ROL"));
        assembler.accept(createFeedRecord(4, FeedRecordType.OFF, "OFF"));
        assembler.accept(createFeedRecord(5, FeedRecordType.OM, "OM", "OM-001"));
        assembler.accept(createFeedRecord(6, FeedRecordType.OID, "OID"));
        assembler.accept(createFeedRecord(7, FeedRecordType.ART, "ART", "1"));
        assembler.accept(createFeedRecord(8, FeedRecordType.IKAC, "IKAC"));
        assembler.accept(createFeedRecord(9, FeedRecordType.COND, "COND"));
        assembler.accept(createFeedRecord(10, FeedRecordType.ACC, "ACC", "BILL-2"));
        assembler.accept(createFeedRecord(11, FeedRecordType.TAR, "TAR"));
        assembler.accept(createFeedRecord(12, FeedRecordType.AVT, "AVT"));

        // Act: Build the assembled contract
        ContractBlock contract = assembler.build();

        // Assert: Assembled contract contains all 12 accepted lines
        assertThat(contract.records()).hasSize(12);
        assertThat(contract.rawRecords()).hasSize(12);

        // Assert: Contract-level children
        assertThat(contract.accounts()).hasSize(1);
        assertThat(contract.roles()).hasSize(1);
        assertThat(contract.offers()).hasSize(1);

        // Assert: OM-level children
        assertThat(contract.marketedObjects()).hasSize(1);
        MarketedObject om = contract.marketedObjects().getFirst();
        assertThat(om.omId()).isEqualTo("OM-001");
        assertThat(om.externalIds()).hasSize(1);

        // Assert: Article-level children
        assertThat(om.articles()).hasSize(1);
        Article art = om.articles().getFirst();
        assertThat(art.sequentialIndex()).isEqualTo(1);
        assertThat(art.ikacs()).hasSize(1);
        assertThat(art.conditions()).hasSize(1);
        assertThat(art.accounts()).hasSize(1);
        assertThat(art.tarifs()).hasSize(1);
        assertThat(art.advantages()).hasSize(1);
    }

    @Test
    void shouldThrowContractFormatExceptionWhenArticleChildEncounteredBeforeArticle() {
        // Given: An assembler containing CTR, ACC, and OM, but no preceding ART line
        ContractBlockAssembler assembler = new ContractBlockAssembler(createFeedRecord(1, FeedRecordType.CTR, "CTR"));
        assembler.accept(createFeedRecord(2, FeedRecordType.ACC, "ACC", "BILL"));
        assembler.accept(createFeedRecord(3, FeedRecordType.OM, "OM", "OM-001"));

        // Act & Assert: Accepting an IKAC line before any ART line throws ContractFormatException
        assertThatThrownBy(() -> assembler.accept(createFeedRecord(4, FeedRecordType.IKAC, "IKAC")))
                .isInstanceOf(ContractFormatException.class)
                .hasMessageContaining("ART");
    }

    @Test
    void shouldBuildValidContractWhenOnlyMandatoryLinesAreProvided() {
        // Given: An assembler initialized with only the minimum mandatory lines (CTR, ACC, OM, ART)
        ContractBlockAssembler assembler = new ContractBlockAssembler(createFeedRecord(1, FeedRecordType.CTR, "CTR"));
        assembler.accept(createFeedRecord(2, FeedRecordType.ACC, "ACC", "BILL"));
        assembler.accept(createFeedRecord(3, FeedRecordType.OM, "OM", "OM-001"));
        assembler.accept(createFeedRecord(4, FeedRecordType.ART, "ART", "1"));

        // Act: Build the assembled contract
        ContractBlock contract = assembler.build();

        // Assert: Assembled contract contains exactly the 4 mandatory lines
        assertThat(contract.records()).hasSize(4);
        assertThat(contract.rawRecords()).hasSize(4);
        assertThat(contract.marketedObjects()).hasSize(1);
        assertThat(contract.marketedObjects().getFirst().articles()).hasSize(1);
    }

    @Test
    void shouldThrowContractFormatExceptionWhenOidEncounteredBeforeOm() {
        // Given: An assembler containing CTR and ACC lines without any preceding OM line
        ContractBlockAssembler assembler = new ContractBlockAssembler(createFeedRecord(1, FeedRecordType.CTR, "CTR"));
        assembler.accept(createFeedRecord(2, FeedRecordType.ACC, "ACC", "BILL"));

        // Act & Assert: Accepting an OID line before an OM line throws ContractFormatException
        assertThatThrownBy(() -> assembler.accept(createFeedRecord(3, FeedRecordType.OID, "OID")))
                .isInstanceOf(ContractFormatException.class)
                .hasMessageContaining("OID");
    }

    @Test
    void shouldThrowContractFormatExceptionWhenBuildingContractWithoutAcc() {
        // Given: An assembler with only a CTR line and missing the mandatory ACC line
        ContractBlockAssembler assembler = new ContractBlockAssembler(createFeedRecord(1, FeedRecordType.CTR, "CTR"));

        // Act & Assert: Building the contract without ACC throws ContractFormatException
        assertThatThrownBy(assembler::build)
                .isInstanceOf(ContractFormatException.class)
                .hasMessageContaining("ACC");
    }

    @Test
    void shouldThrowContractFormatExceptionWhenBuildingContractWithoutOm() {
        // Given: An assembler with CTR and ACC lines but missing the mandatory OM line
        ContractBlockAssembler assembler = new ContractBlockAssembler(createFeedRecord(1, FeedRecordType.CTR, "CTR"));
        assembler.accept(createFeedRecord(2, FeedRecordType.ACC, "ACC", "BILL"));

        // Act & Assert: Building the contract without OM throws ContractFormatException
        assertThatThrownBy(assembler::build)
                .isInstanceOf(ContractFormatException.class)
                .hasMessageContaining("OM");
    }

    private FeedRecord createFeedRecord(long number, FeedRecordType type, String... fields) {
        return new FeedRecord(number, type, String.join(";", fields), List.of(fields));
    }
}
