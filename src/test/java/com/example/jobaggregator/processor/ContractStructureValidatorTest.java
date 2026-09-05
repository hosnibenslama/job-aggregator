package com.example.jobaggregator.processor;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;

import com.example.jobaggregator.domain.ContractBlock;
import com.example.jobaggregator.domain.feed.FeedRecordType;
import com.example.jobaggregator.domain.feed.FeedRecord;
import com.example.jobaggregator.writer.ContractRejectWriter;
import java.io.IOException;
import java.util.List;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class ContractStructureValidatorTest {

    @Mock
    private ContractRejectWriter rejectWriter;

    @InjectMocks
    private ContractStructureValidator validator;

    @Test
    void shouldReturnContractUnchangedWhenStructureIsValid() throws Exception {
        // Given: A valid, complete contract containing CTR, ACC, OM, and ART
        ContractBlock contract = new ContractBlock(List.of(
                createFeedRecord(1, FeedRecordType.CTR, "CTR"),
                createFeedRecord(2, FeedRecordType.ACC, "ACC", "BILL"),
                createFeedRecord(3, FeedRecordType.OM, "OM", "OM-001"),
                createFeedRecord(4, FeedRecordType.ART, "ART", "1")
        ));

        // Act: Process the contract through structure validator
        ContractBlock result = validator.process(contract);

        // Assert: The original contract is returned unaltered and reject writer is not invoked
        assertThat(result).isSameAs(contract);
        verifyNoInteractions(rejectWriter);
    }

    @Test
    void shouldFilterContractAndCallRejectWriterWhenStructureIsInvalid() throws Exception {
        // Given: An invalid contract missing mandatory OM and ART lines
        ContractBlock contract = new ContractBlock(List.of(
                createFeedRecord(1, FeedRecordType.CTR, "CTR"),
                createFeedRecord(2, FeedRecordType.ACC, "ACC", "BILL")
        ));

        // Act: Process the contract through structure validator
        ContractBlock result = validator.process(contract);

        // Assert: Result is null (filtered from writer) and reject writer is called
        assertThat(result).isNull();
        verify(rejectWriter).reject(eq(contract), any(String.class));
    }

    @Test
    void shouldCallRejectWriterWithDescriptiveReasonWhenContractIsMissingRequiredLine() throws Exception {
        // Given: An invalid contract missing the required ART line
        ContractBlock contract = new ContractBlock(List.of(
                createFeedRecord(1, FeedRecordType.CTR, "CTR"),
                createFeedRecord(2, FeedRecordType.ACC, "ACC", "BILL"),
                createFeedRecord(3, FeedRecordType.OM, "OM", "OM-001")
        ));

        // Act: Process the contract through structure validator
        validator.process(contract);

        // Assert: Reject writer is called with specific descriptive failure reason
        verify(rejectWriter).reject(eq(contract), eq("Invalid contract input: line=1, contractId=<unknown>, reason=A contract must contain at least one ART"));
    }

    @Test
    void shouldPropagateIoExceptionWhenRejectWriterFails() throws Exception {
        // Given: An invalid contract and a reject writer that fails with IOException
        ContractBlock contract = new ContractBlock(List.of(
                createFeedRecord(1, FeedRecordType.CTR, "CTR"),
                createFeedRecord(2, FeedRecordType.ACC, "ACC", "BILL")
        ));
        doThrow(new IOException("Disk full")).when(rejectWriter).reject(any(ContractBlock.class), any(String.class));

        // Act & Assert: IOException is propagated directly when validator attempts to reject
        assertThatThrownBy(() -> validator.process(contract))
                .isInstanceOf(IOException.class)
                .hasMessage("Disk full");
    }

    @Test
    void shouldFilterContractAndCallRejectWriterWhenLineSequenceIsInvalid() throws Exception {
        // Given: A contract with an invalid hierarchy (IKAC appearing before ART)
        ContractBlock contract = new ContractBlock(List.of(
                createFeedRecord(1, FeedRecordType.CTR, "CTR"),
                createFeedRecord(2, FeedRecordType.ACC, "ACC", "BILL"),
                createFeedRecord(3, FeedRecordType.OM, "OM", "OM-001"),
                createFeedRecord(4, FeedRecordType.IKAC, "IKAC", "value")
        ));

        // Act: Process the contract through structure validator
        ContractBlock result = validator.process(contract);

        // Assert: Result is null and reject writer is called
        assertThat(result).isNull();
        verify(rejectWriter).reject(eq(contract), any(String.class));
    }

    @Test
    void shouldFilterContractAndCallRejectWriterWhenContractContainsUnknownFeedRecordType() throws Exception {
        // Given: A contract containing an UNKNOWN poison line
        ContractBlock contract = new ContractBlock(List.of(
                createFeedRecord(1, FeedRecordType.UNKNOWN, "CTTR"),
                createFeedRecord(2, FeedRecordType.ACC, "ACC", "BILL"),
                createFeedRecord(3, FeedRecordType.OM, "OM", "OM-001"),
                createFeedRecord(4, FeedRecordType.ART, "ART", "1")
        ));

        // Act: Process the contract through structure validator
        ContractBlock result = validator.process(contract);

        // Assert: Result is null and reject writer is called
        assertThat(result).isNull();
        verify(rejectWriter).reject(eq(contract), any(String.class));
    }

    private FeedRecord createFeedRecord(long number, FeedRecordType type, String... fields) {
        return new FeedRecord(number, type, String.join(";", fields), List.of(fields));
    }
}
