package com.example.jobaggregator.writer;

import static org.assertj.core.api.Assertions.assertThat;
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

    @Test
    void shouldPersistMultiLevelRecordsWithExactLevelDiscriminators() throws Exception {
        // Given: Mock JdbcTemplate capturing SQL statements and parameters
        JdbcTemplate jdbcTemplate = mock(JdbcTemplate.class);
        List<String> executedSqls = new java.util.ArrayList<>();
        List<Object[]> executedParams = new java.util.ArrayList<>();

        org.mockito.Mockito.doAnswer(invocation -> {
            executedSqls.add(invocation.getArgument(0));
            Object[] args = new Object[invocation.getArguments().length - 1];
            System.arraycopy(invocation.getArguments(), 1, args, 0, args.length);
            executedParams.add(args);
            return 1;
        }).when(jdbcTemplate).update(anyString(), any(Object[].class));

        // Mock KeyHolder to return generated IDs for OM (100) and Article (200)
        final java.util.concurrent.atomic.AtomicLong keySequence = new java.util.concurrent.atomic.AtomicLong(100);
        org.mockito.Mockito.doAnswer(invocation -> {
            KeyHolder kh = invocation.getArgument(1);
            kh.getKeyList().add(java.util.Map.of("id", keySequence.getAndAdd(100)));
            return 1;
        }).when(jdbcTemplate).update(any(PreparedStatementCreator.class), any(KeyHolder.class));

        ContractPersistenceWriter writer = new ContractPersistenceWriter(jdbcTemplate);

        // Hierarchical feed records with TAR, AVT, ROL across Contract, OM, and Article levels
        FeedRecord ctr = new FeedRecord(1, FeedRecordType.CTR, "CTR", List.of("CTR", "EUR", "16"));
        FeedRecord accCtr = new FeedRecord(2, FeedRecordType.ACC, "ACC", List.of("ACC", "BILL-CTR", "BNPA", "FR76", "RIB1"));
        FeedRecord rolCtr = new FeedRecord(3, FeedRecordType.ROL, "ROL", List.of("ROL", "ROL-CTR", "BR", "SC", "HOLD1", "IKPI1"));
        FeedRecord tarCtr = new FeedRecord(4, FeedRecordType.TAR, "TAR", List.of("TAR", "TAR-CTR", "001", "2026-01-01", "2026-01-01", "EUR"));
        FeedRecord avtCtr = new FeedRecord(5, FeedRecordType.AVT, "AVT", List.of("AVT", "AVT-CTR", "2026-01-01", "2026-12-31", "C1", "10", "EUR"));

        FeedRecord om = new FeedRecord(6, FeedRecordType.OM, "OM", List.of("OM", "OM-1", "REL-1"));
        FeedRecord oidOm = new FeedRecord(7, FeedRecordType.OID, "OID", List.of("OID", "OID-OM"));
        FeedRecord rolOm = new FeedRecord(8, FeedRecordType.ROL, "ROL", List.of("ROL", "ROL-OM", "BR", "SC", "HOLD2", "IKPI2"));
        FeedRecord tarOm = new FeedRecord(9, FeedRecordType.TAR, "TAR", List.of("TAR", "TAR-OM", "001", "2026-01-01", "2026-01-01", "EUR"));
        FeedRecord avtOm = new FeedRecord(10, FeedRecordType.AVT, "AVT", List.of("AVT", "AVT-OM", "2026-01-01", "2026-12-31", "C2", "20", "EUR"));

        FeedRecord art = new FeedRecord(11, FeedRecordType.ART, "ART", List.of("ART", "1"));
        FeedRecord oidArt = new FeedRecord(12, FeedRecordType.OID, "OID", List.of("OID", "OID-ART"));
        FeedRecord accArt = new FeedRecord(13, FeedRecordType.ACC, "ACC", List.of("ACC", "BILL-ART", "BNPA", "FR76", "RIB2"));
        FeedRecord rolArt = new FeedRecord(14, FeedRecordType.ROL, "ROL", List.of("ROL", "ROL-ART", "BR", "SC", "HOLD3", "IKPI3"));
        FeedRecord tarArt = new FeedRecord(15, FeedRecordType.TAR, "TAR", List.of("TAR", "TAR-ART", "001", "2026-01-01", "2026-01-01", "EUR"));
        FeedRecord avtArt = new FeedRecord(16, FeedRecordType.AVT, "AVT", List.of("AVT", "AVT-ART", "2026-01-01", "2026-12-31", "C3", "30", "EUR"));

        ContractBlock block = new ContractBlock(List.of(
                ctr, accCtr, rolCtr, tarCtr, avtCtr,
                om, oidOm, rolOm, tarOm, avtOm,
                art, oidArt, accArt, rolArt, tarArt, avtArt));

        // Act: Persist the block
        writer.write(Chunk.of(block));

        // Assert: Check executed SQL statements and verify level discriminators
        assertThat(executedSqls).anyMatch(sql -> sql.contains("contract_tarifs"));
        assertThat(executedSqls).anyMatch(sql -> sql.contains("contract_advantages"));
        assertThat(executedSqls).anyMatch(sql -> sql.contains("contract_roles"));
        assertThat(executedSqls).anyMatch(sql -> sql.contains("contract_external_ids"));
        assertThat(executedSqls).anyMatch(sql -> sql.contains("contract_accounts"));

        // Verify level parameters in executed SQL
        List<Object> allFlatParams = executedParams.stream()
                .flatMap(java.util.Arrays::stream)
                .toList();

        assertThat(allFlatParams).contains("CONTRACT", "OM", "ARTICLE");
    }

    @Test
    void shouldHandleEmptyContractWithoutExceptions() {
        // Given: A contract block with minimal mandatory structure
        JdbcTemplate jdbcTemplate = mock(JdbcTemplate.class);
        ContractPersistenceWriter writer = new ContractPersistenceWriter(jdbcTemplate);

        FeedRecord ctr = new FeedRecord(1, FeedRecordType.CTR, "CTR", List.of("CTR", "EUR", "16"));
        FeedRecord acc = new FeedRecord(2, FeedRecordType.ACC, "ACC", List.of("ACC", "BILL", "BNPA", "FR76", "RIB1"));
        FeedRecord om = new FeedRecord(3, FeedRecordType.OM, "OM", List.of("OM", "OM-1", "REL-1"));
        FeedRecord art = new FeedRecord(4, FeedRecordType.ART, "ART", List.of("ART", "1"));

        ContractBlock block = new ContractBlock(List.of(ctr, acc, om, art));

        // Act & Assert: Writing executes cleanly
        assertThatCode(() -> writer.write(Chunk.of(block))).doesNotThrowAnyException();
    }
}
