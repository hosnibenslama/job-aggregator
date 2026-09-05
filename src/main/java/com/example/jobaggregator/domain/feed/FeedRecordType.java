package com.example.jobaggregator.domain.feed;

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

    public static FeedRecordType determineFromFields(String[] fields) {
        if (fields == null || fields.length == 0 || fields[0].isBlank()) {
            return UNKNOWN;
        }

        String first = fields[0].strip().toUpperCase(Locale.ROOT);
        try {
            return FeedRecordType.valueOf(first);
        } catch (IllegalArgumentException ignored) {
            return UNKNOWN;
        }
    }
}
