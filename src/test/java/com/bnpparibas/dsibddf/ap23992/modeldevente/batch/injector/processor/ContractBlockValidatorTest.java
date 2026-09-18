package com.bnpparibas.dsibddf.ap23992.modeldevente.batch.injector.processor;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;

import com.bnpparibas.dsibddf.ap23992.modeldevente.batch.injector.domain.ContractBlock;
import com.bnpparibas.dsibddf.ap23992.modeldevente.batch.injector.domain.feed.FeedRecordType;
import com.bnpparibas.dsibddf.ap23992.modeldevente.batch.injector.domain.feed.FeedRecord;
import com.bnpparibas.dsibddf.ap23992.modeldevente.batch.injector.reader.ContractBlockAssembler;
import com.bnpparibas.dsibddf.ap23992.modeldevente.batch.injector.writer.ContractRejectionPort;
import java.io.IOException;
import java.util.List;
import java.util.UUID;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class ContractBlockValidatorTest {

    @Mock
    private ContractRejectionPort rejectionPort;

    @InjectMocks
    private ContractBlockValidator validator;

    @Test
    void shouldReturnValidatedBlockWhenStructureIsValid() throws Exception {
        // Given: A valid, complete contract containing CTR, ACC, OM, and ART
        ContractBlock contract = assembleBlock(
                createFeedRecord(1, FeedRecordType.CTR, "CTR"),
                createFeedRecord(2, FeedRecordType.ACC, "ACC", "BILL"),
                createFeedRecord(3, FeedRecordType.OM, "OM", "OM-001"),
                createFeedRecord(4, FeedRecordType.ART, "ART", "1")
        );

        // Act: Process the contract through block validator
        ContractBlock result = validator.process(contract);

        // Assert: A validated contract is returned and rejection port is not invoked
        assertThat(result).isNotNull();
        assertThat(result.id()).isEqualTo(contract.id());
        verifyNoInteractions(rejectionPort);
    }

    @Test
    void shouldFilterContractAndCallRejectionPortWhenStructureIsInvalid() throws Exception {
        // Given: An invalid contract missing mandatory OM and ART lines
        ContractBlock contract = assembleBlock(
                createFeedRecord(1, FeedRecordType.CTR, "CTR"),
                createFeedRecord(2, FeedRecordType.ACC, "ACC", "BILL")
        );

        // Act: Process the contract through block validator
        ContractBlock result = validator.process(contract);

        // Assert: Result is null (filtered from writer) and rejection port is called
        assertThat(result).isNull();
        verify(rejectionPort).reject(eq(contract), any(String.class));
    }

    @Test
    void shouldCallRejectionPortWithDescriptiveReasonWhenContractIsMissingRequiredLine() throws Exception {
        // Given: An invalid contract missing the required ART line
        ContractBlock contract = assembleBlock(
                createFeedRecord(1, FeedRecordType.CTR, "CTR"),
                createFeedRecord(2, FeedRecordType.ACC, "ACC", "BILL"),
                createFeedRecord(3, FeedRecordType.OM, "OM", "OM-001")
        );

        // Act: Process the contract through block validator
        validator.process(contract);

        // Assert: Rejection port is called with specific descriptive failure reason
        verify(rejectionPort).reject(eq(contract), eq("Invalid contract input: line=1, contractId=<unknown>, reason=A contract must contain at least one ART"));
    }

    @Test
    void shouldPropagateIoExceptionWhenRejectionPortFails() throws Exception {
        // Given: An invalid contract and a rejection port that fails with IOException
        ContractBlock contract = assembleBlock(
                createFeedRecord(1, FeedRecordType.CTR, "CTR"),
                createFeedRecord(2, FeedRecordType.ACC, "ACC", "BILL")
        );
        doThrow(new IOException("Disk full")).when(rejectionPort).reject(any(ContractBlock.class), any(String.class));

        // Act & Assert: IOException is propagated directly when validator attempts to reject
        assertThatThrownBy(() -> validator.process(contract))
                .isInstanceOf(IOException.class)
                .hasMessage("Disk full");
    }

    @Test
    void shouldFilterContractAndCallRejectionPortWhenLineSequenceIsInvalid() throws Exception {
        // Given: A contract with an invalid hierarchy (IKAC appearing before ART)
        ContractBlock contract = assembleBlock(
                createFeedRecord(1, FeedRecordType.CTR, "CTR"),
                createFeedRecord(2, FeedRecordType.ACC, "ACC", "BILL"),
                createFeedRecord(3, FeedRecordType.OM, "OM", "OM-001"),
                createFeedRecord(4, FeedRecordType.IKAC, "IKAC", "value")
        );

        // Act: Process the contract through block validator
        ContractBlock result = validator.process(contract);

        // Assert: Result is null and rejection port is called
        assertThat(result).isNull();
        verify(rejectionPort).reject(eq(contract), any(String.class));
    }

    @Test
    void shouldFilterContractAndCallRejectionPortWhenContractContainsUnknownFeedRecordType() throws Exception {
        // Given: A contract containing an UNKNOWN poison line
        ContractBlock contract = assembleBlock(
                createFeedRecord(1, FeedRecordType.UNKNOWN, "CTTR"),
                createFeedRecord(2, FeedRecordType.ACC, "ACC", "BILL"),
                createFeedRecord(3, FeedRecordType.OM, "OM", "OM-001"),
                createFeedRecord(4, FeedRecordType.ART, "ART", "1")
        );

        // Act: Process the contract through block validator
        ContractBlock result = validator.process(contract);

        // Assert: Result is null and rejection port is called
        assertThat(result).isNull();
        verify(rejectionPort).reject(eq(contract), any(String.class));
    }

    private FeedRecord createFeedRecord(long number, FeedRecordType type, String... fields) {
        return new FeedRecord(number, type, String.join(";", fields), List.of(fields));
    }

    private ContractBlock assembleBlock(FeedRecord... records) {
        return ContractBlockAssembler.assemble(UUID.randomUUID(), List.of(records));
    }
}
