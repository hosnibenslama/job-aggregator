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

    @Test
    void shouldRouteCoexistingMultiLevelTarifAndAdvantageAcrossAllLevels() {
        // Given: A contract dossier with TAR and AVT present at Contract, OM, and Article levels simultaneously
        ContractBlockAssembler assembler = new ContractBlockAssembler(createFeedRecord(1, FeedRecordType.CTR, "CTR"));
        assembler.accept(createFeedRecord(2, FeedRecordType.ACC, "ACC", "BILL"));
        assembler.accept(createFeedRecord(3, FeedRecordType.TAR, "TAR", "TAR-CONTRACT"));
        assembler.accept(createFeedRecord(4, FeedRecordType.AVT, "AVT", "AVT-CONTRACT"));
        assembler.accept(createFeedRecord(5, FeedRecordType.OM, "OM", "OM-001"));
        assembler.accept(createFeedRecord(6, FeedRecordType.TAR, "TAR", "TAR-OM"));
        assembler.accept(createFeedRecord(7, FeedRecordType.AVT, "AVT", "AVT-OM"));
        assembler.accept(createFeedRecord(8, FeedRecordType.ART, "ART", "1"));
        assembler.accept(createFeedRecord(9, FeedRecordType.TAR, "TAR", "TAR-ART"));
        assembler.accept(createFeedRecord(10, FeedRecordType.AVT, "AVT", "AVT-ART"));

        // Act: Build the contract block
        ContractBlock contract = assembler.build();

        // Assert: Verify Contract level contains only contract-level TAR & AVT
        assertThat(contract.tarifs()).hasSize(1);
        assertThat(contract.tarifs().getFirst().idOpraTarif()).isEqualTo("TAR-CONTRACT");
        assertThat(contract.advantages()).hasSize(1);
        assertThat(contract.advantages().getFirst().idOpraAvantage()).isEqualTo("AVT-CONTRACT");

        // Assert: Verify OM level contains only OM-level TAR & AVT
        assertThat(contract.marketedObjects()).hasSize(1);
        MarketedObject om = contract.marketedObjects().getFirst();
        assertThat(om.tarifs()).hasSize(1);
        assertThat(om.tarifs().getFirst().idOpraTarif()).isEqualTo("TAR-OM");
        assertThat(om.advantages()).hasSize(1);
        assertThat(om.advantages().getFirst().idOpraAvantage()).isEqualTo("AVT-OM");

        // Assert: Verify Article level contains only Article-level TAR & AVT
        assertThat(om.articles()).hasSize(1);
        Article art = om.articles().getFirst();
        assertThat(art.tarifs()).hasSize(1);
        assertThat(art.tarifs().getFirst().idOpraTarif()).isEqualTo("TAR-ART");
        assertThat(art.advantages()).hasSize(1);
        assertThat(art.advantages().getFirst().idOpraAvantage()).isEqualTo("AVT-ART");
    }

    @Test
    void shouldRouteMultiLevelRolesExternalIdsAndAccountsToTheirRespectiveScopes() {
        // Given: ROL at Contract, OM, and Article levels, OID at OM and Article levels, ACC at Contract and Article levels
        ContractBlockAssembler assembler = new ContractBlockAssembler(createFeedRecord(1, FeedRecordType.CTR, "CTR"));
        assembler.accept(createFeedRecord(2, FeedRecordType.ACC, "ACC", "BILL-CTR"));
        assembler.accept(createFeedRecord(3, FeedRecordType.ROL, "ROL", "ROL-CTR"));
        assembler.accept(createFeedRecord(4, FeedRecordType.OM, "OM", "OM-001"));
        assembler.accept(createFeedRecord(5, FeedRecordType.OID, "OID", "OID-OM"));
        assembler.accept(createFeedRecord(6, FeedRecordType.ROL, "ROL", "ROL-OM"));
        assembler.accept(createFeedRecord(7, FeedRecordType.ART, "ART", "1"));
        assembler.accept(createFeedRecord(8, FeedRecordType.OID, "OID", "OID-ART"));
        assembler.accept(createFeedRecord(9, FeedRecordType.ACC, "ACC", "BILL-ART"));
        assembler.accept(createFeedRecord(10, FeedRecordType.ROL, "ROL", "ROL-ART"));

        // Act: Build the contract block
        ContractBlock contract = assembler.build();

        // Assert: Contract-level children
        assertThat(contract.accounts()).extracting("subType").containsExactly("BILL-CTR");
        assertThat(contract.roles()).extracting("role").containsExactly("ROL-CTR");

        // Assert: OM-level children
        MarketedObject om = contract.marketedObjects().getFirst();
        assertThat(om.externalIds()).extracting("externalId").containsExactly("OID-OM");
        assertThat(om.roles()).extracting("role").containsExactly("ROL-OM");

        // Assert: Article-level children
        Article art = om.articles().getFirst();
        assertThat(art.externalIds()).extracting("externalId").containsExactly("OID-ART");
        assertThat(art.accounts()).extracting("subType").containsExactly("BILL-ART");
        assertThat(art.roles()).extracting("role").containsExactly("ROL-ART");
    }

    @Test
    void shouldAssembleMultipleMarketedObjectsAndMultipleArticlesWithoutDataLeakage() {
        // Given: 2 OMs where OM1 has 2 articles and OM2 has 1 article, with distinct children
        ContractBlockAssembler assembler = new ContractBlockAssembler(createFeedRecord(1, FeedRecordType.CTR, "CTR"));
        assembler.accept(createFeedRecord(2, FeedRecordType.ACC, "ACC", "BILL"));

        // OM 1
        assembler.accept(createFeedRecord(3, FeedRecordType.OM, "OM", "OM-001", "REL-001"));
        assembler.accept(createFeedRecord(4, FeedRecordType.OID, "OID", "OM1-OID"));
        // OM 1 - Article 1
        assembler.accept(createFeedRecord(5, FeedRecordType.ART, "ART", "1"));
        assembler.accept(createFeedRecord(6, FeedRecordType.COND, "COND", "COND-1A", "VAL-1A"));
        assembler.accept(createFeedRecord(7, FeedRecordType.TAR, "TAR", "TAR-ART1"));
        // OM 1 - Article 2
        assembler.accept(createFeedRecord(8, FeedRecordType.ART, "ART", "2"));
        assembler.accept(createFeedRecord(9, FeedRecordType.IKAC, "IKAC", "IKAC-ART2"));
        assembler.accept(createFeedRecord(10, FeedRecordType.AVT, "AVT", "AVT-ART2"));

        // OM 2
        assembler.accept(createFeedRecord(11, FeedRecordType.OM, "OM", "OM-002", "REL-002"));
        assembler.accept(createFeedRecord(12, FeedRecordType.ROL, "ROL", "ROL-OM2"));
        // OM 2 - Article 1
        assembler.accept(createFeedRecord(13, FeedRecordType.ART, "ART", "1"));
        assembler.accept(createFeedRecord(14, FeedRecordType.COND, "COND", "COND-2A", "VAL-2A"));

        // Act: Build contract
        ContractBlock contract = assembler.build();

        // Assert: 2 OMs assembled
        assertThat(contract.marketedObjects()).hasSize(2);

        // OM 1 checks
        MarketedObject om1 = contract.marketedObjects().get(0);
        assertThat(om1.omId()).isEqualTo("OM-001");
        assertThat(om1.externalIds()).hasSize(1);
        assertThat(om1.roles()).isEmpty();
        assertThat(om1.articles()).hasSize(2);

        Article om1Art1 = om1.articles().get(0);
        assertThat(om1Art1.sequentialIndex()).isEqualTo(1);
        assertThat(om1Art1.conditions()).hasSize(1);
        assertThat(om1Art1.tarifs()).hasSize(1);
        assertThat(om1Art1.ikacs()).isEmpty();
        assertThat(om1Art1.advantages()).isEmpty();

        Article om1Art2 = om1.articles().get(1);
        assertThat(om1Art2.sequentialIndex()).isEqualTo(2);
        assertThat(om1Art2.ikacs()).hasSize(1);
        assertThat(om1Art2.advantages()).hasSize(1);
        assertThat(om1Art2.conditions()).isEmpty();
        assertThat(om1Art2.tarifs()).isEmpty();

        // OM 2 checks
        MarketedObject om2 = contract.marketedObjects().get(1);
        assertThat(om2.omId()).isEqualTo("OM-002");
        assertThat(om2.externalIds()).isEmpty();
        assertThat(om2.roles()).hasSize(1);
        assertThat(om2.articles()).hasSize(1);

        Article om2Art1 = om2.articles().get(0);
        assertThat(om2Art1.sequentialIndex()).isEqualTo(1);
        assertThat(om2Art1.conditions()).hasSize(1);
        assertThat(om2Art1.conditions().getFirst().conditionId()).isEqualTo("COND-2A");
        assertThat(om2Art1.ikacs()).isEmpty();
        assertThat(om2Art1.tarifs()).isEmpty();
        assertThat(om2Art1.advantages()).isEmpty();
    }

    @Test
    void shouldAssembleContractWithNoTarifOrAdvantageAtAnyLevel() {
        // Given: Only mandatory records (CTR, ACC, OM, ART)
        ContractBlockAssembler assembler = new ContractBlockAssembler(createFeedRecord(1, FeedRecordType.CTR, "CTR"));
        assembler.accept(createFeedRecord(2, FeedRecordType.ACC, "ACC", "BILL"));
        assembler.accept(createFeedRecord(3, FeedRecordType.OM, "OM", "OM-001"));
        assembler.accept(createFeedRecord(4, FeedRecordType.ART, "ART", "1"));

        // Act: Build contract
        ContractBlock contract = assembler.build();

        // Assert: Tarifs and advantages are empty across all levels
        assertThat(contract.tarifs()).isEmpty();
        assertThat(contract.advantages()).isEmpty();
        assertThat(contract.marketedObjects().getFirst().tarifs()).isEmpty();
        assertThat(contract.marketedObjects().getFirst().advantages()).isEmpty();
        assertThat(contract.marketedObjects().getFirst().articles().getFirst().tarifs()).isEmpty();
        assertThat(contract.marketedObjects().getFirst().articles().getFirst().advantages()).isEmpty();
    }

    @Test
    void shouldAssembleContractWithTarifAtContractLevelOnly() {
        // Given: TAR at Contract level only
        ContractBlockAssembler assembler = new ContractBlockAssembler(createFeedRecord(1, FeedRecordType.CTR, "CTR"));
        assembler.accept(createFeedRecord(2, FeedRecordType.ACC, "ACC", "BILL"));
        assembler.accept(createFeedRecord(3, FeedRecordType.TAR, "TAR", "TAR-CTR-ONLY"));
        assembler.accept(createFeedRecord(4, FeedRecordType.OM, "OM", "OM-001"));
        assembler.accept(createFeedRecord(5, FeedRecordType.ART, "ART", "1"));

        // Act: Build contract
        ContractBlock contract = assembler.build();

        // Assert: Contract has 1 tarif, OM and Article have none
        assertThat(contract.tarifs()).hasSize(1);
        assertThat(contract.tarifs().getFirst().idOpraTarif()).isEqualTo("TAR-CTR-ONLY");
        assertThat(contract.marketedObjects().getFirst().tarifs()).isEmpty();
        assertThat(contract.marketedObjects().getFirst().articles().getFirst().tarifs()).isEmpty();
    }

    @Test
    void shouldAssembleContractWithTarifAtArticleLevelOnly() {
        // Given: TAR at Article level only
        ContractBlockAssembler assembler = new ContractBlockAssembler(createFeedRecord(1, FeedRecordType.CTR, "CTR"));
        assembler.accept(createFeedRecord(2, FeedRecordType.ACC, "ACC", "BILL"));
        assembler.accept(createFeedRecord(3, FeedRecordType.OM, "OM", "OM-001"));
        assembler.accept(createFeedRecord(4, FeedRecordType.ART, "ART", "1"));
        assembler.accept(createFeedRecord(5, FeedRecordType.TAR, "TAR", "TAR-ART-ONLY"));

        // Act: Build contract
        ContractBlock contract = assembler.build();

        // Assert: Contract and OM have none, Article has 1 tarif
        assertThat(contract.tarifs()).isEmpty();
        assertThat(contract.marketedObjects().getFirst().tarifs()).isEmpty();
        assertThat(contract.marketedObjects().getFirst().articles().getFirst().tarifs()).hasSize(1);
        assertThat(contract.marketedObjects().getFirst().articles().getFirst().tarifs().getFirst().idOpraTarif())
                .isEqualTo("TAR-ART-ONLY");
    }

    @Test
    void shouldSupportLenientAssemblyViaToContractBlockWithoutMandatoryElements() {
        // Given: An assembler with only a CTR line (missing ACC, OM, ART)
        ContractBlockAssembler assembler = new ContractBlockAssembler(createFeedRecord(1, FeedRecordType.CTR, "CTR"));

        // Act: Lenient build via toContractBlock()
        ContractBlock contract = assembler.toContractBlock();

        // Assert: Block created successfully with empty children
        assertThat(contract).isNotNull();
        assertThat(contract.records()).hasSize(1);
        assertThat(contract.marketedObjects()).isEmpty();
        assertThat(contract.accounts()).isEmpty();
    }

    private FeedRecord createFeedRecord(long number, FeedRecordType type, String... fields) {
        return new FeedRecord(number, type, String.join(";", fields), List.of(fields));
    }
}
