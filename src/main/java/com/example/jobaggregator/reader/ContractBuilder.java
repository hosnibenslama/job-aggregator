package com.example.jobaggregator.reader;

import com.example.jobaggregator.domain.BusinessLine;
import com.example.jobaggregator.domain.Contract;
import com.example.jobaggregator.domain.LineType;
import com.example.jobaggregator.error.ContractFormatException;
import java.util.ArrayList;
import java.util.EnumSet;
import java.util.List;
import java.util.Set;

public final class ContractBuilder {

    private final List<BusinessLine> lines = new ArrayList<>();
    private LineType previous;
    private boolean hasAccount;
    private boolean hasOperation;
    private boolean hasArticle;

    public ContractBuilder(BusinessLine ctr) {
        if (ctr.type() != LineType.CTR) {
            throw new ContractFormatException(ctr.lineNumber(), null, "A contract must begin with CTR");
        }

        lines.add(ctr);
        previous = LineType.CTR;
    }

    public void accept(BusinessLine line) {
        Set<LineType> allowed = allowedAfter(previous);
        if (!allowed.contains(line.type())) {
            throw error(line, "Unexpected " + line.type()
                    + " after " + previous + "; expected one of " + allowed);
        }

        validatePrerequisites(line);
        validateLineFields(line);
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

        return new Contract(
                lines.getFirst().lineNumber(),
                lines.getLast().lineNumber(),
                List.copyOf(lines));
    }

    private void validatePrerequisites(BusinessLine line) {
        if (line.type() == LineType.OID && !hasOperation) {
            throw error(line, "OID requires a preceding OM");
        }
        if (Set.of(LineType.IKAC, LineType.COND, LineType.TAR, LineType.AVT).contains(line.type())
                && !hasArticle) {
            throw error(line, line.type() + " requires a preceding ART");
        }
    }

    private void validateLineFields(BusinessLine line) {
        switch (line.type()) {
            case ACC -> {
                String account = line.field(1);
                if (account == null || account.isBlank()) {
                    throw error(line, "ACC field 1 (account) is required");
                }
            }
            case OM -> {
                String operation = line.field(1);
                if (operation == null || operation.isBlank()) {
                    throw error(line, "OM field 1 (operation) is required");
                }
            }
            case ART -> {
                String articleCode = line.field(1);
                if (articleCode == null || articleCode.isBlank()) {
                    throw error(line, "ART field 1 (articleCode) is required");
                }
            }
            default -> {}
        }
    }

    private Set<LineType> allowedAfter(LineType type) {
        return switch (type) {
            case CTR, OFF -> EnumSet.of(LineType.ACC, LineType.ROL, LineType.OFF, LineType.OM);
            case ROL -> EnumSet.of(LineType.ACC, LineType.ROL, LineType.OFF, LineType.OM, LineType.OID, LineType.ART);
            case ACC -> EnumSet.of(LineType.ACC, LineType.ROL, LineType.OFF, LineType.OM,
                    LineType.ART, LineType.IKAC, LineType.COND, LineType.TAR, LineType.AVT, LineType.OID);
            case OM -> EnumSet.of(LineType.OID, LineType.ROL, LineType.ART);
            case OID -> EnumSet.of(LineType.ROL, LineType.ART, LineType.IKAC, LineType.COND, LineType.ACC, LineType.TAR, LineType.AVT, LineType.OID);
            case ART -> EnumSet.of(
                    LineType.OID, LineType.IKAC, LineType.COND, LineType.ACC,
                    LineType.TAR, LineType.AVT, LineType.ART, LineType.ROL);
            case IKAC -> EnumSet.of(LineType.COND, LineType.ACC, LineType.TAR,
                    LineType.AVT, LineType.ART, LineType.ROL, LineType.OID);
            case COND -> EnumSet.of(LineType.ACC, LineType.TAR, LineType.AVT,
                    LineType.ART, LineType.ROL, LineType.OID, LineType.COND);
            case TAR -> EnumSet.of(LineType.AVT, LineType.ART, LineType.ROL, LineType.ACC, LineType.OID);
            case AVT -> EnumSet.of(LineType.ART, LineType.ROL, LineType.ACC, LineType.OID);
            default -> EnumSet.noneOf(LineType.class);
        };
    }

    private ContractFormatException error(BusinessLine line, String reason) {
        return new ContractFormatException(line.lineNumber(), null, reason);
    }
}
