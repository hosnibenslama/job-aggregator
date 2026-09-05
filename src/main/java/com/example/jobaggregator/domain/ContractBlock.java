package com.example.jobaggregator.domain;

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
        CtrLine ctrLine,
        List<AccLine> accounts,
        List<RolLine> roles,
        List<OffLine> offers,
        List<OmLine> operations,
        List<OidLine> externalIds,
        List<ArtLine> articles,
        List<IkacLine> ikacLines,
        List<CondLine> conditions,
        List<TarLine> tariffs,
        List<AvtLine> advantages) {

    public ContractBlock(List<ParsedLine> lines) {
        this(UUID.randomUUID(), lines);
    }

    public ContractBlock(UUID id, List<ParsedLine> lines) {
        this(
                id,
                lines,
                (!lines.isEmpty() && lines.getFirst().type() == LineType.CTR)
                        ? CtrLine.from(lines.getFirst()) : null,
                filter(lines, LineType.ACC, AccLine::from),
                filter(lines, LineType.ROL, RolLine::from),
                filter(lines, LineType.OFF, OffLine::from),
                filter(lines, LineType.OM, OmLine::from),
                filter(lines, LineType.OID, OidLine::from),
                filter(lines, LineType.ART, ArtLine::from),
                filter(lines, LineType.IKAC, IkacLine::from),
                filter(lines, LineType.COND, CondLine::from),
                filter(lines, LineType.TAR, TarLine::from),
                filter(lines, LineType.AVT, AvtLine::from)
        );
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
