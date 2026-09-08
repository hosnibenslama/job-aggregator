package com.bnpparibas.dsibddf.ap23992.modeldevente.batch.injector.reader;

import com.bnpparibas.dsibddf.ap23992.modeldevente.batch.injector.domain.Account;
import com.bnpparibas.dsibddf.ap23992.modeldevente.batch.injector.domain.Advantage;
import com.bnpparibas.dsibddf.ap23992.modeldevente.batch.injector.domain.ContractBlock;
import com.bnpparibas.dsibddf.ap23992.modeldevente.batch.injector.domain.ContractHeader;
import com.bnpparibas.dsibddf.ap23992.modeldevente.batch.injector.domain.ExternalId;
import com.bnpparibas.dsibddf.ap23992.modeldevente.batch.injector.domain.MarketedObject;
import com.bnpparibas.dsibddf.ap23992.modeldevente.batch.injector.domain.Offer;
import com.bnpparibas.dsibddf.ap23992.modeldevente.batch.injector.domain.Role;
import com.bnpparibas.dsibddf.ap23992.modeldevente.batch.injector.domain.Tarif;
import com.bnpparibas.dsibddf.ap23992.modeldevente.batch.injector.domain.feed.ContractFeedMapper;
import com.bnpparibas.dsibddf.ap23992.modeldevente.batch.injector.domain.feed.FeedRecord;
import com.bnpparibas.dsibddf.ap23992.modeldevente.batch.injector.domain.feed.FeedRecordType;
import com.bnpparibas.dsibddf.ap23992.modeldevente.batch.injector.error.ContractFormatException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

/**
 * Assembles and validates the structural sequencing of a hierarchical {@link ContractBlock}
 * from ordered feed records according to the specification (Section 3 and Section 9).
 */
public final class ContractBlockAssembler {

    private static final Logger log = LoggerFactory.getLogger(ContractBlockAssembler.class);

    // -----------------------------------------------------------------------
    // Instance state
    // -----------------------------------------------------------------------

    private final UUID contractId;
    private final List<FeedRecord> records = new ArrayList<>();
    private FeedRecordType previousRecordType;

    private ContractHeader header;
    private final List<Account> contractAccounts = new ArrayList<>();
    private final List<Role> contractRoles = new ArrayList<>();
    private final List<Offer> contractOffers = new ArrayList<>();
    private final List<Tarif> contractTarifs = new ArrayList<>();
    private final List<Advantage> contractAdvantages = new ArrayList<>();
    private final List<MarketedObjectBuilder> marketedObjectBuilders = new ArrayList<>();

    private MarketedObjectBuilder currentMarketedObjectBuilder;
    private ArticleBuilder currentArticleBuilder;

    public ContractBlockAssembler(FeedRecord contractRecord) {
        this(UUID.randomUUID(), contractRecord);
    }

    public ContractBlockAssembler(UUID contractId, FeedRecord contractRecord) {
        this.contractId = contractId != null ? contractId : UUID.randomUUID();
        if (contractRecord == null || contractRecord.type() != FeedRecordType.CTR) {
            long lineNumber = contractRecord != null ? contractRecord.lineNumber() : 0;
            throw new ContractFormatException(lineNumber, null, "A contract must begin with CTR");
        }

        this.records.add(contractRecord);
        this.header = ContractFeedMapper.toHeader(contractRecord);
        this.previousRecordType = FeedRecordType.CTR;
    }

    // -----------------------------------------------------------------------
    // Record acceptance
    // -----------------------------------------------------------------------

    public void accept(FeedRecord record) {
        if (!ContractSequencingRules.isAllowed(previousRecordType, record.type())) {
            throw createFormatException(record, "Unexpected " + record.type()
                    + " after " + previousRecordType + "; expected one of "
                    + ContractSequencingRules.getAllowedSuccessors(previousRecordType));
        }

        validatePrerequisites(record);
        records.add(record);
        previousRecordType = record.type();

        routeRecord(record);
    }

