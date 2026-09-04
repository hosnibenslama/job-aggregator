package com.example.jobaggregator.reader;

import com.example.jobaggregator.domain.LineType;
import com.example.jobaggregator.domain.ParsedLine;
import com.example.jobaggregator.error.ContractFormatException;
import com.example.jobaggregator.reader.validator.AccLineValidator;
import com.example.jobaggregator.reader.validator.ArtLineValidator;
import com.example.jobaggregator.reader.validator.AvtLineValidator;
import com.example.jobaggregator.reader.validator.CondLineValidator;
import com.example.jobaggregator.reader.validator.CtrLineValidator;
import com.example.jobaggregator.reader.validator.IkacLineValidator;
import com.example.jobaggregator.reader.validator.LineFieldValidator;
import com.example.jobaggregator.reader.validator.OffLineValidator;
import com.example.jobaggregator.reader.validator.OidLineValidator;
import com.example.jobaggregator.reader.validator.OmLineValidator;
import com.example.jobaggregator.reader.validator.RolLineValidator;
import com.example.jobaggregator.reader.validator.TarLineValidator;
import com.example.jobaggregator.reader.validator.TrlLineValidator;
import java.util.Arrays;
import java.util.List;
import java.util.Map;

import org.springframework.batch.infrastructure.item.file.LineMapper;

/**
 * Maps a raw semicolon-delimited text line to a {@link ParsedLine}.
 *
 * <p>Parsing steps:
 * <ol>
 *   <li>Reject blank lines</li>
 *   <li>Split on {@code ;} and strip each field</li>
 *   <li>Determine the {@link LineType} from the first field</li>
 *   <li>Delegate field-level validation to the registered {@link LineFieldValidator} for that type</li>
 *   <li>Return the immutable {@link ParsedLine}</li>
 * </ol>
 */
public final class SemicolonLineParser implements LineMapper<ParsedLine> {

    /** Registry of per-type field validators. Line types not listed (HDR, TRL) pass through. */
    private static final Map<LineType, LineFieldValidator> VALIDATORS = Map.ofEntries(
            Map.entry(LineType.CTR,  CtrLineValidator::validate),
            Map.entry(LineType.ACC,  AccLineValidator::validate),
            Map.entry(LineType.OM,   OmLineValidator::validate),
            Map.entry(LineType.OFF,  OffLineValidator::validate),
            Map.entry(LineType.ART,  ArtLineValidator::validate),
            Map.entry(LineType.ROL,  RolLineValidator::validate),
            Map.entry(LineType.TAR,  TarLineValidator::validate),
            Map.entry(LineType.OID,  OidLineValidator::validate),
            Map.entry(LineType.IKAC, IkacLineValidator::validate),
            Map.entry(LineType.COND, CondLineValidator::validate),
            Map.entry(LineType.AVT,  AvtLineValidator::validate),
            Map.entry(LineType.TRL,  TrlLineValidator::validate)
    );

    @Override
    public ParsedLine mapLine(String line, int lineNumber) {
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

        LineFieldValidator validator = VALIDATORS.get(type);
        if (validator != null) {
            validator.validate(fields, lineNumber);
        }

        return new ParsedLine(lineNumber, type, line, fields);
    }
}
