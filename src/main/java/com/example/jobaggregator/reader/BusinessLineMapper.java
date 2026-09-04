package com.example.jobaggregator.reader;

import com.example.jobaggregator.domain.BusinessLine;
import com.example.jobaggregator.domain.LineType;
import com.example.jobaggregator.error.ContractFormatException;
import com.example.jobaggregator.reader.validator.AccLineValidator;
import com.example.jobaggregator.reader.validator.ArtLineValidator;
import com.example.jobaggregator.reader.validator.CtrLineValidator;
import com.example.jobaggregator.reader.validator.LineValidator;
import com.example.jobaggregator.reader.validator.OffLineValidator;
import com.example.jobaggregator.reader.validator.OmLineValidator;
import com.example.jobaggregator.reader.validator.RolLineValidator;
import com.example.jobaggregator.reader.validator.TarLineValidator;
import java.util.Arrays;
import java.util.List;
import java.util.Map;
import org.springframework.batch.infrastructure.item.file.LineMapper;

/**
 * Maps a raw semicolon-delimited text line to a {@link BusinessLine}.
 *
 * <p>Parsing steps:
 * <ol>
 *   <li>Reject blank lines</li>
 *   <li>Split on {@code ;} and strip each field</li>
 *   <li>Determine the {@link LineType} from the first field</li>
 *   <li>Delegate field-level validation to the registered {@link LineValidator} for that type</li>
 *   <li>Return the immutable {@link BusinessLine}</li>
 * </ol>
 */
public final class BusinessLineMapper implements LineMapper<BusinessLine> {

    /**
     * Registry of per-type validators.
     * Line types not listed here (COND, OID, IKAC, AVT, HDR, TRL) pass through without
     * field-level validation until their specification is finalized.
     */
    private static final Map<LineType, LineValidator> VALIDATORS = Map.of(
            LineType.CTR, CtrLineValidator::validate,
            LineType.ACC, AccLineValidator::validate,
            LineType.OM,  OmLineValidator::validate,
            LineType.OFF, OffLineValidator::validate,
            LineType.ART, ArtLineValidator::validate,
            LineType.ROL, RolLineValidator::validate,
            LineType.TAR, TarLineValidator::validate
    );

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
            throw new ContractFormatException(lineNumber, null,
                    "Unknown line type: " + fields.getFirst());
        }

        LineValidator validator = VALIDATORS.get(type);
        if (validator != null) {
            validator.validate(fields, lineNumber);
        }

        return new BusinessLine(lineNumber, type, line, fields);
    }
}
