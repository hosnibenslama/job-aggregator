package com.example.jobaggregator.reader;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.example.jobaggregator.domain.BusinessLine;
import com.example.jobaggregator.domain.LineType;
import com.example.jobaggregator.error.ContractFormatException;
import java.util.List;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

/**
 * Unit tests for {@link BusinessLineMapper} covering all spec-defined line type validators.
 */
class BusinessLineMapperTest {

    private final BusinessLineMapper mapper = new BusinessLineMapper();

    // =========================================================================
    // General parsing
    // =========================================================================

    @Test
    void rejectsBlankLine() {
        assertThatThrownBy(() -> mapper.mapLine("", 1))
                .isInstanceOf(ContractFormatException.class)
                .hasMessageContaining("Blank");
    }

    @Test
    void rejectsUnknownLineType() {
        assertThatThrownBy(() -> mapper.mapLine("XYZ;data", 1))
                .isInstanceOf(ContractFormatException.class)
                .hasMessageContaining("Unknown line type");
    }

    // =========================================================================
    // CTR — section 4.2
    // =========================================================================

    @Nested
    class CtrValidation {

        private static final String VALID_CTR =
                "CTR;EUR;16;000;Carte VISA PREMIER;031030000;;BR-00001090;;MENSUELLE;;" +
                "abcdef0123456789;fedcba9876543210;1234567890abcdef;001;003";

        @Test
        void parsesValidCtrLine() {
            BusinessLine line = mapper.mapLine(VALID_CTR, 1);
            assertThat(line.type()).isEqualTo(LineType.CTR);
            assertThat(line.field(1)).isEqualTo("EUR");
            assertThat(line.field(5)).isEqualTo("031030000");
            assertThat(line.field(7)).isEqualTo("BR-00001090");
            assertThat(line.field(11)).isEqualTo("abcdef0123456789");
            assertThat(line.field(14)).isEqualTo("001");
            assertThat(line.field(15)).isEqualTo("003");
        }

        @Test
        void rejectsTooFewFields() {
            assertThatThrownBy(() -> mapper.mapLine("CTR;EUR;16", 2))
                    .isInstanceOf(ContractFormatException.class)
                    .hasMessageContaining("16 fields");
        }

        @Test
        void rejectsBlankDevise()               { assertCtrFieldRequired(1,  "Devise"); }
        @Test
        void rejectsBlankState()                { assertCtrFieldRequired(2,  "State"); }
        @Test
        void rejectsBlankOuManagement()         { assertCtrFieldRequired(5,  "OuManagement"); }
        @Test
        void rejectsBlankBusinessRelationship() { assertCtrFieldRequired(7,  "BusinessRelationship"); }

        @Test
        void rejectsInvalidTraceId() {
            assertThatThrownBy(() -> mapper.mapLine(buildCtr(11, "not-hex!"), 7))
                    .isInstanceOf(ContractFormatException.class)
                    .hasMessageContaining("X-B3-TraceId");
        }

        @Test
        void rejectsTraceIdTooShort() {
            assertThatThrownBy(() -> mapper.mapLine(buildCtr(11, "abcdef01"), 8))
                    .isInstanceOf(ContractFormatException.class)
                    .hasMessageContaining("X-B3-TraceId");
        }

        @Test
        void rejectsBlankUserId() { assertCtrFieldRequired(13, "UserId"); }

        @Test
        void rejectsInvalidChannel() {
            assertThatThrownBy(() -> mapper.mapLine(buildCtr(14, "999"), 9))
                    .isInstanceOf(ContractFormatException.class)
                    .hasMessageContaining("Channel");
        }

        @Test
        void acceptsAllValidChannels() {
            for (String ch : List.of("001", "007", "008", "012")) {
                assertThat(mapper.mapLine(buildCtr(14, ch), 1).field(14)).isEqualTo(ch);
            }
        }

        @Test
        void rejectsInvalidMedia() {
            assertThatThrownBy(() -> mapper.mapLine(buildCtr(15, "WEB"), 10))
                    .isInstanceOf(ContractFormatException.class)
                    .hasMessageContaining("Media");
        }

        @Test
        void acceptsAllValidMediaCodes() {
            for (String media : List.of("001", "003", "055", "073")) {
                assertThat(mapper.mapLine(buildCtr(15, media), 1).field(15)).isEqualTo(media);
            }
        }

