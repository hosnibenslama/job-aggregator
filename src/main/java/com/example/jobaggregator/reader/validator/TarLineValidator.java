package com.example.jobaggregator.reader.validator;

import static com.example.jobaggregator.reader.validator.FieldConstraints.*;

import java.util.List;
import java.util.Set;

/**
 * Validates TAR (tarif) line fields — specification section 4.8 (complete, 20 fields).
 * All fields beyond the type are optional; enum-constrained fields are validated when present.
 * Conditional requirements (e.g. "required if formatTarif=003") are enforced where possible.
 *
 * <pre>
 * Pos  Field                   Mandatory  Rule (when present)
 * ---  ----------------------  ---------  -----------------------------------------------
 *  1   Type                    Yes        fixed "TAR"
 *  2   idOpraTarif             No
 *  3   typeFrais               No         001/002/003/004/005/006/007/013/014/900
 *  4   dateCreationTarif       No
 *  5   dateEffetTarif          No
 *  6   deviseTarif             No
 *  7   indicTarifPaliers       No         0=simple, 1=paliers
 *  8   formatTarif             No         001=forfaitaire, 002=par unité, 003=taux
 *  9   periodiciteFacturation  No         007=mensuelle, 010=trimestrielle, 012=annuelle
 * 10   typeTaxation            No         001=HT, 002=TTC, 003=HT-inconnu, 004=TTC-inconnu,
 *                                         005=HT DOM-TOM, 006=TTC DOM-TOM
 * 11   typeTauxTarif           Cond.      required if formatTarif=003; when present: 001=intérêts, 002=crédit
 * 12   tauxTarif               Cond.      required if formatTarif=001
 * 13   montantBase             Cond.      required if formatTarif=001
 * 14   ratioTarif              No
 * 15   montantUnite            Cond.      required if formatTarif=002
 * 16   typeUnite               Cond.      required if formatTarif=002; when present: 001-022
 * 17   indicLimiteHaute        No         0=limite renseignée, 1=limite non renseignée
 * 18   limiteHauteMontant      No
 * 19   indicLimiteBasse        No         0=limite renseignée, 1=limite non renseignée
 * 20   limiteBasseMontant      No
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

    /** 001=intérêts, 002=crédit */
    public static final Set<String> VALID_TYPE_TAUX_TARIF = Set.of("001", "002");

    /**
     * 001=Pour événement, 002=Pour opération, 003=Par opération, 004=Par dizaine,
     * 005=Par centaine, 006=Par millier, 007=Par unité, 008=Par effet, 009=Par facture,
     * 010=Par fichier, 011=Par lettre de relance, 012=Par modification ou annulation,
     * 013=Par notification, 014=Par ordre, 015=Par paiement, 016=Par remise,
     * 017=Par token, 018=Par virement, 019=Par alerte, 020=Les 100 écritures,
     * 021=Par bordereau, 022=Par demande
     */
    public static final Set<String> VALID_TYPE_UNITE =
            Set.of("001", "002", "003", "004", "005", "006", "007", "008", "009", "010",
                   "011", "012", "013", "014", "015", "016", "017", "018", "019", "020",
                   "021", "022");

    /** 0=limite renseignée, 1=limite non renseignée */
    public static final Set<String> VALID_INDIC_LIMITE = Set.of("0", "1");

    private TarLineValidator() {}

    public static void validate(List<String> fields, int lineNumber) {
        // ── fields 3-10: basic enum validation when present ──────────────────
        requireOneOfIfPresent(fields,  2, "typeFrais",              VALID_TYPE_FRAIS,      TYPE, lineNumber);
        requireOneOfIfPresent(fields,  6, "indicTarifPaliers",      VALID_INDIC_PALIERS,   TYPE, lineNumber);
        requireOneOfIfPresent(fields,  7, "formatTarif",            VALID_FORMAT_TARIF,    TYPE, lineNumber);
        requireOneOfIfPresent(fields,  8, "periodiciteFacturation",  VALID_PERIODICITE,    TYPE, lineNumber);
        requireOneOfIfPresent(fields,  9, "typeTaxation",           VALID_TYPE_TAXATION,   TYPE, lineNumber);

        // ── fields 11-20: new spec continuation ──────────────────────────────
        requireOneOfIfPresent(fields, 10, "typeTauxTarif",          VALID_TYPE_TAUX_TARIF, TYPE, lineNumber);
        requireOneOfIfPresent(fields, 15, "typeUnite",              VALID_TYPE_UNITE,      TYPE, lineNumber);
        requireOneOfIfPresent(fields, 16, "indicLimiteHaute",       VALID_INDIC_LIMITE,    TYPE, lineNumber);
        requireOneOfIfPresent(fields, 18, "indicLimiteBasse",       VALID_INDIC_LIMITE,    TYPE, lineNumber);

        // ── conditional cross-field rules ────────────────────────────────────
        String formatTarif = field(fields, 7);
        if ("003".equals(formatTarif)) {
            requireNonBlank(fields, 10, "typeTauxTarif", TYPE, lineNumber);
        }
        if ("001".equals(formatTarif)) {
            requireNonBlank(fields, 11, "tauxTarif",   TYPE, lineNumber);
            requireNonBlank(fields, 12, "montantBase",  TYPE, lineNumber);
        }
        if ("002".equals(formatTarif)) {
            requireNonBlank(fields, 14, "montantUnite", TYPE, lineNumber);
            requireNonBlank(fields, 15, "typeUnite",    TYPE, lineNumber);
        }
    }
}
