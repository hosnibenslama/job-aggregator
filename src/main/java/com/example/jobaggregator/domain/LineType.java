package com.example.jobaggregator.domain;

import java.util.Locale;

public enum LineType {
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

    public static LineType determineFromFields(String[] fields) {
        if (fields == null || fields.length == 0 || fields[0].isBlank()) {
            return UNKNOWN;
        }

        String first = fields[0].strip().toUpperCase(Locale.ROOT);
        try {
            return LineType.valueOf(first);
        } catch (IllegalArgumentException ignored) {
            return UNKNOWN;
        }
    }
}
