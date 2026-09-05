package com.example.jobaggregator.reader;

import com.example.jobaggregator.domain.Account;
import com.example.jobaggregator.domain.Advantage;
import com.example.jobaggregator.domain.Article;
import com.example.jobaggregator.domain.Condition;
import com.example.jobaggregator.domain.ContractBlock;
import com.example.jobaggregator.domain.ContractHeader;
import com.example.jobaggregator.domain.ExternalId;
import com.example.jobaggregator.domain.Ikac;
import com.example.jobaggregator.domain.MarketedObject;
import com.example.jobaggregator.domain.Offer;
import com.example.jobaggregator.domain.Role;
import com.example.jobaggregator.domain.Tarif;
import com.example.jobaggregator.domain.feed.ContractFeedMapper;
import com.example.jobaggregator.domain.feed.FeedRecord;
import com.example.jobaggregator.domain.feed.FeedRecordType;
import com.example.jobaggregator.error.ContractFormatException;
import java.util.ArrayList;
import java.util.EnumSet;
import java.util.List;
import java.util.Set;
import java.util.UUID;

/**
 * Assembles and validates the structural sequencing of a hierarchical {@link ContractBlock}
 * from ordered feed records according to the specification (Section 3 and Section 9).
 */
public final class ContractBlockAssembler {

    private final UUID contractId;
    private final List<FeedRecord> records = new ArrayList<>();
    private FeedRecordType previous;

    private ContractHeader header;
    private final List<Account> contractAccounts = new ArrayList<>();
    private final List<Role> contractRoles = new ArrayList<>();
    private final List<Offer> contractOffers = new ArrayList<>();
    private final List<Tarif> contractTarifs = new ArrayList<>();
    private final List<Advantage> contractAdvantages = new ArrayList<>();
    private final List<MarketedObjectBuilder> omBuilders = new ArrayList<>();

    private MarketedObjectBuilder currentOm;
    private ArticleBuilder currentArticle;

    public ContractBlockAssembler(FeedRecord ctr) {
        this(UUID.randomUUID(), ctr);
    }

    public ContractBlockAssembler(UUID contractId, FeedRecord ctr) {
        this.contractId = contractId != null ? contractId : UUID.randomUUID();
        if (ctr == null || ctr.type() != FeedRecordType.CTR) {
            long lineNum = ctr != null ? ctr.lineNumber() : 0;
            throw new ContractFormatException(lineNum, null, "A contract must begin with CTR");
        }

        this.records.add(ctr);
        this.header = ContractFeedMapper.toHeader(ctr);
        this.previous = FeedRecordType.CTR;
    }

    public void accept(FeedRecord record) {
        Set<FeedRecordType> allowed = allowedAfter(previous);
        if (!allowed.contains(record.type())) {
            throw error(record, "Unexpected " + record.type()
                    + " after " + previous + "; expected one of " + allowed);
        }

        validatePrerequisites(record);
        records.add(record);
        previous = record.type();

        routeRecord(record);
    }

    private void routeRecord(FeedRecord record) {
        switch (record.type()) {
            case ACC -> {
                Account acc = ContractFeedMapper.toAccount(record);
                if (currentArticle != null) {
                    currentArticle.accounts.add(acc);
                } else {
                    contractAccounts.add(acc);
                }
            }
            case ROL -> {
                Role rol = ContractFeedMapper.toRole(record);
                if (currentArticle != null) {
                    currentArticle.roles.add(rol);
                } else if (currentOm != null) {
                    currentOm.roles.add(rol);
                } else {
                    contractRoles.add(rol);
                }
            }
            case OFF -> contractOffers.add(ContractFeedMapper.toOffer(record));
            case OM -> {
                currentOm = new MarketedObjectBuilder(record);
                currentArticle = null;
                omBuilders.add(currentOm);
            }
            case OID -> {
                ExternalId oid = ContractFeedMapper.toExternalId(record);
                if (currentArticle != null) {
                    currentArticle.externalIds.add(oid);
                } else if (currentOm != null) {
                    currentOm.externalIds.add(oid);
                }
            }
            case ART -> {
                currentArticle = new ArticleBuilder(record);
                currentOm.articleBuilders.add(currentArticle);
            }
            case IKAC -> {
                if (currentArticle != null) {
                    currentArticle.ikacs.add(ContractFeedMapper.toIkac(record));
                }
            }
            case COND -> {
                if (currentArticle != null) {
                    currentArticle.conditions.add(ContractFeedMapper.toCondition(record));
                }
            }
            case TAR -> {
                Tarif tar = ContractFeedMapper.toTarif(record);
                if (currentArticle != null) {
                    currentArticle.tarifs.add(tar);
                } else if (currentOm != null) {
                    currentOm.tarifs.add(tar);
                } else {
                    contractTarifs.add(tar);
                }
            }
            case AVT -> {
                Advantage avt = ContractFeedMapper.toAdvantage(record);
                if (currentArticle != null) {
                    currentArticle.advantages.add(avt);
                } else if (currentOm != null) {
                    currentOm.advantages.add(avt);
                } else {
                    contractAdvantages.add(avt);
                }
            }
            default -> {}
        }
    }

