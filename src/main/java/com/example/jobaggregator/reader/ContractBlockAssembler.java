package com.example.jobaggregator.reader;

import com.example.jobaggregator.domain.Contract;
import com.example.jobaggregator.domain.LineType;
import com.example.jobaggregator.domain.ParsedLine;
import com.example.jobaggregator.error.ContractFormatException;
import java.util.ArrayList;
import java.util.EnumSet;
import java.util.List;
import java.util.Set;

/**
 * Assembles and validates the structural sequencing of a single {@link Contract} block from ordered lines.
 *
 * <p>Responsibilities (Single Responsibility Principle):
 * <ul>
 *   <li>Enforces line sequencing grammar (which line types may follow a given line type)</li>
 *   <li>Enforces structural prerequisites (e.g. OID requires OM, TAR requires ART)</li>
 *   <li>Enforces mandatory block content (at least one ACC, OM, ART per contract)</li>
 * </ul>
 * Field-level validation is delegated to {@link com.example.jobaggregator.reader.validator.LineFieldValidator}
 * implementations in {@link SemicolonLineParser}.
 */
public final class ContractBlockAssembler {

    private final List<ParsedLine> lines = new ArrayList<>();
    private LineType previous;
    private boolean hasAccount;
    private boolean hasOperation;
    private boolean hasArticle;

    public ContractBlockAssembler(ParsedLine ctr) {
        if (ctr.type() != LineType.CTR) {
            throw new ContractFormatException(ctr.lineNumber(), null, "A contract must begin with CTR");
        }

        lines.add(ctr);
        previous = LineType.CTR;
    }

    public void accept(ParsedLine line) {
        Set<LineType> allowed = allowedAfter(previous);
        if (!allowed.contains(line.type())) {
            throw error(line, "Unexpected " + line.type()
                    + " after " + previous + "; expected one of " + allowed);
        }

        validatePrerequisites(line);
        lines.add(line);
        previous = line.type();

        switch (line.type()) {
            case ACC -> hasAccount = true;
            case OM -> hasOperation = true;
            case ART -> hasArticle = true;
            default -> {}
        }
    }

    public Contract build() {
        if (!hasAccount) {
            throw error(lines.getFirst(), "A contract must contain at least one ACC");
        }
        if (!hasOperation) {
            throw error(lines.getFirst(), "A contract must contain at least one OM");
        }
        if (!hasArticle) {
            throw error(lines.getFirst(), "A contract must contain at least one ART");
        }

        return new Contract(List.copyOf(lines));
    }

    private void validatePrerequisites(ParsedLine line) {
        if (line.type() == LineType.OID && !hasOperation) {
            throw error(line, "OID requires a preceding OM");
        }
        if (Set.of(LineType.IKAC, LineType.COND, LineType.TAR, LineType.AVT).contains(line.type())
                && !hasArticle) {
            throw error(line, line.type() + " requires a preceding ART");
        }
    }

    /**
     * Line-ordering grammar: defines the allowed successor line types according to the specification.
     */
    private Set<LineType> allowedAfter(LineType type) {
        return switch (type) {
            // After Contract root or Offer: can transition to account, commercial role, offer, or product
            case CTR, OFF -> EnumSet.of(
                    LineType.ACC,
                    LineType.ROL,
                    LineType.OFF,
                    LineType.OM);

            // After Commercial Role: can transition to account, role, offer, product, operation detail, or article
            case ROL -> EnumSet.of(
                    LineType.ACC,
                    LineType.ROL,
                    LineType.OFF,
                    LineType.OM,
                    LineType.OID,
                    LineType.ART);

            // After Account: can transition to further accounts, roles, offers, products, articles, conditions, tariffs
            case ACC -> EnumSet.of(
                    LineType.ACC,
                    LineType.ROL,
                    LineType.OFF,
                    LineType.OM,
                    LineType.ART,
                    LineType.IKAC,
                    LineType.COND,
                    LineType.TAR,
                    LineType.AVT,
                    LineType.OID);

            // After Marketed Product (OM): can transition to operation detail, commercial role, or article
            case OM -> EnumSet.of(
                    LineType.OID,
                    LineType.ROL,
                    LineType.ART);

            // After Operation Detail (OID): transitions within article/product scope
            case OID -> EnumSet.of(
                    LineType.ROL,
                    LineType.ART,
                    LineType.IKAC,
                    LineType.COND,
                    LineType.ACC,
                    LineType.TAR,
                    LineType.AVT,
                    LineType.OID);

            // After Article: transitions to tariff, conditions, next article, or account
            case ART -> EnumSet.of(
                    LineType.OID,
                    LineType.IKAC,
                    LineType.COND,
                    LineType.ACC,
                    LineType.TAR,
                    LineType.AVT,
                    LineType.ART,
                    LineType.ROL);

            // After IKAC: conditions, account, tariffs, or next article
            case IKAC -> EnumSet.of(
                    LineType.COND,
                    LineType.ACC,
                    LineType.TAR,
                    LineType.AVT,
                    LineType.ART,
                    LineType.ROL,
                    LineType.OID);

            // After COND: further conditions, tariffs, or accounts
            case COND -> EnumSet.of(
                    LineType.ACC,
                    LineType.TAR,
                    LineType.AVT,
                    LineType.ART,
                    LineType.ROL,
                    LineType.OID,
                    LineType.COND);

            // After Tariff (TAR): advantages, next article, role, account, or OID
            case TAR -> EnumSet.of(
                    LineType.AVT,
                    LineType.ART,
                    LineType.ROL,
                    LineType.ACC,
                    LineType.OID);

            // After Avantage (AVT): next article, role, account, or OID
            case AVT -> EnumSet.of(
                    LineType.ART,
                    LineType.ROL,
                    LineType.ACC,
                    LineType.OID);

            default -> throw new IllegalStateException("No grammar rule for LineType: " + type);
        };
    }

    private ContractFormatException error(ParsedLine line, String reason) {
        return new ContractFormatException(line.lineNumber(), null, reason);
    }
}
