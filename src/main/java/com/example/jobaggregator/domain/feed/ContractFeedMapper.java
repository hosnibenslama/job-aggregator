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
 * Maps raw {@link ParsedLine} tokens from the input feed into pure domain records.
 * Encapsulates the flat-file field indices and record-type parsing rules.
 */
public final class ContractFeedMapper {

    private ContractFeedMapper() {
        // Utility class
    }

    public static ContractHeader toHeader(ParsedLine line) {
        requireType(line, LineType.CTR);
        return new ContractHeader(
                line.field(1),  // Devise
                line.field(2),  // State
                line.field(3),  // Motif (optional)
                line.field(4),  // OuDistribution (optional)
                line.field(5),  // OuManagement
                line.field(6),  // AddressId (optional)
                line.field(7),  // BusinessRelationship
                line.field(8),  // EffectiveDate (optional)
                line.field(9),  // PeriodeFacturation (optional)
                line.field(10), // DatesFacturation (optional)
                line.field(11), // X-B3-TraceId
                line.field(12), // X-B3-SpanId
                line.field(13), // UserId
                line.field(14), // Channel
                line.field(15)  // Media
        );
    }

    public static Account toAccount(ParsedLine line) {
        requireType(line, LineType.ACC);
        return new Account(
                line.field(1),  // subType (BILL/FEE)
                line.field(2),  // BIC
                line.field(3),  // IBAN
                line.field(4)); // RIB (optional)
    }

    public static Role toRole(ParsedLine line) {
        requireType(line, LineType.ROL);
        return new Role(
                line.field(1),  // Role
                line.field(2),  // Brand
                line.field(3),  // Scope
                line.field(4),  // Holder ID
                line.field(5)); // IKPI
    }

    public static Offer toOffer(ParsedLine line) {
        requireType(line, LineType.OFF);
        return new Offer(
                line.field(1),  // Offer ID
                line.field(2),  // Provider
                line.field(3)); // Personalized label (optional)
    }

    public static MarketedObject toMarketedObject(ParsedLine line) {
        requireType(line, LineType.OM);
        return new MarketedObject(
                line.field(1),  // OM identifier
                line.field(2)); // BusinessRelationship
    }

    public static MarketedObject toOperation(ParsedLine line) {
        return toMarketedObject(line);
    }

    public static ExternalId toExternalId(ParsedLine line) {
        requireType(line, LineType.OID);
        return new ExternalId(line.field(1));
    }

    public static Article toArticle(ParsedLine line) {
        requireType(line, LineType.ART);
        return new Article(Integer.parseInt(line.field(1)));
    }

    public static Ikac toIkac(ParsedLine line) {
        requireType(line, LineType.IKAC);
        return new Ikac(line.field(1));
    }

    public static Condition toCondition(ParsedLine line) {
        requireType(line, LineType.COND);
        return new Condition(
                line.field(1),
                line.field(2));
    }

    public static Tarif toTarif(ParsedLine line) {
        requireType(line, LineType.TAR);
        return new Tarif(
                line.field(1),   // idOpraTarif (optional)
                line.field(2),   // typeFrais (optional)
                line.field(3),   // dateCreationTarif (optional)
                line.field(4),   // dateEffetTarif (optional)
                line.field(5),   // deviseTarif (optional)
                line.field(6),   // indicTarifPaliers (optional)
                line.field(7),   // formatTarif (optional)
                line.field(8),   // periodiciteFacturation (optional)
                line.field(9),   // typeTaxation (optional)
                line.field(10),  // typeTauxTarif (optional)
                line.field(11),  // tauxTarif (optional)
                line.field(12),  // montantBase (optional)
                line.field(13),  // ratioTarif (optional)
                line.field(14),  // montantUnite (optional)
                line.field(15),  // typeUnite (optional)
                line.field(16),  // indicLimiteHaute (optional)
                line.field(17),  // limiteHauteMontant (optional)
                line.field(18),  // indicLimiteBasse (optional)
                line.field(19)); // limiteBasseMontant (optional)
    }

    public static Advantage toAdvantage(ParsedLine line) {
        requireType(line, LineType.AVT);
        return new Advantage(
                line.field(1),
                line.field(2),
                line.field(3),
                line.field(4),
                line.field(5),
                line.field(6));
    }

    private static void requireType(ParsedLine line, LineType expected) {
        if (line.type() != expected) {
            throw new IllegalArgumentException("Expected " + expected + " line but got: " + line.type());
        }
    }
}
