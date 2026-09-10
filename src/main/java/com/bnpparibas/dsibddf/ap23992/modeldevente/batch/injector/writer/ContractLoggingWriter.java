package com.bnpparibas.dsibddf.ap23992.modeldevente.batch.injector.writer;

import com.bnpparibas.dsibddf.ap23992.modeldevente.batch.injector.domain.ContractBlock;
import java.util.concurrent.atomic.AtomicLong;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.batch.infrastructure.item.Chunk;
import org.springframework.batch.infrastructure.item.ItemWriter;
import org.springframework.stereotype.Component;

/**
 * Non-persisting ItemWriter that logs valid contract blocks without persisting them to a database.
 * Used when running the batch import in validation/rejection-only or ingestion-testing mode.
 */
@Component
public class ContractLoggingWriter implements ItemWriter<ContractBlock> {

    private static final Logger log = LoggerFactory.getLogger(ContractLoggingWriter.class);
    private final AtomicLong totalProcessedCount = new AtomicLong(0);

    @Override
    public void write(Chunk<? extends ContractBlock> chunk) {
        for (ContractBlock contract : chunk) {
            String contractId = contract.id() != null ? contract.id().toString() : "UNKNOWN";
            int omCount = contract.marketedObjects() != null ? contract.marketedObjects().size() : 0;
            int accountCount = contract.accounts() != null ? contract.accounts().size() : 0;
            long currentTotal = totalProcessedCount.incrementAndGet();

            log.info("Valid contract #{} processed (no-db mode): [id={}, accounts={}, marketedObjects={}]",
                    currentTotal, contractId, accountCount, omCount);
        }
    }

    public long getTotalProcessedCount() {
        return totalProcessedCount.get();
    }

    public void reset() {
        totalProcessedCount.set(0);
    }
}