        private void assertCtrFieldRequired(int idx, String name) {
            assertThatThrownBy(() -> mapper.mapLine(buildCtr(idx, ""), 1))
                    .isInstanceOf(ContractFormatException.class)
                    .hasMessageContaining(name);
        }

        private static String buildCtr(int fieldIndex, String newValue) {
            String[] fields = {
                "CTR",                // 0  Type
                "EUR",                // 1  Devise
                "16",                 // 2  State
                "000",                // 3  Motif (optional)
                "VISA PREMIER",       // 4  OuDistribution (optional)
                "031030000",          // 5  OuManagement
                "",                   // 6  AddressId (optional)
                "BR-00001090",        // 7  BusinessRelationship
                "",                   // 8  EffectiveDate (optional)
                "MENSUELLE",          // 9  PeriodeFacturation (optional)
                "",                   // 10 DatesFacturation (optional)
                "abcdef0123456789",   // 11 X-B3-TraceId
                "fedcba9876543210",   // 12 X-B3-SpanId
                "1234567890abcdef",   // 13 UserId
                "001",                // 14 Channel
                "003"                 // 15 Media
            };
            fields[fieldIndex] = newValue;
            return String.join(";", fields);
        }
    }

    // =========================================================================
    // ACC — section 4.3
    // =========================================================================

    @Nested
    class AccValidation {

        private static final String VALID_ACC = "ACC;BILL;BNPAFRPP;FR76300040219600001;300040219600001";

        @Test
        void parsesValidAccLine() {
            BusinessLine line = mapper.mapLine(VALID_ACC, 1);
            assertThat(line.type()).isEqualTo(LineType.ACC);
            assertThat(line.field(1)).isEqualTo("BILL");
            assertThat(line.field(2)).isEqualTo("BNPAFRPP");
        }

        @Test
        void acceptsFeeSubtype() {
            assertThat(mapper.mapLine("ACC;FEE;BNPAFRPP;FR76300040219600001;", 1).field(1))
                    .isEqualTo("FEE");
        }

        @Test
        void rejectsInvalidSubtype() {
            assertThatThrownBy(() -> mapper.mapLine("ACC;UNKNOWN;BIC;IBAN;", 1))
                    .isInstanceOf(ContractFormatException.class)
                    .hasMessageContaining("Sous-type");
        }

        @Test
        void rejectsBlankBic() {
            assertThatThrownBy(() -> mapper.mapLine("ACC;BILL;;FR76300040219600001;", 1))
                    .isInstanceOf(ContractFormatException.class)
                    .hasMessageContaining("BIC");
        }

        @Test
        void rejectsBlankIban() {
            assertThatThrownBy(() -> mapper.mapLine("ACC;BILL;BNPAFRPP;;", 1))
                    .isInstanceOf(ContractFormatException.class)
                    .hasMessageContaining("IBAN");
        }

        @Test
        void rejectsTooFewFields() {
            assertThatThrownBy(() -> mapper.mapLine("ACC;BILL;BIC", 1))
                    .isInstanceOf(ContractFormatException.class)
                    .hasMessageContaining("4 fields");
        }
    }

    // =========================================================================
    // OM — section 4.4
    // =========================================================================

    @Nested
    class OmValidation {

        @Test
        void parsesValidOmLine() {
            BusinessLine line = mapper.mapLine("OM;00058680432692016;000058680432692016", 1);
            assertThat(line.type()).isEqualTo(LineType.OM);
            assertThat(line.field(1)).isEqualTo("00058680432692016");
            assertThat(line.field(2)).isEqualTo("000058680432692016");
        }

        @Test
        void rejectsBlankOmId() {
            assertThatThrownBy(() -> mapper.mapLine("OM;;BR-001", 1))
                    .isInstanceOf(ContractFormatException.class)
                    .hasMessageContaining("OM identifier");
        }

        @Test
        void rejectsBlankBusinessRelationship() {
            assertThatThrownBy(() -> mapper.mapLine("OM;OM-001;", 1))
                    .isInstanceOf(ContractFormatException.class)
                    .hasMessageContaining("BusinessRelationship");
        }
    }

    // =========================================================================
    // OFF — section 4.5
    // =========================================================================

