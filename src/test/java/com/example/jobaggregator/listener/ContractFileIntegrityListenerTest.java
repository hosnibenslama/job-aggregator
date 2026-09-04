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
    void rule4Violation_noContractsRead_returnsFailed() {
        StepExecution stepExecution = stepExecution(0, null);

        assertThat(listener.afterStep(stepExecution)).isEqualTo(ExitStatus.FAILED);
    }

    @Test
    void rule3Violation_mismatchBetweenExpectedAndActual_returnsFailed() {
        StepExecution stepExecution = stepExecution(5, 10);

        assertThat(listener.afterStep(stepExecution)).isEqualTo(ExitStatus.FAILED);
    }

    @Test
    void rule3Success_matchBetweenExpectedAndActual_returnsCompleted() {
        StepExecution stepExecution = stepExecution(100, 100);

        assertThat(listener.afterStep(stepExecution)).isEqualTo(ExitStatus.COMPLETED);
    }

    @Test
    void rule3Skipped_trlLineNotEncountered_returnsCompleted() {
        StepExecution stepExecution = stepExecution(50, null);

        assertThat(listener.afterStep(stepExecution)).isEqualTo(ExitStatus.COMPLETED);
    }

    @Test
    void singleContractWithMatchingTrl_returnsCompleted() {
        StepExecution stepExecution = stepExecution(1, 1);

        assertThat(listener.afterStep(stepExecution)).isEqualTo(ExitStatus.COMPLETED);
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
