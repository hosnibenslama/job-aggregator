package com.bnpparibas.dsibddf.ap23992.modelevente.batch.injector.reader;

import com.bnpparibas.dsibddf.ap23992.modelevente.batch.injector.domain.feed.FeedRecordType;
import java.util.EnumMap;
import java.util.Map;
import java.util.Set;

import static com.bnpparibas.dsibddf.ap23992.modelevente.batch.injector.domain.feed.FeedRecordType.ACC;
import static com.bnpparibas.dsibddf.ap23992.modelevente.batch.injector.domain.feed.FeedRecordType.ART;
import static com.bnpparibas.dsibddf.ap23992.modelevente.batch.injector.domain.feed.FeedRecordType.AVT;
import static com.bnpparibas.dsibddf.ap23992.modelevente.batch.injector.domain.feed.FeedRecordType.COND;
import static com.bnpparibas.dsibddf.ap23992.modelevente.batch.injector.domain.feed.FeedRecordType.CTR;
import static com.bnpparibas.dsibddf.ap23992.modelevente.batch.injector.domain.feed.FeedRecordType.IKAC;
import static com.bnpparibas.dsibddf.ap23992.modelevente.batch.injector.domain.feed.FeedRecordType.OFF;
import static com.bnpparibas.dsibddf.ap23992.modelevente.batch.injector.domain.feed.FeedRecordType.OID;
import static com.bnpparibas.dsibddf.ap23992.modelevente.batch.injector.domain.feed.FeedRecordType.OM;
import static com.bnpparibas.dsibddf.ap23992.modelevente.batch.injector.domain.feed.FeedRecordType.ROL;
import static com.bnpparibas.dsibddf.ap23992.modelevente.batch.injector.domain.feed.FeedRecordType.TAR;

/**
 * Record-ordering grammar rules defining allowed successor record types
 * according to the contract feed specification (Section 9).
 * Package-private — used exclusively by {@link ContractBlockAssembler}.
 */
final class ContractSequencingRules {

    private static final Set<FeedRecordType> ALLOWED_AFTER_CONTRACT_OR_OFFER = Set.of(ACC, ROL, OFF, TAR, AVT, OM);
    private static final Set<FeedRecordType> ALLOWED_AFTER_ROLE = Set.of(ACC, ROL, OFF, OM, OID, ART, TAR, AVT);
    private static final Set<FeedRecordType> ALLOWED_AFTER_ACCOUNT = Set.of(ACC, ROL, OFF, OM, ART, IKAC, COND, TAR, AVT, OID);
    private static final Set<FeedRecordType> ALLOWED_AFTER_MARKETED_OBJECT = Set.of(OID, ROL, TAR, AVT, ART);
    private static final Set<FeedRecordType> ALLOWED_AFTER_OID = Set.of(OID, ROL, ART, IKAC, COND, ACC, TAR, AVT, OM);
    private static final Set<FeedRecordType> ALLOWED_AFTER_IKAC = Set.of(COND, ACC, ROL, TAR, AVT, ART, OM, OID);
    private static final Set<FeedRecordType> ALLOWED_AFTER_CONDITION = Set.of(COND, ACC, ROL, TAR, AVT, ART, OM, OID);
    private static final Set<FeedRecordType> ALLOWED_AFTER_ARTICLE = Set.of(OID, IKAC, COND, ACC, ROL, TAR, AVT, ART, OM);
    private static final Set<FeedRecordType> ALLOWED_AFTER_TARIF = Set.of(TAR, AVT, ART, OM, ROL, ACC, OID);
    private static final Set<FeedRecordType> ALLOWED_AFTER_ADVANTAGE = Set.of(AVT, ART, OM, ROL, ACC, OID);

    private static final Map<FeedRecordType, Set<FeedRecordType>> RULES = new EnumMap<>(Map.ofEntries(
            Map.entry(CTR, ALLOWED_AFTER_CONTRACT_OR_OFFER),
            Map.entry(OFF, ALLOWED_AFTER_CONTRACT_OR_OFFER),
            Map.entry(ROL, ALLOWED_AFTER_ROLE),
            Map.entry(ACC, ALLOWED_AFTER_ACCOUNT),
            Map.entry(OM, ALLOWED_AFTER_MARKETED_OBJECT),
            Map.entry(OID, ALLOWED_AFTER_OID),
            Map.entry(IKAC, ALLOWED_AFTER_IKAC),
            Map.entry(COND, ALLOWED_AFTER_CONDITION),
            Map.entry(ART, ALLOWED_AFTER_ARTICLE),
            Map.entry(TAR, ALLOWED_AFTER_TARIF),
            Map.entry(AVT, ALLOWED_AFTER_ADVANTAGE)
    ));

    private ContractSequencingRules() {
    }

    static boolean isAllowed(FeedRecordType previousRecordType, FeedRecordType nextRecordType) {
        Set<FeedRecordType> allowed = RULES.get(previousRecordType);
        return allowed != null && allowed.contains(nextRecordType);
    }

    static Set<FeedRecordType> getAllowedSuccessors(FeedRecordType previousRecordType) {
        return RULES.getOrDefault(previousRecordType, Set.of());
    }
}
