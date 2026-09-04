package com.example.jobaggregator.reader.validator;

import static com.example.jobaggregator.reader.validator.LineValidationSupport.*;

import java.util.List;
import java.util.Set;

/**
 * Validates TAR (tarif) line fields — specification section 4.8.
 * All fields beyond the type are optional; enum-constrained optional fields are
 * validated only when non-blank.
 *
 * <pre>
 * Pos  Field                   Mandatory  Rule (when present)
 * ---  ----------------------  ---------  -----------------------------------------------
 *  1   Type                    Yes        fixed "TAR"
 *  2   idOpraTarif             No
 *  3   typeFrais               No         001/002/003/004/005/006/007/013/014/900
 *  4   DateCreationTarif       No         YYYY-MM-DDTHH:MM:SS.ssssssZ
 *  5   DateEffetTarif          No         YYYY-MM-DDTHH:MM:SS.ssssssZ
 *  6   deviseTarif             No         e.g. EUR
 *  7   indicTarifPaliers       No         0=simple, 1=paliers
 *  8   formatTarif             No         001=forfaitaire, 002=par unité, 003=taux
 *  9   periodiciteFacturation  No         007=mensuelle, 010=trimestrielle, 012=annuelle
 * 10   typeTaxation            No         001=HT, 002=TTC, 003=HT-inconnu, 004=TTC-inconnu,
 *                                         005=HT DOM-TOM, 006=TTC DOM-TOM
 * </pre>
 */
public final class TarLineValidator {

    private static final String TYPE = "TAR";

    /** 001=Abonnement, 002=Installation, 003=Fixes, 004=Perf, 005=Perf capé,
     *  006=Franchise, 007=Inactivité, 013=Mensuel, 014=Gratuité HB, 900=Négociation */
    public static final Set<String> VALID_TYPE_FRAIS =
            Set.of("001", "002", "003", "004", "005", "006", "007", "013", "014", "900");

    /** 0=Tarif simple, 1=Tarif par paliers */
    public static final Set<String> VALID_INDIC_PALIERS = Set.of("0", "1");

    /** 001=forfaitaire, 002=par unité, 003=taux */
    public static final Set<String> VALID_FORMAT_TARIF = Set.of("001", "002", "003");

    /** 007=mensuelle, 010=trimestrielle, 012=annuelle */
    public static final Set<String> VALID_PERIODICITE = Set.of("007", "010", "012");

    /** 001=HT, 002=TTC, 003=HT-inconnu, 004=TTC-inconnu, 005=HT DOM-TOM, 006=TTC DOM-TOM */
    public static final Set<String> VALID_TYPE_TAXATION =
            Set.of("001", "002", "003", "004", "005", "006");

    private TarLineValidator() {}

    public static void validate(List<String> fields, int lineNumber) {
        // All TAR fields beyond the type are optional — only validate enum constraints
        requireOneOfIfPresent(fields, 2, "typeFrais",              VALID_TYPE_FRAIS,   TYPE, lineNumber);
        requireOneOfIfPresent(fields, 6, "indicTarifPaliers",      VALID_INDIC_PALIERS, TYPE, lineNumber);
        requireOneOfIfPresent(fields, 7, "formatTarif",            VALID_FORMAT_TARIF,  TYPE, lineNumber);
        requireOneOfIfPresent(fields, 8, "periodiciteFacturation",  VALID_PERIODICITE,  TYPE, lineNumber);
        requireOneOfIfPresent(fields, 9, "typeTaxation",           VALID_TYPE_TAXATION, TYPE, lineNumber);
    }
}
