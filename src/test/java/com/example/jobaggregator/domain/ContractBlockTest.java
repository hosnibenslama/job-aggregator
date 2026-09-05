package com.example.jobaggregator.domain;

import static org.assertj.core.api.Assertions.assertThat;

import com.example.jobaggregator.domain.feed.FeedRecord;
import com.example.jobaggregator.domain.feed.FeedRecordType;
import java.util.List;
import java.util.UUID;
import org.junit.jupiter.api.Test;

class ContractBlockTest {

    @Test
    void shouldCreateArticleWithFullComponents() {
        // Given: Components for an Article
        ExternalId oid = new ExternalId("OID-1");
        Ikac ikac = new Ikac("IKAC-1");
        Condition cond = new Condition("COND-1", "VAL-1");
        Account acc = new Account("BILL", "BIC1", "IBAN1", "RIB1");
        Role rol = new Role("TIT", "BRAND", "PER", "HOLDER1", "IKPI1");
        Tarif tar = new Tarif("TAR1", "001", "2026-01-01", "2026-01-01", "EUR", "N", "F", "M", "T", "T", "10", "100", "1", "1", "U", "N", null, "N", null);
        Advantage avt = new Advantage("AVT1", "2026-01-01", "2026-12-31", "COD1", "10", "EUR");

        // Act: Construct article
        Article article = new Article(1, List.of(oid), List.of(ikac), List.of(cond), List.of(acc), List.of(rol), List.of(tar), List.of(avt));

        // Assert: All fields and child collections are preserved
        assertThat(article.sequentialIndex()).isEqualTo(1);
        assertThat(article.externalIds()).containsExactly(oid);
        assertThat(article.ikacs()).containsExactly(ikac);
        assertThat(article.conditions()).containsExactly(cond);
        assertThat(article.accounts()).containsExactly(acc);
        assertThat(article.roles()).containsExactly(rol);
        assertThat(article.tarifs()).containsExactly(tar);
        assertThat(article.advantages()).containsExactly(avt);
    }

    @Test
    void shouldCreateArticleWithDefaultEmptyListsUsingSingleArgConstructor() {
        // Act: Construct article with sequentialIndex only
        Article article = new Article(42);

        // Assert: Index is preserved and all collections are empty non-null lists
        assertThat(article.sequentialIndex()).isEqualTo(42);
        assertThat(article.externalIds()).isEmpty();
        assertThat(article.ikacs()).isEmpty();
        assertThat(article.conditions()).isEmpty();
        assertThat(article.accounts()).isEmpty();
        assertThat(article.roles()).isEmpty();
        assertThat(article.tarifs()).isEmpty();
        assertThat(article.advantages()).isEmpty();
    }

    @Test
    void shouldCreateMarketedObjectWithFullComponents() {
        // Given: Components for an OM
        ExternalId oid = new ExternalId("OID-OM");
        Role rol = new Role("TIT", "BRAND", "PER", "HOLDER1", "IKPI1");
        Tarif tar = new Tarif("TAR-OM", "001", "2026-01-01", "2026-01-01", "EUR", "N", "F", "M", "T", "T", "10", "100", "1", "1", "U", "N", null, "N", null);
        Advantage avt = new Advantage("AVT-OM", "2026-01-01", "2026-12-31", "COD1", "10", "EUR");
        Article art = new Article(1);

        // Act: Construct MarketedObject
        MarketedObject om = new MarketedObject("OM-001", "000012345678901234",
                List.of(oid), List.of(rol), List.of(tar), List.of(avt), List.of(art));

        // Assert: Fields and child lists
        assertThat(om.omId()).isEqualTo("OM-001");
        assertThat(om.businessRelationship()).isEqualTo("000012345678901234");
        assertThat(om.externalIds()).containsExactly(oid);
        assertThat(om.roles()).containsExactly(rol);
        assertThat(om.tarifs()).containsExactly(tar);
        assertThat(om.advantages()).containsExactly(avt);
        assertThat(om.articles()).containsExactly(art);
    }

    @Test
    void shouldCreateMarketedObjectWithDefaultEmptyListsUsingTwoArgConstructor() {
        // Act: Construct OM with omId and businessRelationship only
        MarketedObject om = new MarketedObject("OM-002", "REL-002");

        // Assert: IDs are set and child lists are empty
        assertThat(om.omId()).isEqualTo("OM-002");
        assertThat(om.businessRelationship()).isEqualTo("REL-002");
        assertThat(om.externalIds()).isEmpty();
        assertThat(om.roles()).isEmpty();
        assertThat(om.tarifs()).isEmpty();
        assertThat(om.advantages()).isEmpty();
        assertThat(om.articles()).isEmpty();
    }

    @Test
    void shouldCreateContractBlockAndReturnRawRecords() {
        // Given: Raw feed records
        FeedRecord r1 = new FeedRecord(1, FeedRecordType.CTR, "CTR;EUR;16", List.of("CTR", "EUR", "16"));
        FeedRecord r2 = new FeedRecord(2, FeedRecordType.ACC, "ACC;BILL;BNPA", List.of("ACC", "BILL", "BNPA"));
        UUID contractId = UUID.randomUUID();

        // Act: Construct ContractBlock
        ContractBlock block = new ContractBlock(
                contractId,
                List.of(r1, r2),
                null,
                List.of(new Account("BILL", "BIC", "IBAN", "RIB")),
                List.of(),
                List.of(),
                List.of(),
                List.of(),
                List.of(new MarketedObject("OM-1", "REL-1"))
        );

        // Assert: Root aggregate properties
        assertThat(block.id()).isEqualTo(contractId);
        assertThat(block.records()).containsExactly(r1, r2);
        assertThat(block.rawRecords()).containsExactly("CTR;EUR;16", "ACC;BILL;BNPA");
        assertThat(block.accounts()).hasSize(1);
        assertThat(block.marketedObjects()).hasSize(1);
    }

    @Test
    void shouldReturnEmptyListWhenRawRecordsOnNullRecords() {
        // Given: A contract block with null records list
        ContractBlock block = new ContractBlock(UUID.randomUUID(), null, null, List.of(), List.of(), List.of(), List.of(), List.of(), List.of());

        // Act & Assert: rawRecords returns empty list without NPE
        assertThat(block.rawRecords()).isEmpty();
    }
}
