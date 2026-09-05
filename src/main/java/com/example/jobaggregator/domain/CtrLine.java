package com.example.jobaggregator.domain;

/**
 * Typed view of a CTR (contract root) line as per the input file specification (section 4.2).
 *
 * <p>Field positions (1-indexed per spec):
 * <ol>
 *   <li>Type — fixed value "CTR" (not included here, implicit)</li>
 *   <li>Devise — currency code (ISO 4217, e.g. "EUR") — mandatory</li>
 *   <li>State — internal state code, max 2 chars (e.g. "16") — mandatory</li>
 *   <li>Motif — reason code (e.g. "003") — optional</li>
 *   <li>OuDistribution — organisational distribution unit — optional</li>
 *   <li>OuManagement — client agency code (e.g. "031030000") — mandatory</li>
 *   <li>AddressId — address identifier of the contract holder — optional</li>
 *   <li>BusinessRelationship — commercial relationship identifier — mandatory</li>
 *   <li>EffectiveDate — effect date (YYYY-MM-DDTHH:MM:SS.ssssssZ) — optional</li>
 *   <li>PeriodeFacturation — billing period (QUOTIDIENNE / HEBDOMADAIRE / MENSUELLE / ANNUELLE) — optional</li>
 *   <li>DatesFacturation — billing dates — optional</li>
 *   <li>XB3TraceId — correlation trace id, 16 hex chars — mandatory</li>
 *   <li>XB3SpanId — correlation span id, 16 hex chars — mandatory</li>
 *   <li>UserId — user identifier, 16 hex chars — mandatory</li>
 *   <li>Channel — interaction channel (001/007/008/012) — mandatory</li>
 *   <li>Media — interaction media, 3 chars — mandatory</li>
 * </ol>
 */
public record CtrLine(
        String devise,
        String state,
        String motif,
        String ouDistribution,
        String ouManagement,
        String addressId,
        String businessRelationship,
        String effectiveDate,
        String periodeFacturation,
        String datesFacturation,
        String xB3TraceId,
        String xB3SpanId,
        String userId,
        String channel,
        String media) {

    /**
     * Extracts a typed {@code CtrLine} from a raw {@link ParsedLine} of type CTR.
     * Assumes the line has already been validated by {@code ContractLineMapper}.
     */
    public static CtrLine from(ParsedLine line) {
        if (line.type() != LineType.CTR) {
            throw new IllegalArgumentException("Expected CTR line but got: " + line.type());
        }
        return new CtrLine(
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
}
