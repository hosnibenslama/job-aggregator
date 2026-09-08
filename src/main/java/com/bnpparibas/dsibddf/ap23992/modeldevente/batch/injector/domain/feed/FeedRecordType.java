package com.bnpparibas.dsibddf.ap23992.modeldevente.batch.injector.domain.feed;

import java.util.List;
import java.util.Locale;

/**
 * Record types defining the structure in the raw contract input feed.
 */
public enum FeedRecordType {
    HDR,
    CTR,
    ACC,
    ROL,
    OFF,
    OM,
    OID,
    ART,
    IKAC,
    COND,
    TAR,
    AVT,
    TRL,
    UNKNOWN;

    public static FeedRecordType determineFromFields(List<String> fields) {
        if (fields == null || fields.isEmpty() || fields.get(0).isBlank()) {
            return UNKNOWN;
        }

        String first = fields.get(0).strip().toUpperCase(Locale.ROOT);
        try {
            return FeedRecordType.valueOf(first);
        } catch (IllegalArgumentException ignored) {
            return UNKNOWN;
        }
    }
}
