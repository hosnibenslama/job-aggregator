package com.example.jobaggregator.domain;

import com.example.jobaggregator.domain.feed.ContractFeedMapper;
import com.example.jobaggregator.domain.feed.FeedRecord;
import com.example.jobaggregator.domain.feed.FeedRecordType;
import java.util.List;
import java.util.UUID;
import java.util.function.Function;

/**
 * Represents a normalized domain aggregate root constituting a single contract.
 * Holds a unique generated UUID, typed child components, and raw feed records.
 */
public record ContractBlock(
        UUID id,
        List<FeedRecord> records,
        ContractHeader header,
        List<Account> accounts,
        List<Role> roles,
        List<Offer> offers,
        List<MarketedObject> marketedObjects,
        List<ExternalId> externalIds,
        List<Article> articles,
        List<Ikac> ikacs,
        List<Condition> conditions,
        List<Tarif> tarifs,
        List<Advantage> advantages) {

    public ContractBlock(List<FeedRecord> records) {
        this(UUID.randomUUID(), records);
    }

    public ContractBlock(UUID id, List<FeedRecord> records) {
        this(
                id,
                records,
                (!records.isEmpty() && records.getFirst().type() == FeedRecordType.CTR)
                        ? ContractFeedMapper.toHeader(records.getFirst()) : null,
                filter(records, FeedRecordType.ACC, ContractFeedMapper::toAccount),
                filter(records, FeedRecordType.ROL, ContractFeedMapper::toRole),
                filter(records, FeedRecordType.OFF, ContractFeedMapper::toOffer),
                filter(records, FeedRecordType.OM, ContractFeedMapper::toMarketedObject),
                filter(records, FeedRecordType.OID, ContractFeedMapper::toExternalId),
                filter(records, FeedRecordType.ART, ContractFeedMapper::toArticle),
                filter(records, FeedRecordType.IKAC, ContractFeedMapper::toIkac),
                filter(records, FeedRecordType.COND, ContractFeedMapper::toCondition),
                filter(records, FeedRecordType.TAR, ContractFeedMapper::toTarif),
                filter(records, FeedRecordType.AVT, ContractFeedMapper::toAdvantage)
        );
    }

    /**
     * Returns the raw text strings of all feed records in this contract block.
     */
    public List<String> rawRecords() {
        return records != null ? records.stream().map(FeedRecord::raw).toList() : List.of();
    }

    private static <T> List<T> filter(List<FeedRecord> records, FeedRecordType type, Function<FeedRecord, T> mapper) {
        if (records == null) {
            return List.of();
        }
        return records.stream()
                .filter(l -> l.type() == type)
                .map(mapper)
                .toList();
    }
}
