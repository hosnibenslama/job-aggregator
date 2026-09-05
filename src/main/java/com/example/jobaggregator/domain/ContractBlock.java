package com.example.jobaggregator.domain;

import com.example.jobaggregator.domain.feed.ContractFeedMapper;
import com.example.jobaggregator.domain.feed.LineType;
import com.example.jobaggregator.domain.feed.ParsedLine;
import java.util.List;
import java.util.UUID;
import java.util.function.Function;

/**
 * Represents a normalized domain aggregate root constituting a single contract.
 * Holds a unique generated UUID, typed child components, and raw line strings for rejection.
 */
public record ContractBlock(
        UUID id,
        List<ParsedLine> lines,
        ContractHeader header,
        List<Account> accounts,
        List<Role> roles,
        List<Offer> offers,
        List<MarketedObject> marketedObjects,
        List<ExternalId> externalIds,
        List<Article> articles,
        List<Ikac> ikacAttributes,
        List<Condition> conditions,
        List<Tarif> tarifs,
        List<Advantage> advantages) {

    public ContractBlock(List<ParsedLine> lines) {
        this(UUID.randomUUID(), lines);
    }

    public ContractBlock(UUID id, List<ParsedLine> lines) {
        this(
                id,
                lines,
                (!lines.isEmpty() && lines.getFirst().type() == LineType.CTR)
                        ? ContractFeedMapper.toHeader(lines.getFirst()) : null,
                filter(lines, LineType.ACC, ContractFeedMapper::toAccount),
                filter(lines, LineType.ROL, ContractFeedMapper::toRole),
                filter(lines, LineType.OFF, ContractFeedMapper::toOffer),
                filter(lines, LineType.OM, ContractFeedMapper::toMarketedObject),
                filter(lines, LineType.OID, ContractFeedMapper::toExternalId),
                filter(lines, LineType.ART, ContractFeedMapper::toArticle),
                filter(lines, LineType.IKAC, ContractFeedMapper::toIkac),
                filter(lines, LineType.COND, ContractFeedMapper::toCondition),
                filter(lines, LineType.TAR, ContractFeedMapper::toTarif),
                filter(lines, LineType.AVT, ContractFeedMapper::toAdvantage)
        );
    }

    /**
     * Backward-compatible accessor for the contract header.
     */
    public ContractHeader ctrLine() {
        return header;
    }

    /**
     * Backward-compatible accessor for IKAC attributes.
     */
    public List<Ikac> ikacLines() {
        return ikacAttributes;
    }

    /**
     * Backward-compatible accessor for marketed objects.
     */
    public List<MarketedObject> operations() {
        return marketedObjects;
    }

    public List<String> rawLines() {
        return lines != null ? lines.stream().map(ParsedLine::raw).toList() : List.of();
    }

    private static <T> List<T> filter(List<ParsedLine> lines, LineType type, Function<ParsedLine, T> mapper) {
        if (lines == null) {
            return List.of();
        }
        return lines.stream()
                .filter(l -> l.type() == type)
                .map(mapper)
                .toList();
    }
}