    @Nested
    class OffValidation {

        @Test
        void parsesValidOffLine() {
            BusinessLine line = mapper.mapLine("OFF;OFF-0000000001090;AP00111;Carte VISA PREMIER DI", 1);
            assertThat(line.type()).isEqualTo(LineType.OFF);
            assertThat(line.field(1)).isEqualTo("OFF-0000000001090");
            assertThat(line.field(3)).isEqualTo("Carte VISA PREMIER DI");
        }

        @Test
        void parsesOffWithoutOptionalLabel() {
            BusinessLine line = mapper.mapLine("OFF;OFF-0000000001090;AP00111", 1);
            assertThat(line.type()).isEqualTo(LineType.OFF);
        }

        @Test
        void rejectsBlankOfferId() {
            assertThatThrownBy(() -> mapper.mapLine("OFF;;AP00111", 1))
                    .isInstanceOf(ContractFormatException.class)
                    .hasMessageContaining("Identifiant offre");
        }

        @Test
        void rejectsBlankProvider() {
            assertThatThrownBy(() -> mapper.mapLine("OFF;OFF-001;", 1))
                    .isInstanceOf(ContractFormatException.class)
                    .hasMessageContaining("Provider");
        }
    }

    // =========================================================================
    // ART — section 4.6
    // =========================================================================

    @Nested
    class ArtValidation {

        @Test
        void parsesValidArtLine() {
            BusinessLine line = mapper.mapLine("ART;5", 1);
            assertThat(line.type()).isEqualTo(LineType.ART);
            assertThat(line.field(1)).isEqualTo("5");
        }

        @Test
        void rejectsNonIntegerIndex() {
            assertThatThrownBy(() -> mapper.mapLine("ART;abc", 1))
                    .isInstanceOf(ContractFormatException.class)
                    .hasMessageContaining("integer");
        }

        @Test
        void rejectsZeroIndex() {
            assertThatThrownBy(() -> mapper.mapLine("ART;0", 1))
                    .isInstanceOf(ContractFormatException.class)
                    .hasMessageContaining("positive");
        }

        @Test
        void rejectsNegativeIndex() {
            assertThatThrownBy(() -> mapper.mapLine("ART;-1", 1))
                    .isInstanceOf(ContractFormatException.class)
                    .hasMessageContaining("positive");
        }
    }

    // =========================================================================
    // ROL — section 4.7
    // =========================================================================

    @Nested
    class RolValidation {

        private static final String VALID_ROL = "ROL;1;001;PRI;01970013368500000;01970013368500002";

        @Test
        void parsesValidRolLine() {
            BusinessLine line = mapper.mapLine(VALID_ROL, 1);
            assertThat(line.type()).isEqualTo(LineType.ROL);
            assertThat(line.field(3)).isEqualTo("PRI");
            assertThat(line.field(4)).isEqualTo("01970013368500000");
            assertThat(line.field(5)).isEqualTo("01970013368500002");
        }

        @Test
        void rejectsTooFewFields() {
            assertThatThrownBy(() -> mapper.mapLine("ROL;1;001;PRI;holderID", 1))
                    .isInstanceOf(ContractFormatException.class)
                    .hasMessageContaining("6 fields");
        }

        @Test
        void rejectsBlankRole()     { assertRolFieldRequired(1, "Role"); }
        @Test
        void rejectsBlankBrand()    { assertRolFieldRequired(2, "Brand"); }
        @Test
        void rejectsBlankScope()    { assertRolFieldRequired(3, "Scope"); }
        @Test
        void rejectsBlankHolderId() { assertRolFieldRequired(4, "Holder ID"); }
        @Test
        void rejectsBlankIkpi()     { assertRolFieldRequired(5, "IKPI"); }

        private void assertRolFieldRequired(int idx, String name) {
            String[] f = {"ROL", "1", "001", "PRI", "holderID", "ikpi"};
            f[idx] = "";
            assertThatThrownBy(() -> mapper.mapLine(String.join(";", f), 1))
                    .isInstanceOf(ContractFormatException.class)
                    .hasMessageContaining(name);
        }
    }

    // =========================================================================
    // TAR — section 4.8
    // =========================================================================

    @Nested
    class TarValidation {

