package com.example.jobaggregator.listener;

import com.example.jobaggregator.reader.ContractBlockReader;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.batch.core.ExitStatus;
import org.springframework.batch.core.listener.StepExecutionListener;
import org.springframework.batch.core.step.StepExecution;
import org.springframework.batch.infrastructure.item.ExecutionContext;
import org.springframework.stereotype.Component;

/**
 * Enforces file-level control rules (specification section 8) after the step completes:
 *
 * <ul>
 *   <li><b>Rule 4 — CTR mandatory:</b> at least one contract must have been read.</li>
 *   <li><b>Rule 3 — TRL counter:</b> the {@code NBCTR} field of the {@code TRL} line must
 *       equal the actual number of contracts returned by the reader.</li>
 * </ul>
 *
 * <p>A violation marks the step (and therefore the job) as {@link ExitStatus#FAILED}.</p>
 */
@Component
public class ContractFileIntegrityListener implements StepExecutionListener {

    private static final Logger log = LoggerFactory.getLogger(ContractFileIntegrityListener.class);

    @Override
    public ExitStatus afterStep(StepExecution stepExecution) {
        long actualCount = stepExecution.getReadCount();

        // Rule 4: at least one CTR
        if (actualCount == 0) {
            log.error("Rule 4 violation: no CTR contract found in the file");
            return ExitStatus.FAILED;
        }

        // Rule 3: TRL NBCTR matches actual read count
        ExecutionContext ctx = stepExecution.getExecutionContext();
        if (ctx.containsKey(ContractBlockReader.KEY_EXPECTED_CONTRACT_COUNT)) {
            long expected = ctx.getInt(ContractBlockReader.KEY_EXPECTED_CONTRACT_COUNT);
            if (expected != actualCount) {
                log.error("Rule 3 violation: TRL declares {} contracts but {} were read",
                        expected, actualCount);
                return ExitStatus.FAILED;
            }
            log.info("Rule 3 OK: TRL NBCTR={} matches actual contract count", expected);
        } else {
            log.warn("Rule 3 skipped: TRL line was not encountered (file may be missing trailer)");
        }

        return ExitStatus.COMPLETED;
    }
}