    public ContractBlock build() {
        boolean hasAccount = !contractAccounts.isEmpty()
                || omBuilders.stream().flatMap(om -> om.articleBuilders.stream()).anyMatch(a -> !a.accounts.isEmpty());
        if (!hasAccount) {
            throw error(records.getFirst(), "A contract must contain at least one ACC");
        }
        if (omBuilders.isEmpty()) {
            throw error(records.getFirst(), "A contract must contain at least one OM");
        }
        boolean hasArticle = omBuilders.stream().anyMatch(om -> !om.articleBuilders.isEmpty());
        if (!hasArticle) {
            throw error(records.getFirst(), "A contract must contain at least one ART");
        }

        return toContractBlock();
    }

    /**
     * Builds the ContractBlock without enforcing mandatory content checks (used for lenient construction).
     */
    public ContractBlock toContractBlock() {
        List<MarketedObject> marketedObjects = omBuilders.stream()
                .map(MarketedObjectBuilder::build)
                .toList();

        return new ContractBlock(
                contractId,
                List.copyOf(records),
                header,
                List.copyOf(contractAccounts),
                List.copyOf(contractRoles),
                List.copyOf(contractOffers),
                List.copyOf(contractTarifs),
                List.copyOf(contractAdvantages),
                List.copyOf(marketedObjects)
        );
    }

    /**
     * Leniently assemble records into a hierarchical ContractBlock.
     */
    public static ContractBlock assemble(UUID id, List<FeedRecord> records) {
        if (records == null || records.isEmpty() || records.getFirst().type() != FeedRecordType.CTR) {
            return new ContractBlock(
                    id != null ? id : UUID.randomUUID(),
                    records != null ? List.copyOf(records) : List.of(),
                    null,
                    List.of(), List.of(), List.of(), List.of(), List.of(), List.of()
            );
        }

        ContractBlockAssembler assembler = new ContractBlockAssembler(id, records.getFirst());
        for (int i = 1; i < records.size(); i++) {
            FeedRecord rec = records.get(i);
            try {
                assembler.accept(rec);
            } catch (Exception ignored) {
                // In lenient assembly, collect raw record even if grammar fails
                assembler.records.add(rec);
            }
        }
        return assembler.toContractBlock();
    }

    private void validatePrerequisites(FeedRecord record) {
        if (record.type() == FeedRecordType.OID && currentOm == null && currentArticle == null) {
            throw error(record, "OID requires a preceding OM or ART");
        }
        if (Set.of(FeedRecordType.IKAC, FeedRecordType.COND).contains(record.type()) && currentArticle == null) {
            throw error(record, record.type() + " requires a preceding ART");
        }
    }

