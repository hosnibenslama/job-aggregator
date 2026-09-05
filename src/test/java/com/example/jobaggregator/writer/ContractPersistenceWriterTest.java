package com.example.jobaggregator.writer;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;

import com.example.jobaggregator.domain.ContractBlock;
import com.example.jobaggregator.domain.feed.LineType;
import com.example.jobaggregator.domain.feed.ParsedLine;
import com.example.jobaggregator.persistence.ContractEntity;
import com.example.jobaggregator.persistence.ContractEntityRepository;
import java.util.List;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.springframework.batch.infrastructure.item.Chunk;

class ContractPersistenceWriterTest {

    @Test
    void shouldMapContractBlockToNormalizedEntitiesAndSave() {
        // Given: A repository mock and a writer
        ContractEntityRepository repository = mock(ContractEntityRepository.class);
        ContractPersistenceWriter writer = new ContractPersistenceWriter(repository);

        // A ContractBlock with multiple child line types
        ParsedLine ctr = new ParsedLine(1, LineType.CTR, "CTR", List.of(
                "CTR", "EUR", "16", "003", "ouDist", "ouMgmt", "addr1", "rel1",
                "2026-01-01T00:00:00.000000Z", "MENSUELLE", "2026-01-01",
                "0123456789abcdef", "0123456789abcdef", "user1", "001", "003"));
        ParsedLine acc = new ParsedLine(2, LineType.ACC, "ACC", List.of(
                "ACC", "BILL", "BNPAFRPP", "FR76300040219600000167638828", "RIB123"));
        ParsedLine om = new ParsedLine(3, LineType.OM, "OM", List.of(
                "OM", "OM-001", "BR-001"));
        ParsedLine art = new ParsedLine(4, LineType.ART, "ART", List.of(
                "ART", "1"));
        ParsedLine ikac = new ParsedLine(5, LineType.IKAC, "IKAC", List.of(
                "IKAC", "IKAC-VAL-1"));
        ParsedLine cond = new ParsedLine(6, LineType.COND, "COND", List.of(
                "COND", "COND-1", "VAL-1"));
        ParsedLine tar = new ParsedLine(7, LineType.TAR, "TAR", List.of(
                "TAR", "TAR-1", "001", "2026-01-01", "2026-01-01", "EUR"));

        ContractBlock block = new ContractBlock(List.of(ctr, acc, om, art, ikac, cond, tar));

        // Act: Write chunk
        writer.write(Chunk.of(block));

        // Assert: SaveAll was invoked with populated entities
        @SuppressWarnings("unchecked")
        ArgumentCaptor<List<ContractEntity>> captor = ArgumentCaptor.forClass(List.class);
        verify(repository).saveAll(captor.capture());

        List<ContractEntity> saved = captor.getValue();
        assertThat(saved).hasSize(1);

        ContractEntity entity = saved.getFirst();
        assertThat(entity.getId()).isEqualTo(block.id());
        assertThat(entity.getDevise()).isEqualTo("EUR");
        assertThat(entity.getAccounts()).hasSize(1);
        assertThat(entity.getAccounts().iterator().next().getIban()).isEqualTo("FR76300040219600000167638828");
        assertThat(entity.getMarketedObjects()).hasSize(1);
        assertThat(entity.getArticles()).hasSize(1);
        assertThat(entity.getIkacLines()).hasSize(1);
        assertThat(entity.getConditions()).hasSize(1);
        assertThat(entity.getTarifs()).hasSize(1);
        assertThat(entity.getTarifs().iterator().next().getIdOpraTarif()).isEqualTo("TAR-1");
    }
}
