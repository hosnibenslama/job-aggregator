package com.example.jobaggregator.reader;

import com.example.jobaggregator.domain.BusinessLine;
import com.example.jobaggregator.domain.LineType;
import com.example.jobaggregator.error.ContractFormatException;
import java.util.Arrays;
import java.util.List;
import org.springframework.batch.infrastructure.item.file.LineMapper;

public final class BusinessLineMapper implements LineMapper<BusinessLine> {

    @Override
    public BusinessLine mapLine(String line, int lineNumber) {
        if (line == null || line.isBlank()) {
            throw new ContractFormatException(lineNumber, null, "Blank input line");
        }

        List<String> fields = Arrays.stream(line.split(";", -1))
                .map(String::strip)
                .toList();
        LineType type = LineType.from(fields.toArray(String[]::new));

        if (type == LineType.UNKNOWN) {
            throw new ContractFormatException(
                    lineNumber,
                    null,
                    "Unknown line code: " + fields.getFirst());
        }

        validateLineFields(type, fields, lineNumber);

        return new BusinessLine(lineNumber, type, line, fields);
    }

    private void validateLineFields(LineType type, List<String> fields, int lineNumber) {
        switch (type) {
            case CTR -> validateCtrFields(fields, lineNumber);
            case ACC -> validateAccFields(fields, lineNumber);
            case OM -> validateOmFields(fields, lineNumber);
            case ART_N -> validateArtNFields(fields, lineNumber);
            default -> {} // no special validation for other line types yet
        }
    }

    private void validateCtrFields(List<String> fields, int lineNumber) {
        String contractId = field(fields, 1);
        if (contractId == null || contractId.isBlank()) {
            throw new ContractFormatException(lineNumber, null, "CTR field 1 (contractId) is required");
        }
        if (contractId.length() > 50) {
            throw new ContractFormatException(lineNumber, null, "CTR field 1 (contractId) must be at most 50 characters");
        }
    }

    private void validateAccFields(List<String> fields, int lineNumber) {
        String account = field(fields, 1);
        if (account == null || account.isBlank()) {
            throw new ContractFormatException(lineNumber, null, "ACC field 1 (account) is required");
        }
    }

    private void validateOmFields(List<String> fields, int lineNumber) {
        String operation = field(fields, 1);
        if (operation == null || operation.isBlank()) {
            throw new ContractFormatException(lineNumber, null, "OM field 1 (operation) is required");
        }
    }

    private void validateArtNFields(List<String> fields, int lineNumber) {
        String articleCode = field(fields, 1);
        if (articleCode == null || articleCode.isBlank()) {
            throw new ContractFormatException(lineNumber, null, "ART;N field 1 (articleCode) is required");
        }
    }

    private String field(List<String> fields, int index) {
        return index < fields.size() ? fields.get(index) : null;
    }
}
