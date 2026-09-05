package com.example.jobaggregator.domain;

import com.example.jobaggregator.domain.feed.FeedRecord;
import com.example.jobaggregator.reader.ContractBlockAssembler;
import java.util.List;
import java.util.UUID;

/**
 * Represents a normalized domain aggregate root constituting a single contract.
 * Holds a unique generated UUID, typed child components, and raw feed records.
 */
public record ContractBlock(
        UUID id,
        List<FeedRecord> records,
        ContractHeader header,
        List<Account> accounts,          // Contract-level accounts
        List<Role> roles,                // Contract-level roles
        List<Offer> offers,              // Contract-level offers
        List<Tarif> tarifs,              // Contract-level tarifs
        List<Advantage> advantages,      // Contract-level advantages
        List<MarketedObject> marketedObjects) {

    public ContractBlock(List<FeedRecord> records) {
        this(UUID.randomUUID(), records);
    }

    public ContractBlock(UUID id, List<FeedRecord> records) {
        this(
                id,
                records,
                ContractBlockAssembler.assemble(id, records)
        );
    }

    private ContractBlock(UUID id, List<FeedRecord> records, ContractBlock assembled) {
        this(
                id,
                records,
                assembled.header(),
                assembled.accounts(),
                assembled.roles(),
                assembled.offers(),
                assembled.tarifs(),
                assembled.advantages(),
                assembled.marketedObjects()
        );
    }

    /**
     * Returns the raw text strings of all feed records in this contract block.
     */
    public List<String> rawRecords() {
        return records != null ? records.stream().map(FeedRecord::raw).toList() : List.of();
    }
}
