package com.bnpparibas.dsibddf.ap23992.modeldevente.batch.injector.writer;

import static org.assertj.core.api.Assertions.assertThat;

import com.bnpparibas.dsibddf.ap23992.modeldevente.batch.injector.domain.Account;
import com.bnpparibas.dsibddf.ap23992.modeldevente.batch.injector.domain.ContractBlock;
import com.bnpparibas.dsibddf.ap23992.modeldevente.batch.injector.domain.ContractHeader;
import java.util.List;
import java.util.UUID;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.batch.infrastructure.item.Chunk;

class ContractLoggingWriterTest {

    private ContractLoggingWriter writer;

    @BeforeEach
    void setUp() {
        writer = new ContractLoggingWriter();
    }

    @Test
    void shouldLogAndCountValidContractsWithoutError() {
        UUID id1 = UUID.randomUUID();
        UUID id2 = UUID.randomUUID();

        ContractBlock b1 = new ContractBlock(
                id1, List.of(), null, List.of(), List.of(), List.of(), List.of(), List.of(), List.of());
        ContractBlock b2 = new ContractBlock(
                id2, List.of(), null, List.of(), List.of(), List.of(), List.of(), List.of(), List.of());

        Chunk<ContractBlock> chunk = new Chunk<>(List.of(b1, b2));
        writer.write(chunk);

        assertThat(writer.getTotalProcessedCount()).isEqualTo(2);
    }

    @Test
    void shouldHandleNullIdAndFieldsGracefully() {
        ContractBlock b1 = new ContractBlock(
                null, null, null, null, null, null, null, null, null);
        Chunk<ContractBlock> chunk = new Chunk<>(List.of(b1));

        writer.write(chunk);

        assertThat(writer.getTotalProcessedCount()).isEqualTo(1);
    }
}
