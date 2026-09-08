package com.bnpparibas.dsibddf.ap23992.modeldevente.batch.injector.domain.feed;

import com.bnpparibas.dsibddf.ap23992.modeldevente.batch.injector.domain.Account;
import com.bnpparibas.dsibddf.ap23992.modeldevente.batch.injector.domain.Advantage;
import com.bnpparibas.dsibddf.ap23992.modeldevente.batch.injector.domain.Article;
import com.bnpparibas.dsibddf.ap23992.modeldevente.batch.injector.domain.Condition;
import com.bnpparibas.dsibddf.ap23992.modeldevente.batch.injector.domain.ContractHeader;
import com.bnpparibas.dsibddf.ap23992.modeldevente.batch.injector.domain.ExternalId;
import com.bnpparibas.dsibddf.ap23992.modeldevente.batch.injector.domain.Ikac;
import com.bnpparibas.dsibddf.ap23992.modeldevente.batch.injector.domain.Offer;
import com.bnpparibas.dsibddf.ap23992.modeldevente.batch.injector.domain.MarketedObject;
import com.bnpparibas.dsibddf.ap23992.modeldevente.batch.injector.domain.Role;
import com.bnpparibas.dsibddf.ap23992.modeldevente.batch.injector.domain.Tarif;

/**
 * Maps raw {@link FeedRecord} tokens from the input feed into pure domain records.
 * Encapsulates the flat-file field indices and record-type parsing rules.
 */
public final class ContractFeedMapper {

    private ContractFeedMapper() {
        // Utility class
    }

    public static ContractHeader toHeader(FeedRecord record) {
        requireType(record, FeedRecordType.CTR);
        return new ContractHeader(
                record.getField(1),  // Devise
                record.getField(2),  // State
                record.getField(3),  // Motif (optional)
                record.getField(4),  // OuDistribution (optional)
                record.getField(5),  // OuManagement
                record.getField(6),  // AddressId (optional)
                record.getField(7),  // BusinessRelationship
                record.getField(8),  // EffectiveDate (optional)
                record.getField(9),  // PeriodeFacturation (optional)
                record.getField(10), // DatesFacturation (optional)
                record.getField(11), // X-B3-TraceId
                record.getField(12), // X-B3-SpanId
                record.getField(13), // UserId
                record.getField(14), // Channel
                record.getField(15)  // Media
        );
    }

    public static Account toAccount(FeedRecord record) {
        requireType(record, FeedRecordType.ACC);
        return new Account(
                record.getField(1),  // subType (BILL/FEE)
                record.getField(2),  // BIC
                record.getField(3),  // IBAN
                record.getField(4)); // RIB (optional)
    }

    public static Role toRole(FeedRecord record) {
        requireType(record, FeedRecordType.ROL);
        return new Role(
                record.getField(1),  // Role
                record.getField(2),  // Brand
                record.getField(3),  // Scope
                record.getField(4),  // Holder ID
                record.getField(5)); // IKPI
    }

    public static Offer toOffer(FeedRecord record) {
        requireType(record, FeedRecordType.OFF);
        return new Offer(
                record.getField(1),  // Offer ID
                record.getField(2)); // Personalized label (optional)
    }

    public static MarketedObject toMarketedObject(FeedRecord record) {
        requireType(record, FeedRecordType.OM);
        return new MarketedObject(
                record.getField(1),  // OM identifier
                record.getField(2)); // BusinessRelationship
    }



    public static ExternalId toExternalId(FeedRecord record) {
        requireType(record, FeedRecordType.OID);
        return new ExternalId(record.getField(1));
    }

    public static Article toArticle(FeedRecord record) {
        requireType(record, FeedRecordType.ART);
        return new Article(Integer.parseInt(record.getField(1)));
    }

    public static Ikac toIkac(FeedRecord record) {
        requireType(record, FeedRecordType.IKAC);
        return new Ikac(record.getField(1), record.getField(2));
    }

    public static Condition toCondition(FeedRecord record) {
        requireType(record, FeedRecordType.COND);
        return new Condition(
                record.getField(1),
                record.getField(2));
    }

    public static Tarif toTarif(FeedRecord record) {
        requireType(record, FeedRecordType.TAR);
        return new Tarif(
                record.getField(1),   // idOpraTarif (optional)
                record.getField(2),   // typeFrais (optional)
                record.getField(3),   // dateCreationTarif (optional)
                record.getField(4),   // dateEffetTarif (optional)
                record.getField(5),   // deviseTarif (optional)
                record.getField(6),   // indicTarifPaliers (optional)
                record.getField(7),   // formatTarif (optional)
                record.getField(8),   // periodiciteFacturation (optional)
                record.getField(9),   // typeTaxation (optional)
                record.getField(10),  // typeTauxTarif (optional)
                record.getField(11),  // tauxTarif (optional)
                record.getField(12),  // montantBase (optional)
                record.getField(13),  // ratioTarif (optional)
                record.getField(14),  // montantUnite (optional)
                record.getField(15),  // typeUnite (optional)
                record.getField(16),  // indicLimiteHaute (optional)
                record.getField(17),  // limiteHauteMontant (optional)
                record.getField(18),  // indicLimiteBasse (optional)
                record.getField(19)); // limiteBasseMontant (optional)
    }

    public static Advantage toAdvantage(FeedRecord record) {
        requireType(record, FeedRecordType.AVT);
        return new Advantage(
                record.getField(1),
                record.getField(2),
                record.getField(3),
                record.getField(4),
                record.getField(5),
                record.getField(6));
    }

    private static void requireType(FeedRecord record, FeedRecordType expected) {
        if (record.type() != expected) {
            throw new IllegalArgumentException("Expected " + expected + " record but got: " + record.type());
        }
    }
}
