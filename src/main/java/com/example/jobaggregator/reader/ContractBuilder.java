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

    private final String contractId;
    private final List<BusinessLine> lines = new ArrayList<>();
    private LineType previous;
    private boolean hasAccount;
    private boolean hasOperation;
    private boolean hasArticle;

    public ContractBuilder(BusinessLine ctr) {
        if (ctr.type() != LineType.CTR) {
            throw new ContractFormatException(ctr.lineNumber(), null, "A contract must begin with CTR");
        }

        contractId = readContractId(ctr);
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
        lines.add(line);
        previous = line.type();

        switch (line.type()) {
            case ACC -> hasAccount = true;
            case OM -> hasOperation = true;
            case ART_N -> hasArticle = true;
            default -> {
            }
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
            throw error(lines.getFirst(), "A contract must contain at least one ART;N");
        }

        return new Contract(
                contractId,
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
            throw error(line, line.type() + " requires a preceding ART;N");
        }
    }

    private Set<LineType> allowedAfter(LineType type) {
        return switch (type) {
            case CTR, ROL, OFF -> EnumSet.of(LineType.ACC, LineType.ROL, LineType.OFF, LineType.OM);
            case ACC -> EnumSet.of(LineType.ACC, LineType.ROL, LineType.OFF, LineType.OM,
                    LineType.ART_N, LineType.IKAC, LineType.COND, LineType.TAR, LineType.AVT);
            case OM -> EnumSet.of(LineType.OID, LineType.ROL, LineType.ART_N);
            case OID -> EnumSet.of(LineType.ROL, LineType.ART_N);
            case ART_N -> EnumSet.of(
                    LineType.OID, LineType.IKAC, LineType.COND, LineType.ACC,
                    LineType.TAR, LineType.AVT, LineType.ART_N, LineType.ROL);
            case IKAC -> EnumSet.of(LineType.COND, LineType.ACC, LineType.TAR,
                    LineType.AVT, LineType.ART_N, LineType.ROL);
            case COND -> EnumSet.of(LineType.ACC, LineType.TAR, LineType.AVT,
                    LineType.ART_N, LineType.ROL);
            case TAR -> EnumSet.of(LineType.AVT, LineType.ART_N, LineType.ROL);
            case AVT -> EnumSet.of(LineType.ART_N, LineType.ROL);
            default -> EnumSet.noneOf(LineType.class);
        };
    }

    private String readContractId(BusinessLine ctr) {
        String id = ctr.field(1);
        return (id != null && !id.isBlank()) ? id : null;
    }

    private ContractFormatException error(BusinessLine line, String reason) {
        return new ContractFormatException(line.lineNumber(), contractId, reason);
    }
}
