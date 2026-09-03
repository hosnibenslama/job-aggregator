package com.example.jobaggregator.reader;

import com.example.jobaggregator.domain.BusinessLine;
import com.example.jobaggregator.domain.LineType;
import com.example.jobaggregator.error.ContractFormatException;
import java.util.Arrays;
import java.util.List;
import java.util.Set;
import java.util.regex.Pattern;
import org.springframework.batch.infrastructure.item.file.LineMapper;

public final class BusinessLineMapper implements LineMapper<BusinessLine> {

    /**
     * Valid channel codes as per spec section 4.2, field 15.
     * 001 = Intranet, 007 = Internet, 008 = GAB, 012 = Partenaire.
     */
    private static final Set<String> VALID_CHANNELS = Set.of("001", "007", "008", "012");

    /**
     * Pattern for a 16-character hexadecimal string (64-bit identifier).
     * Used for X-B3-TraceId, X-B3-SpanId, and UserId fields.
     */
    private static final Pattern HEX_16 = Pattern.compile("[0-9a-fA-F]{16}");

    @Override
    public BusinessLine mapLine(String line, int lineNumber) {
        if (line == null || line.isBlank()) {
            throw new ContractFormatException(lineNumber, null, "Blank input line");
        }

        List<String> fields = Arrays.stream(line.split(";", -1))
                .map(String::strip)
                .toList();
        LineType type = LineType.determineFromFields(fields.toArray(String[]::new));

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
            case ART -> validateArtFields(fields, lineNumber);
            default -> {} // no special validation for other line types yet
        }
    }

    /**
     * Validates CTR line fields against the input file specification (section 4.2).
     * <pre>
     * Pos  Field                  Mandatory  Rule
     * ---  ---------------------  ---------  --------------------------------
     *  1   Type                   Yes        Fixed "CTR"
     *  2   Devise                 Yes        Non-blank (ISO 4217)
     *  3   State                  Yes        Non-blank (max 2 chars)
     *  4   Motif                  No
     *  5   OuDistribution         No
     *  6   OuManagement           Yes        Non-blank
     *  7   AddressId              No
     *  8   BusinessRelationship   Yes        Non-blank
     *  9   EffectiveDate          No
     * 10   PeriodeFacturation     No
     * 11   DatesFacturation       No
     * 12   X-B3-TraceId           Yes        16 hex chars
     * 13   X-B3-SpanId            Yes        16 hex chars
     * 14   UserId                 Yes        16 hex chars
     * 15   Channel                Yes        One of: 001, 007, 008, 012
     * 16   Media                  Yes        Non-blank, 3 chars
     * </pre>
     */
    private void validateCtrFields(List<String> fields, int lineNumber) {
        if (fields.size() < 16) {
            throw new ContractFormatException(lineNumber, null,
                    "CTR requires 16 fields, found " + fields.size());
        }
        requireNonBlank(fields, 1, "Devise", lineNumber);
        requireNonBlank(fields, 2, "State", lineNumber);
        requireNonBlank(fields, 5, "OuManagement", lineNumber);
        requireNonBlank(fields, 7, "BusinessRelationship", lineNumber);
        requireHex16(fields, 11, "X-B3-TraceId", lineNumber);
        requireHex16(fields, 12, "X-B3-SpanId", lineNumber);
        requireHex16(fields, 13, "UserId", lineNumber);
        requireChannel(fields, 14, lineNumber);
        requireNonBlank(fields, 15, "Media", lineNumber);
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

    private void validateArtFields(List<String> fields, int lineNumber) {
        String articleCode = field(fields, 1);
        if (articleCode == null || articleCode.isBlank()) {
            throw new ContractFormatException(lineNumber, null, "ART field 1 (articleCode) is required");
        }
    }

    // -----------------------------------------------------------------------
    // Helper validators
    // -----------------------------------------------------------------------

    private void requireNonBlank(List<String> fields, int index, String name, int lineNumber) {
        String val = field(fields, index);
        if (val == null || val.isBlank()) {
            throw new ContractFormatException(lineNumber, null,
                    "CTR field " + (index + 1) + " (" + name + ") is required and must not be blank");
        }
    }

    private void requireHex16(List<String> fields, int index, String name, int lineNumber) {
        String val = field(fields, index);
        if (val == null || !HEX_16.matcher(val).matches()) {
            throw new ContractFormatException(lineNumber, null,
                    "CTR field " + (index + 1) + " (" + name + ") must be exactly 16 hexadecimal characters, got: " + val);
        }
    }

    private void requireChannel(List<String> fields, int index, int lineNumber) {
        String val = field(fields, index);
        if (val == null || !VALID_CHANNELS.contains(val)) {
            throw new ContractFormatException(lineNumber, null,
                    "CTR field " + (index + 1) + " (Channel) must be one of " + VALID_CHANNELS + ", got: " + val);
        }
    }

    private String field(List<String> fields, int index) {
        return index < fields.size() ? fields.get(index) : null;
    }
}
