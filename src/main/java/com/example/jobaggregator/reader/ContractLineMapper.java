package com.example.jobaggregator.reader;

import com.example.jobaggregator.domain.feed.LineType;
import com.example.jobaggregator.domain.feed.ParsedLine;
import com.example.jobaggregator.error.ContractFormatException;
import com.example.jobaggregator.reader.validator.AccountValidator;
import com.example.jobaggregator.reader.validator.AdvantageValidator;
import com.example.jobaggregator.reader.validator.ArticleValidator;
import com.example.jobaggregator.reader.validator.ConditionValidator;
import com.example.jobaggregator.reader.validator.ContractHeaderValidator;
import com.example.jobaggregator.reader.validator.ExternalIdValidator;
import com.example.jobaggregator.reader.validator.IkacValidator;
import com.example.jobaggregator.reader.validator.LineFieldValidator;
import com.example.jobaggregator.reader.validator.OfferValidator;
import com.example.jobaggregator.reader.validator.OmValidator;
import com.example.jobaggregator.reader.validator.RoleValidator;
import com.example.jobaggregator.reader.validator.TarifValidator;
import com.example.jobaggregator.reader.validator.TrailerValidator;
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
public final class ContractLineMapper implements LineMapper<ParsedLine> {

    /** Registry of per-type field validators. Line types not listed (HDR, TRL) pass through. */
    private static final Map<LineType, LineFieldValidator> VALIDATORS = Map.ofEntries(
            Map.entry(LineType.CTR,  ContractHeaderValidator::validate),
            Map.entry(LineType.ACC,  AccountValidator::validate),
            Map.entry(LineType.OM,   OmValidator::validate),
            Map.entry(LineType.OFF,  OfferValidator::validate),
            Map.entry(LineType.ART,  ArticleValidator::validate),
            Map.entry(LineType.ROL,  RoleValidator::validate),
            Map.entry(LineType.TAR,  TarifValidator::validate),
            Map.entry(LineType.OID,  ExternalIdValidator::validate),
            Map.entry(LineType.IKAC, IkacValidator::validate),
            Map.entry(LineType.COND, ConditionValidator::validate),
            Map.entry(LineType.AVT,  AdvantageValidator::validate),
            Map.entry(LineType.TRL,  TrailerValidator::validate)
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
            // Return a poison line instead of throwing — allows the contract block
            // to be assembled and rejected as a whole by the processor
            return new ParsedLine(lineNumber, LineType.UNKNOWN, line, fields);
        }

        LineFieldValidator validator = VALIDATORS.get(type);
        if (validator != null) {
            validator.validate(fields, lineNumber);
        }

        return new ParsedLine(lineNumber, type, line, fields);
    }
}
