package com.example.jobaggregator.reader;

import com.example.jobaggregator.domain.feed.FeedRecordType;
import com.example.jobaggregator.domain.feed.FeedRecord;
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
 * Maps a raw semicolon-delimited text line to a {@link FeedRecord}.
 *
 * <p>Parsing steps:
 * <ol>
 *   <li>Reject blank lines</li>
 *   <li>Split on {@code ;} and strip each field</li>
 *   <li>Determine the {@link FeedRecordType} from the first field</li>
 *   <li>Delegate field-level validation to the registered {@link LineFieldValidator} for that type</li>
 *   <li>Return the immutable {@link FeedRecord}</li>
 * </ol>
 */
public final class ContractLineMapper implements LineMapper<FeedRecord> {

    /** Registry of per-type field validators. Record types not listed (HDR, TRL) pass through. */
    private static final Map<FeedRecordType, LineFieldValidator> VALIDATORS = Map.ofEntries(
            Map.entry(FeedRecordType.CTR,  ContractHeaderValidator::validate),
            Map.entry(FeedRecordType.ACC,  AccountValidator::validate),
            Map.entry(FeedRecordType.OM,   OmValidator::validate),
            Map.entry(FeedRecordType.OFF,  OfferValidator::validate),
            Map.entry(FeedRecordType.ART,  ArticleValidator::validate),
            Map.entry(FeedRecordType.ROL,  RoleValidator::validate),
            Map.entry(FeedRecordType.TAR,  TarifValidator::validate),
            Map.entry(FeedRecordType.OID,  ExternalIdValidator::validate),
            Map.entry(FeedRecordType.IKAC, IkacValidator::validate),
            Map.entry(FeedRecordType.COND, ConditionValidator::validate),
            Map.entry(FeedRecordType.AVT,  AdvantageValidator::validate),
            Map.entry(FeedRecordType.TRL,  TrailerValidator::validate)
    );

    @Override
    public FeedRecord mapLine(String line, int lineNumber) {
        if (line == null || line.isBlank()) {
            throw new ContractFormatException(lineNumber, null, "Blank input line");
        }

        List<String> fields = Arrays.stream(line.split(";", -1))
                .map(String::strip)
                .toList();

        FeedRecordType type = FeedRecordType.determineFromFields(fields.toArray(String[]::new));
        if (type == FeedRecordType.UNKNOWN) {
            // Return a poison record instead of throwing — allows the contract block
            // to be assembled and rejected as a whole by the processor
            return new FeedRecord(lineNumber, FeedRecordType.UNKNOWN, line, fields);
        }

        LineFieldValidator validator = VALIDATORS.get(type);
        if (validator != null) {
            validator.validate(fields, lineNumber);
        }

        return new FeedRecord(lineNumber, type, line, fields);
    }
}