    private void routeRecord(FeedRecord record) {
        switch (record.type()) {
            case ACC -> {
                Account account = ContractFeedMapper.toAccount(record);
                if (currentArticleBuilder != null) {
                    currentArticleBuilder.accounts.add(account);
                } else {
                    contractAccounts.add(account);
                }
            }
            case ROL -> {
                Role role = ContractFeedMapper.toRole(record);
                if (currentArticleBuilder != null) {
                    currentArticleBuilder.roles.add(role);
                } else if (currentMarketedObjectBuilder != null) {
                    currentMarketedObjectBuilder.roles.add(role);
                } else {
                    contractRoles.add(role);
                }
            }
            case OFF -> contractOffers.add(ContractFeedMapper.toOffer(record));
            case OM -> {
                currentMarketedObjectBuilder = new MarketedObjectBuilder(record);
                currentArticleBuilder = null;
                marketedObjectBuilders.add(currentMarketedObjectBuilder);
            }
            case OID -> {
                ExternalId externalId = ContractFeedMapper.toExternalId(record);
                if (currentArticleBuilder != null) {
                    currentArticleBuilder.externalIds.add(externalId);
                } else if (currentMarketedObjectBuilder != null) {
                    currentMarketedObjectBuilder.externalIds.add(externalId);
                }
            }
            case ART -> {
                currentArticleBuilder = new ArticleBuilder(record);
                currentMarketedObjectBuilder.articleBuilders.add(currentArticleBuilder);
            }
            case IKAC -> {
                if (currentArticleBuilder != null) {
                    currentArticleBuilder.ikacs.add(ContractFeedMapper.toIkac(record));
                }
            }
            case COND -> {
                if (currentArticleBuilder != null) {
                    currentArticleBuilder.conditions.add(ContractFeedMapper.toCondition(record));
                }
            }
            case TAR -> {
                Tarif tarif = ContractFeedMapper.toTarif(record);
                if (currentArticleBuilder != null) {
                    currentArticleBuilder.tarifs.add(tarif);
                } else if (currentMarketedObjectBuilder != null) {
                    currentMarketedObjectBuilder.tarifs.add(tarif);
                } else {
                    contractTarifs.add(tarif);
                }
            }
            case AVT -> {
                Advantage advantage = ContractFeedMapper.toAdvantage(record);
                if (currentArticleBuilder != null) {
                    currentArticleBuilder.advantages.add(advantage);
                } else if (currentMarketedObjectBuilder != null) {
                    currentMarketedObjectBuilder.advantages.add(advantage);
                } else {
                    contractAdvantages.add(advantage);
                }
            }
            default -> {}
        }
    }

    // -----------------------------------------------------------------------
    // Build
    // -----------------------------------------------------------------------

    public ContractBlock build() {
        boolean hasAccount = !contractAccounts.isEmpty()
                || marketedObjectBuilders.stream()
                        .flatMap(marketedObjectBuilder -> marketedObjectBuilder.articleBuilders.stream())
                        .anyMatch(articleBuilder -> !articleBuilder.accounts.isEmpty());
        if (!hasAccount) {
            throw createFormatException(records.get(0), "A contract must contain at least one ACC");
        }
        if (marketedObjectBuilders.isEmpty()) {
            throw createFormatException(records.get(0), "A contract must contain at least one OM");
        }
        boolean hasArticle = marketedObjectBuilders.stream()
                .anyMatch(marketedObjectBuilder -> !marketedObjectBuilder.articleBuilders.isEmpty());
        if (!hasArticle) {
            throw createFormatException(records.get(0), "A contract must contain at least one ART");
        }

        return toContractBlock();
    }

    /**
     * Builds the ContractBlock without enforcing mandatory content checks (used for lenient construction).
     */
    public ContractBlock toContractBlock() {
        List<MarketedObject> marketedObjects = marketedObjectBuilders.stream()
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
    public static ContractBlock assemble(UUID contractId, List<FeedRecord> records) {
        if (records == null || records.isEmpty() || records.get(0).type() != FeedRecordType.CTR) {
            return new ContractBlock(
                    contractId != null ? contractId : UUID.randomUUID(),
                    records != null ? List.copyOf(records) : List.of(),
                    null,
                    List.of(), List.of(), List.of(), List.of(), List.of(), List.of()
            );
        }

        ContractBlockAssembler assembler = new ContractBlockAssembler(contractId, records.get(0));
        for (int i = 1; i < records.size(); i++) {
            FeedRecord record = records.get(i);
            try {
                assembler.accept(record);
            } catch (ContractFormatException formatException) {
                log.debug("Lenient assembly: skipping {} at line {} — {}",
                        record.type(), record.lineNumber(), formatException.getReason());
                assembler.records.add(record);
            }
        }
        return assembler.toContractBlock();
    }

    // -----------------------------------------------------------------------
    // Validation helpers
    // -----------------------------------------------------------------------

    private void validatePrerequisites(FeedRecord record) {
        if (record.type() == FeedRecordType.OID && currentMarketedObjectBuilder == null && currentArticleBuilder == null) {
            throw createFormatException(record, "OID requires a preceding OM or ART");
        }
        if ((record.type() == FeedRecordType.IKAC || record.type() == FeedRecordType.COND) && currentArticleBuilder == null) {
            throw createFormatException(record, record.type() + " requires a preceding ART");
        }
    }

    private ContractFormatException createFormatException(FeedRecord record, String reason) {
        long lineNumber = record != null ? record.lineNumber() : 0;
        return new ContractFormatException(lineNumber, null, reason);
    }
}
