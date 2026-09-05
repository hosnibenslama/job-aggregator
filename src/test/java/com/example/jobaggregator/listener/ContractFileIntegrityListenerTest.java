package com.example.jobaggregator.listener;

import static org.assertj.core.api.Assertions.assertThat;

import com.example.jobaggregator.reader.ContractBlockReader;
import org.junit.jupiter.api.Test;
import org.springframework.batch.core.ExitStatus;
import org.springframework.batch.core.step.StepExecution;
import org.springframework.batch.infrastructure.item.ExecutionContext;
import org.springframework.batch.test.MetaDataInstanceFactory;

class ContractFileIntegrityListenerTest {

    private final ContractFileIntegrityListener listener = new ContractFileIntegrityListener();

    @Test
    void shouldReturnFailedExitStatusWhenNoContractsWereRead() {
        // Given: Step execution where 0 contracts were read (Rule 4 violation)
        StepExecution stepExecution = stepExecution(0, null);

        // Act: Execute the file integrity listener after step completion
        ExitStatus exitStatus = listener.afterStep(stepExecution);

        // Assert: Step exit status is FAILED
        assertThat(exitStatus).isEqualTo(ExitStatus.FAILED);
    }

    @Test
    void shouldReturnFailedExitStatusWhenActualCountMismatchesTrailerExpectedCount() {
        // Given: Step execution where actual read count (5) does not match trailer count (10) (Rule 3 violation)
        StepExecution stepExecution = stepExecution(5, 10);

        // Act: Execute the file integrity listener after step completion
        ExitStatus exitStatus = listener.afterStep(stepExecution);

        // Assert: Step exit status is FAILED
        assertThat(exitStatus).isEqualTo(ExitStatus.FAILED);
    }

    @Test
    void shouldReturnCompletedExitStatusWhenActualCountMatchesTrailerExpectedCount() {
        // Given: Step execution where actual read count (100) matches expected trailer count (100) (Rule 3 satisfied)
        StepExecution stepExecution = stepExecution(100, 100);

        // Act: Execute the file integrity listener after step completion
        ExitStatus exitStatus = listener.afterStep(stepExecution);

        // Assert: Step exit status is COMPLETED
        assertThat(exitStatus).isEqualTo(ExitStatus.COMPLETED);
    }

    @Test
    void shouldReturnCompletedExitStatusWhenTrailerLineWasNotEncountered() {
        // Given: Step execution with valid read count (50) but no trailer count recorded in ExecutionContext
        StepExecution stepExecution = stepExecution(50, null);

        // Act: Execute the file integrity listener after step completion
        ExitStatus exitStatus = listener.afterStep(stepExecution);

        // Assert: Step exit status is COMPLETED with Rule 3 skipped
        assertThat(exitStatus).isEqualTo(ExitStatus.COMPLETED);
    }

    @Test
    void shouldReturnCompletedExitStatusForSingleContractWithMatchingTrailerCount() {
        // Given: Step execution with 1 contract read and trailer count of 1
        StepExecution stepExecution = stepExecution(1, 1);

        // Act: Execute the file integrity listener after step completion
        ExitStatus exitStatus = listener.afterStep(stepExecution);

        // Assert: Step exit status is COMPLETED
        assertThat(exitStatus).isEqualTo(ExitStatus.COMPLETED);
    }

    private StepExecution stepExecution(long readCount, Integer expectedContractCount) {
        ExecutionContext ctx = new ExecutionContext();
        if (expectedContractCount != null) {
            ctx.putInt(ContractBlockReader.KEY_EXPECTED_CONTRACT_COUNT, expectedContractCount);
        }

        StepExecution stepExecution = MetaDataInstanceFactory.createStepExecution(ctx);
        stepExecution.setReadCount(readCount);
        return stepExecution;
    }
}
