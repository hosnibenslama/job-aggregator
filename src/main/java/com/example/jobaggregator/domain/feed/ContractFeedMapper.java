package com.example.jobaggregator.domain.feed;

import com.example.jobaggregator.domain.Account;
import com.example.jobaggregator.domain.Advantage;
import com.example.jobaggregator.domain.Article;
import com.example.jobaggregator.domain.Condition;
import com.example.jobaggregator.domain.ContractHeader;
import com.example.jobaggregator.domain.ExternalId;
import com.example.jobaggregator.domain.Ikac;
import com.example.jobaggregator.domain.Offer;
import com.example.jobaggregator.domain.MarketedObject;
import com.example.jobaggregator.domain.Role;
import com.example.jobaggregator.domain.Tarif;

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
                record.field(1),  // Devise
                record.field(2),  // State
                record.field(3),  // Motif (optional)
                record.field(4),  // OuDistribution (optional)
                record.field(5),  // OuManagement
                record.field(6),  // AddressId (optional)
                record.field(7),  // BusinessRelationship
                record.field(8),  // EffectiveDate (optional)
                record.field(9),  // PeriodeFacturation (optional)
                record.field(10), // DatesFacturation (optional)
                record.field(11), // X-B3-TraceId
                record.field(12), // X-B3-SpanId
                record.field(13), // UserId
                record.field(14), // Channel
                record.field(15)  // Media
        );
    }

    public static Account toAccount(FeedRecord record) {
        requireType(record, FeedRecordType.ACC);
        return new Account(
                record.field(1),  // subType (BILL/FEE)
                record.field(2),  // BIC
                record.field(3),  // IBAN
                record.field(4)); // RIB (optional)
    }

    public static Role toRole(FeedRecord record) {
        requireType(record, FeedRecordType.ROL);
        return new Role(
                record.field(1),  // Role
                record.field(2),  // Brand
                record.field(3),  // Scope
                record.field(4),  // Holder ID
                record.field(5)); // IKPI
    }

    public static Offer toOffer(FeedRecord record) {
        requireType(record, FeedRecordType.OFF);
        return new Offer(
                record.field(1),  // Offer ID
                record.field(2),  // Provider
                record.field(3)); // Personalized label (optional)
    }

    public static MarketedObject toMarketedObject(FeedRecord record) {
        requireType(record, FeedRecordType.OM);
        return new MarketedObject(
                record.field(1),  // OM identifier
                record.field(2)); // BusinessRelationship
    }

    public static MarketedObject toOperation(FeedRecord record) {
        return toMarketedObject(record);
    }

    public static ExternalId toExternalId(FeedRecord record) {
        requireType(record, FeedRecordType.OID);
        return new ExternalId(record.field(1));
    }

    public static Article toArticle(FeedRecord record) {
        requireType(record, FeedRecordType.ART);
        return new Article(Integer.parseInt(record.field(1)));
    }

    public static Ikac toIkac(FeedRecord record) {
        requireType(record, FeedRecordType.IKAC);
        return new Ikac(record.field(1));
    }

    public static Condition toCondition(FeedRecord record) {
        requireType(record, FeedRecordType.COND);
        return new Condition(
                record.field(1),
                record.field(2));
    }

    public static Tarif toTarif(FeedRecord record) {
        requireType(record, FeedRecordType.TAR);
        return new Tarif(
                record.field(1),   // idOpraTarif (optional)
                record.field(2),   // typeFrais (optional)
                record.field(3),   // dateCreationTarif (optional)
                record.field(4),   // dateEffetTarif (optional)
                record.field(5),   // deviseTarif (optional)
                record.field(6),   // indicTarifPaliers (optional)
                record.field(7),   // formatTarif (optional)
                record.field(8),   // periodiciteFacturation (optional)
                record.field(9),   // typeTaxation (optional)
                record.field(10),  // typeTauxTarif (optional)
                record.field(11),  // tauxTarif (optional)
                record.field(12),  // montantBase (optional)
                record.field(13),  // ratioTarif (optional)
                record.field(14),  // montantUnite (optional)
                record.field(15),  // typeUnite (optional)
                record.field(16),  // indicLimiteHaute (optional)
                record.field(17),  // limiteHauteMontant (optional)
                record.field(18),  // indicLimiteBasse (optional)
                record.field(19)); // limiteBasseMontant (optional)
    }

    public static Advantage toAdvantage(FeedRecord record) {
        requireType(record, FeedRecordType.AVT);
        return new Advantage(
                record.field(1),
                record.field(2),
                record.field(3),
                record.field(4),
                record.field(5),
                record.field(6));
    }

    private static void requireType(FeedRecord record, FeedRecordType expected) {
        if (record.type() != expected) {
            throw new IllegalArgumentException("Expected " + expected + " record but got: " + record.type());
        }
    }
}
