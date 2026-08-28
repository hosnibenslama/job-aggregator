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
    ART_N,
    IKAC,
    COND,
    TAR,
    AVT,
    TRL,
    UNKNOWN;

    public static LineType from(String[] fields) {
        if (fields.length == 0 || fields[0].isBlank()) {
            return UNKNOWN;
        }

        String first = fields[0].strip().toUpperCase(Locale.ROOT);
        if ("ART".equals(first)
                && fields.length > 1
                && "N".equalsIgnoreCase(fields[1].strip())) {
            return ART_N;
        }

        try {
            return LineType.valueOf(first);
        } catch (IllegalArgumentException ignored) {
            return UNKNOWN;
        }
    }
}