        @Test
        void parsesMinimalTarLine() {
            BusinessLine line = mapper.mapLine("TAR", 1);
            assertThat(line.type()).isEqualTo(LineType.TAR);
        }

        @Test
        void parsesFullSpecCompliantTarLine() {
            // formatTarif=001 → tauxTarif + montantBase required
            String tar = "TAR;TARIF_001;001;2026-01-01T00:00:00.000000Z;2026-01-01T00:00:00.000000Z;" +
                         "EUR;1;001;007;001;;10.50;50.00;1.0;;;0;1000.00;1;";
            BusinessLine line = mapper.mapLine(tar, 1);
            assertThat(line.type()).isEqualTo(LineType.TAR);
        }

        @Test
        void rejectsInvalidTypeFraisWhenPresent() {
            assertThatThrownBy(() -> mapper.mapLine("TAR;id;INVALID", 1))
                    .isInstanceOf(ContractFormatException.class)
                    .hasMessageContaining("typeFrais");
        }

        @Test
        void rejectsInvalidFormatTarifWhenPresent() {
            assertThatThrownBy(() -> mapper.mapLine("TAR;id;001;;;EUR;1;999", 1))
                    .isInstanceOf(ContractFormatException.class)
                    .hasMessageContaining("formatTarif");
        }

        @Test
        void rejectsInvalidTypeTauxTarifWhenPresent() {
            // position 11 (index 10)
            assertThatThrownBy(() -> mapper.mapLine("TAR;id;001;;;;;;007;001;INVALID", 1))
                    .isInstanceOf(ContractFormatException.class)
                    .hasMessageContaining("typeTauxTarif");
        }

        @Test
        void rejectsInvalidTypeUniteWhenPresent() {
            // No formatTarif (index 7 blank) so no conditional rules fire.
            // INVALID is placed at index 15 = typeUnite.
            // TAR(0);id(1);(2);(3);(4);(5);(6);(7-empty);007(8);001(9);(10);(11);(12);(13);(14);INVALID(15)
            String tar = "TAR;id;;;;;;;007;001;;;;;;INVALID";
            assertThatThrownBy(() -> mapper.mapLine(tar, 1))
                    .isInstanceOf(ContractFormatException.class)
                    .hasMessageContaining("typeUnite");
        }

        @Test
        void rejectsInvalidIndicLimiteHauteWhenPresent() {
            // position 17 (index 16) = indicLimiteHaute
            String tar = "TAR;id;001;;;;1;001;007;001;;10.50;50.00;;;;2;1000.00;1;";
            assertThatThrownBy(() -> mapper.mapLine(tar, 1))
                    .isInstanceOf(ContractFormatException.class)
                    .hasMessageContaining("indicLimiteHaute");
        }

        @Test
        void conditionalFormatTarif003RequiresTypeTauxTarif() {
            // formatTarif=003 (taux) but typeTauxTarif (index 10) is blank
            String tar = "TAR;id;001;;;;1;003;007;001;";
            assertThatThrownBy(() -> mapper.mapLine(tar, 1))
                    .isInstanceOf(ContractFormatException.class)
                    .hasMessageContaining("typeTauxTarif");
        }

        @Test
        void conditionalFormatTarif001RequiresTauxTarifAndMontantBase() {
            // formatTarif=001 (forfaitaire) but tauxTarif (index 11) is blank
            String tar = "TAR;id;001;;;;1;001;007;001;;";
            assertThatThrownBy(() -> mapper.mapLine(tar, 1))
                    .isInstanceOf(ContractFormatException.class)
                    .hasMessageContaining("tauxTarif");
        }

        @Test
        void conditionalFormatTarif002RequiresMontantUniteAndTypeUnite() {
            // formatTarif=002 (par unité) but montantUnite (index 14) is blank
            String tar = "TAR;id;001;;;;1;002;007;001;;;;;";
            assertThatThrownBy(() -> mapper.mapLine(tar, 1))
                    .isInstanceOf(ContractFormatException.class)
                    .hasMessageContaining("montantUnite");
        }

        @Test
        void ignoresBlankOptionalFields() {
            BusinessLine line = mapper.mapLine("TAR;;;;;;;;;;;;;;;;;;;;;", 1);
            assertThat(line.type()).isEqualTo(LineType.TAR);
        }
    }

}