    /**
     * Record-ordering grammar: defines the allowed successor record types according to Section 9.
     */
    private Set<FeedRecordType> allowedAfter(FeedRecordType type) {
        return switch (type) {
            // After Contract root or Offer: can transition to account, commercial role, offer, tarif, advantage, or OM
            case CTR, OFF -> EnumSet.of(
                    FeedRecordType.ACC,
                    FeedRecordType.ROL,
                    FeedRecordType.OFF,
                    FeedRecordType.TAR,
                    FeedRecordType.AVT,
                    FeedRecordType.OM);

            // After Commercial Role: depends on current context
            case ROL -> EnumSet.of(
                    FeedRecordType.ACC,
                    FeedRecordType.ROL,
                    FeedRecordType.OFF,
                    FeedRecordType.OM,
                    FeedRecordType.OID,
                    FeedRecordType.ART,
                    FeedRecordType.TAR,
                    FeedRecordType.AVT);

            // After Account:
            case ACC -> EnumSet.of(
                    FeedRecordType.ACC,
                    FeedRecordType.ROL,
                    FeedRecordType.OFF,
                    FeedRecordType.OM,
                    FeedRecordType.ART,
                    FeedRecordType.IKAC,
                    FeedRecordType.COND,
                    FeedRecordType.TAR,
                    FeedRecordType.AVT,
                    FeedRecordType.OID);

            // After Marketed Product (OM): transitions to OID, ROL, TAR, AVT, or ART
            case OM -> EnumSet.of(
                    FeedRecordType.OID,
                    FeedRecordType.ROL,
                    FeedRecordType.TAR,
                    FeedRecordType.AVT,
                    FeedRecordType.ART);

            // After Operation Detail (OID):
            case OID -> EnumSet.of(
                    FeedRecordType.OID,
                    FeedRecordType.ROL,
                    FeedRecordType.ART,
                    FeedRecordType.IKAC,
                    FeedRecordType.COND,
                    FeedRecordType.ACC,
                    FeedRecordType.TAR,
                    FeedRecordType.AVT,
                    FeedRecordType.OM);

            // After Article:
            case ART -> EnumSet.of(
                    FeedRecordType.OID,
                    FeedRecordType.IKAC,
                    FeedRecordType.COND,
                    FeedRecordType.ACC,
                    FeedRecordType.ROL,
                    FeedRecordType.TAR,
                    FeedRecordType.AVT,
                    FeedRecordType.ART,
                    FeedRecordType.OM);

            // After IKAC:
            case IKAC -> EnumSet.of(
                    FeedRecordType.COND,
                    FeedRecordType.ACC,
                    FeedRecordType.ROL,
                    FeedRecordType.TAR,
                    FeedRecordType.AVT,
                    FeedRecordType.ART,
                    FeedRecordType.OM,
                    FeedRecordType.OID);

            // After COND:
            case COND -> EnumSet.of(
                    FeedRecordType.COND,
                    FeedRecordType.ACC,
                    FeedRecordType.ROL,
                    FeedRecordType.TAR,
                    FeedRecordType.AVT,
                    FeedRecordType.ART,
                    FeedRecordType.OM,
                    FeedRecordType.OID);

            // After Tarif (TAR):
            case TAR -> EnumSet.of(
                    FeedRecordType.TAR,
                    FeedRecordType.AVT,
                    FeedRecordType.ART,
                    FeedRecordType.OM,
                    FeedRecordType.ROL,
                    FeedRecordType.ACC,
                    FeedRecordType.OID);

            // After Avantage (AVT):
            case AVT -> EnumSet.of(
                    FeedRecordType.AVT,
                    FeedRecordType.ART,
                    FeedRecordType.OM,
                    FeedRecordType.ROL,
                    FeedRecordType.ACC,
                    FeedRecordType.OID);

            default -> throw new IllegalStateException("No grammar rule for FeedRecordType: " + type);
        };
    }

    private ContractFormatException error(FeedRecord record, String reason) {
        long lineNum = record != null ? record.lineNumber() : 0;
        return new ContractFormatException(lineNum, null, reason);
    }

    // -----------------------------------------------------------------------
    // Internal hierarchical builders
    // -----------------------------------------------------------------------

    private static class MarketedObjectBuilder {
        final FeedRecord record;
        final List<ExternalId> externalIds = new ArrayList<>();
        final List<Role> roles = new ArrayList<>();
        final List<Tarif> tarifs = new ArrayList<>();
        final List<Advantage> advantages = new ArrayList<>();
        final List<ArticleBuilder> articleBuilders = new ArrayList<>();

        MarketedObjectBuilder(FeedRecord record) {
            this.record = record;
        }

        MarketedObject build() {
            MarketedObject base = ContractFeedMapper.toMarketedObject(record);
            List<Article> articles = articleBuilders.stream()
                    .map(ArticleBuilder::build)
                    .toList();

            return new MarketedObject(
                    base.omId(),
                    base.businessRelationship(),
                    List.copyOf(externalIds),
                    List.copyOf(roles),
                    List.copyOf(tarifs),
                    List.copyOf(advantages),
                    List.copyOf(articles)
            );
        }
    }

    private static class ArticleBuilder {
        final FeedRecord record;
        final List<ExternalId> externalIds = new ArrayList<>();
        final List<Ikac> ikacs = new ArrayList<>();
        final List<Condition> conditions = new ArrayList<>();
        final List<Account> accounts = new ArrayList<>();
        final List<Role> roles = new ArrayList<>();
        final List<Tarif> tarifs = new ArrayList<>();
        final List<Advantage> advantages = new ArrayList<>();

        ArticleBuilder(FeedRecord record) {
            this.record = record;
        }

        Article build() {
            Article base = ContractFeedMapper.toArticle(record);
            return new Article(
                    base.sequentialIndex(),
                    List.copyOf(externalIds),
                    List.copyOf(ikacs),
                    List.copyOf(conditions),
                    List.copyOf(accounts),
                    List.copyOf(roles),
                    List.copyOf(tarifs),
                    List.copyOf(advantages)
            );
        }
    }
}
