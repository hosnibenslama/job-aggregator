package com.bnpparibas.dsibddf.ap23992.modeldevente.batch.injector.domain;

import com.bnpparibas.dsibddf.ap23992.modeldevente.batch.injector.domain.feed.FeedRecord;
import java.util.List;
import java.util.UUID;

/**
 * Immutable domain aggregate root representing a single contract.
 *
 * <p>This record is a pure data carrier — it does not perform any assembly or
 * validation logic. Construction from raw {@link FeedRecord}s is done by
 * {@link com.bnpparibas.dsibddf.ap23992.modeldevente.batch.injector.reader.ContractBlockAssembler}.
 */
public record ContractBlock(
        UUID id,
        List<FeedRecord> records,
        ContractHeader header,
        List<Account> accounts,
        List<Role> roles,
        List<Offer> offers,
        List<Tarif> tarifs,
        List<Advantage> advantages,
        List<MarketedObject> marketedObjects) {

    /**
     * Returns the raw text strings of all feed records in this contract block.
     */
    public List<String> rawRecords() {
        return records != null ? records.stream().map(FeedRecord::raw).toList() : List.of();
    }
}
