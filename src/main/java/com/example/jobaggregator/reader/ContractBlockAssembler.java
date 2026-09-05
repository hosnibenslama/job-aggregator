package com.example.jobaggregator.reader;

import com.example.jobaggregator.domain.ContractBlock;
import com.example.jobaggregator.domain.feed.FeedRecordType;
import com.example.jobaggregator.domain.feed.FeedRecord;
import com.example.jobaggregator.error.ContractFormatException;
import java.util.ArrayList;
import java.util.EnumSet;
import java.util.List;
import java.util.Set;

/**
 * Assembles and validates the structural sequencing of a single {@link ContractBlock} from ordered feed records.
 *
 * <p>Responsibilities (Single Responsibility Principle):
 * <ul>
 *   <li>Enforces record sequencing grammar (which record types may follow a given record type)</li>
 *   <li>Enforces structural prerequisites (e.g. OID requires OM, TAR requires ART)</li>
 *   <li>Enforces mandatory block content (at least one ACC, OM, ART per contract)</li>
 * </ul>
 * Field-level validation is delegated to {@link com.example.jobaggregator.reader.validator.LineFieldValidator}
 * implementations in {@link ContractLineMapper}.
 */
public final class ContractBlockAssembler {

    private final List<FeedRecord> records = new ArrayList<>();
    private FeedRecordType previous;
    private boolean hasAccount;
    private boolean hasMarketedObject;
    private boolean hasArticle;

    public ContractBlockAssembler(FeedRecord ctr) {
        if (ctr.type() != FeedRecordType.CTR) {
            throw new ContractFormatException(ctr.lineNumber(), null, "A contract must begin with CTR");
        }

        records.add(ctr);
        previous = FeedRecordType.CTR;
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

        switch (record.type()) {
            case ACC -> hasAccount = true;
            case OM -> hasMarketedObject = true;
            case ART -> hasArticle = true;
            default -> {}
        }
    }

    public ContractBlock build() {
        if (!hasAccount) {
            throw error(records.getFirst(), "A contract must contain at least one ACC");
        }
        if (!hasMarketedObject) {
            throw error(records.getFirst(), "A contract must contain at least one OM");
        }
        if (!hasArticle) {
            throw error(records.getFirst(), "A contract must contain at least one ART");
        }

        return new ContractBlock(List.copyOf(records));
    }

    private void validatePrerequisites(FeedRecord record) {
        if (record.type() == FeedRecordType.OID && !hasMarketedObject) {
            throw error(record, "OID requires a preceding OM");
        }
        if (Set.of(FeedRecordType.IKAC, FeedRecordType.COND, FeedRecordType.TAR, FeedRecordType.AVT).contains(record.type())
                && !hasArticle) {
            throw error(record, record.type() + " requires a preceding ART");
        }
    }

    /**
     * Record-ordering grammar: defines the allowed successor record types according to the specification.
     */
    private Set<FeedRecordType> allowedAfter(FeedRecordType type) {
        return switch (type) {
            // After Contract root or Offer: can transition to account, commercial role, offer, or product
            case CTR, OFF -> EnumSet.of(
                    FeedRecordType.ACC,
                    FeedRecordType.ROL,
                    FeedRecordType.OFF,
                    FeedRecordType.OM);

            // After Commercial Role: can transition to account, role, offer, product, operation detail, or article
            case ROL -> EnumSet.of(
                    FeedRecordType.ACC,
                    FeedRecordType.ROL,
                    FeedRecordType.OFF,
                    FeedRecordType.OM,
                    FeedRecordType.OID,
                    FeedRecordType.ART);

            // After Account: can transition to further accounts, roles, offers, products, articles, conditions, tarifs
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

            // After Marketed Product (OM): can transition to operation detail, commercial role, or article
            case OM -> EnumSet.of(
                    FeedRecordType.OID,
                    FeedRecordType.ROL,
                    FeedRecordType.ART);

            // After Operation Detail (OID): transitions within article/product scope
            case OID -> EnumSet.of(
                    FeedRecordType.ROL,
                    FeedRecordType.ART,
                    FeedRecordType.IKAC,
                    FeedRecordType.COND,
                    FeedRecordType.ACC,
                    FeedRecordType.TAR,
                    FeedRecordType.AVT,
                    FeedRecordType.OID);

            // After Article: transitions to tarif, conditions, next article, or account
            case ART -> EnumSet.of(
                    FeedRecordType.OID,
                    FeedRecordType.IKAC,
                    FeedRecordType.COND,
                    FeedRecordType.ACC,
                    FeedRecordType.TAR,
                    FeedRecordType.AVT,
                    FeedRecordType.ART,
                    FeedRecordType.ROL);

            // After IKAC: conditions, account, tarifs, or next article
            case IKAC -> EnumSet.of(
                    FeedRecordType.COND,
                    FeedRecordType.ACC,
                    FeedRecordType.TAR,
                    FeedRecordType.AVT,
                    FeedRecordType.ART,
                    FeedRecordType.ROL,
                    FeedRecordType.OID);

            // After COND: further conditions, tarifs, or accounts
            case COND -> EnumSet.of(
                    FeedRecordType.ACC,
                    FeedRecordType.TAR,
                    FeedRecordType.AVT,
                    FeedRecordType.ART,
                    FeedRecordType.ROL,
                    FeedRecordType.OID,
                    FeedRecordType.COND);

            // After Tarif (TAR): advantages, next article, role, account, or OID
            case TAR -> EnumSet.of(
                    FeedRecordType.AVT,
                    FeedRecordType.ART,
                    FeedRecordType.ROL,
                    FeedRecordType.ACC,
                    FeedRecordType.OID);

            // After Avantage (AVT): next article, role, account, or OID
            case AVT -> EnumSet.of(
                    FeedRecordType.ART,
                    FeedRecordType.ROL,
                    FeedRecordType.ACC,
                    FeedRecordType.OID);

            default -> throw new IllegalStateException("No grammar rule for FeedRecordType: " + type);
        };
    }

    private ContractFormatException error(FeedRecord record, String reason) {
        return new ContractFormatException(record.lineNumber(), null, reason);
    }
}
