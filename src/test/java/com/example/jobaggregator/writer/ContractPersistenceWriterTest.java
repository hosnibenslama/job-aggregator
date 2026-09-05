package com.example.jobaggregator.writer;

import static org.assertj.core.api.Assertions.assertThatCode;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.atLeastOnce;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;

import com.example.jobaggregator.domain.ContractBlock;
import com.example.jobaggregator.domain.feed.FeedRecord;
import com.example.jobaggregator.domain.feed.FeedRecordType;
import java.util.List;
import org.junit.jupiter.api.Test;
import org.springframework.batch.infrastructure.item.Chunk;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.core.PreparedStatementCreator;
import org.springframework.jdbc.support.KeyHolder;

class ContractPersistenceWriterTest {

    @Test
    void shouldPersistContractBlockAndHierarchicalChildren() {
        // Given: A mock JdbcTemplate and a writer
        JdbcTemplate jdbcTemplate = mock(JdbcTemplate.class);
        ContractPersistenceWriter writer = new ContractPersistenceWriter(jdbcTemplate);

        // A ContractBlock with multiple hierarchical child lines
        FeedRecord ctr = new FeedRecord(1, FeedRecordType.CTR, "CTR", List.of(
                "CTR", "EUR", "16", "003", "ouDist", "ouMgmt", "addr1", "rel1",
                "2026-01-01T00:00:00.000000Z", "MENSUELLE", "2026-01-01",
                "0123456789abcdef", "0123456789abcdef", "user1", "001", "003"));
        FeedRecord acc = new FeedRecord(2, FeedRecordType.ACC, "ACC", List.of(
                "ACC", "BILL", "BNPAFRPP", "FR76300040219600000167638828", "RIB123"));
        FeedRecord om = new FeedRecord(3, FeedRecordType.OM, "OM", List.of(
                "OM", "OM-001", "BR-001"));
        FeedRecord art = new FeedRecord(4, FeedRecordType.ART, "ART", List.of(
                "ART", "1"));
        FeedRecord ikac = new FeedRecord(5, FeedRecordType.IKAC, "IKAC", List.of(
                "IKAC", "IKAC-VAL-1"));
        FeedRecord cond = new FeedRecord(6, FeedRecordType.COND, "COND", List.of(
                "COND", "COND-1", "VAL-1"));
        FeedRecord tar = new FeedRecord(7, FeedRecordType.TAR, "TAR", List.of(
                "TAR", "TAR-1", "001", "2026-01-01", "2026-01-01", "EUR"));

        ContractBlock block = new ContractBlock(List.of(ctr, acc, om, art, ikac, cond, tar));

        // Act & Assert: Writing the chunk executes without error and updates database tables
        assertThatCode(() -> writer.write(Chunk.of(block))).doesNotThrowAnyException();

        verify(jdbcTemplate, atLeastOnce()).update(anyString(), any(Object[].class));
        verify(jdbcTemplate, atLeastOnce()).update(any(PreparedStatementCreator.class), any(KeyHolder.class));
    }
}
