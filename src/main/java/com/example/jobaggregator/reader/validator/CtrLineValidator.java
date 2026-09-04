package com.example.jobaggregator.reader.validator;

import static com.example.jobaggregator.reader.validator.FieldConstraints.*;

import java.util.List;
import java.util.Set;

/**
 * Validates CTR (contrat racine) line fields — specification section 4.2.
 *
 * <pre>
 * Pos  Field                  Mandatory  Rule
 * ---  ---------------------  ---------  --------------------------------
 *  1   Type                   Yes        fixed "CTR"
 *  2   Devise                 Yes        ISO 4217 (ex. EUR)
 *  3   State                  Yes        internal state code, max 2 chars
 *  4   Motif                  No
 *  5   OuDistribution         No
 *  6   OuManagement           Yes        client agency code
 *  7   AddressId              No
 *  8   BusinessRelationship   Yes
 *  9   EffectiveDate          No
 * 10   PeriodeFacturation     No
 * 11   DatesFacturation       No
 * 12   X-B3-TraceId           Yes        16 hex chars
 * 13   X-B3-SpanId            Yes        16 hex chars
 * 14   UserId                 Yes        non-blank
 * 15   Channel                Yes        001/007/008/012
 * 16   Media                  Yes        001/003/055/073
 * </pre>
 */
public final class CtrLineValidator {

    private static final String TYPE = "CTR";

    /** 001=Intranet, 007=Internet, 008=GAB, 012=Partenaire */
    public static final Set<String> VALID_CHANNELS = Set.of("001", "007", "008", "012");

    /** 001=Face à face, 003=Téléphone, 055=SMS, 073=Chat */
    public static final Set<String> VALID_MEDIA = Set.of("001", "003", "055", "073");

    private CtrLineValidator() {}

    public static void validate(List<String> fields, int lineNumber) {
        requireMinSize (fields, 16, TYPE, lineNumber);
        requireNonBlank(fields, 1,  "Devise",               TYPE, lineNumber);
        requireNonBlank(fields, 2,  "State",                TYPE, lineNumber);
        requireNonBlank(fields, 5,  "OuManagement",         TYPE, lineNumber);
        requireNonBlank(fields, 7,  "BusinessRelationship", TYPE, lineNumber);
        requireHex16   (fields, 11, "X-B3-TraceId",         TYPE, lineNumber);
        requireHex16   (fields, 12, "X-B3-SpanId",          TYPE, lineNumber);
        requireNonBlank(fields, 13, "UserId",               TYPE, lineNumber);
        requireOneOf   (fields, 14, "Channel", VALID_CHANNELS, TYPE, lineNumber);
        requireOneOf   (fields, 15, "Media",   VALID_MEDIA,    TYPE, lineNumber);
    }
}
