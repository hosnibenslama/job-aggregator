package com.example.jobaggregator.reader;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.example.jobaggregator.domain.feed.FeedRecordType;
import com.example.jobaggregator.domain.feed.FeedRecord;
import com.example.jobaggregator.error.ContractFormatException;
import java.util.List;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

/**
 * Unit tests for {@link ContractLineMapper} covering all spec-defined line type validators.
 */
class ContractLineMapperTest {

    private final ContractLineMapper mapper = new ContractLineMapper();

    // =========================================================================
    // General parsing
    // =========================================================================

    @Test
    void shouldThrowContractFormatExceptionWhenLineIsBlank() {
        // Given: An empty line string
        String emptyLine = "";

        // Act & Assert: Parser throws ContractFormatException stating line cannot be blank
        assertThatThrownBy(() -> mapper.mapLine(emptyLine, 1))
                .isInstanceOf(ContractFormatException.class)
                .hasMessageContaining("Blank");
    }

    @Test
    void shouldReturnPoisonLineWithUnknownTypeWhenPrefixIsUnrecognized() {
        // Given: A line with an unrecognized prefix
        String lineContent = "XYZ;data";

        // Act: Map the line
        FeedRecord line = mapper.mapLine(lineContent, 1);

        // Assert: Line is recognized as UNKNOWN with lineNumber and raw content preserved
        assertThat(line.type()).isEqualTo(FeedRecordType.UNKNOWN);
        assertThat(line.lineNumber()).isEqualTo(1);
        assertThat(line.raw()).isEqualTo("XYZ;data");
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
        void shouldParseValidCtrLineWhenAllFieldsMeetRequirements() {
            // Given: A fully compliant CTR line
            String rawLine = VALID_CTR;

            // Act: Map the line
            FeedRecord line = mapper.mapLine(rawLine, 1);

            // Assert: Type is CTR and all mapped fields match expected values
            assertThat(line.type()).isEqualTo(FeedRecordType.CTR);
            assertThat(line.field(1)).isEqualTo("EUR");
            assertThat(line.field(5)).isEqualTo("031030000");
            assertThat(line.field(7)).isEqualTo("BR-00001090");
            assertThat(line.field(11)).isEqualTo("abcdef0123456789");
            assertThat(line.field(14)).isEqualTo("001");
            assertThat(line.field(15)).isEqualTo("003");
        }

        @Test
        void shouldThrowContractFormatExceptionWhenCtrLineHasFewerThan16Fields() {
            // Given: A CTR line with fewer than the required 16 fields
            String shortLine = "CTR;EUR;16";

            // Act & Assert: Parser throws ContractFormatException mentioning 16 fields
            assertThatThrownBy(() -> mapper.mapLine(shortLine, 2))
                    .isInstanceOf(ContractFormatException.class)
                    .hasMessageContaining("16 fields");
        }

        @Test
        void shouldRejectCtrWhenDeviseIsBlank() {
            // Given & Act & Assert: CTR line with blank Devise throws ContractFormatException
            assertCtrFieldRequired(1, "Devise");
        }

        @Test
        void shouldRejectCtrWhenStateIsBlank() {
            // Given & Act & Assert: CTR line with blank State throws ContractFormatException
            assertCtrFieldRequired(2, "State");
        }

        @Test
        void shouldRejectCtrWhenOuManagementIsBlank() {
            // Given & Act & Assert: CTR line with blank OuManagement throws ContractFormatException
            assertCtrFieldRequired(5, "OuManagement");
        }

        @Test
        void shouldRejectCtrWhenBusinessRelationshipIsBlank() {
            // Given & Act & Assert: CTR line with blank BusinessRelationship throws ContractFormatException
            assertCtrFieldRequired(7, "BusinessRelationship");
        }

        @Test
        void shouldRejectCtrWhenTraceIdIsNotValidHex() {
            // Given: CTR line with non-hexadecimal trace ID
            String line = buildCtr(11, "not-hex!");

            // Act & Assert: Parser throws ContractFormatException for invalid X-B3-TraceId
            assertThatThrownBy(() -> mapper.mapLine(line, 7))
                    .isInstanceOf(ContractFormatException.class)
                    .hasMessageContaining("X-B3-TraceId");
        }

        @Test
        void shouldRejectCtrWhenTraceIdIsTooShort() {
            // Given: CTR line with trace ID shorter than required 16 hex characters
            String line = buildCtr(11, "abcdef01");

            // Act & Assert: Parser throws ContractFormatException for short X-B3-TraceId
            assertThatThrownBy(() -> mapper.mapLine(line, 8))
                    .isInstanceOf(ContractFormatException.class)
                    .hasMessageContaining("X-B3-TraceId");
        }

        @Test
        void shouldRejectCtrWhenUserIdIsBlank() {
            // Given & Act & Assert: CTR line with blank UserId throws ContractFormatException
            assertCtrFieldRequired(13, "UserId");
        }

        @Test
        void shouldRejectCtrWhenChannelIsInvalid() {
            // Given: CTR line with invalid channel code
            String line = buildCtr(14, "999");

            // Act & Assert: Parser throws ContractFormatException for unknown Channel
            assertThatThrownBy(() -> mapper.mapLine(line, 9))
                    .isInstanceOf(ContractFormatException.class)
                    .hasMessageContaining("Channel");
        }

        @Test
        void shouldAcceptCtrWhenChannelIsAnyAllowedValue() {
            // Given: All valid channels defined in specification
            List<String> validChannels = List.of("001", "007", "008", "012");

            for (String ch : validChannels) {
                // Act: Parse CTR line with each channel
                FeedRecord line = mapper.mapLine(buildCtr(14, ch), 1);

                // Assert: Channel is successfully parsed
                assertThat(line.field(14)).isEqualTo(ch);
            }
        }

        @Test
        void shouldRejectCtrWhenMediaIsInvalid() {
            // Given: CTR line with invalid media code
            String line = buildCtr(15, "WEB");

            // Act & Assert: Parser throws ContractFormatException for unknown Media
            assertThatThrownBy(() -> mapper.mapLine(line, 10))
                    .isInstanceOf(ContractFormatException.class)
                    .hasMessageContaining("Media");
        }

        @Test
        void shouldAcceptCtrWhenMediaIsAnyAllowedCode() {
            // Given: All valid media codes defined in specification
            List<String> validMediaCodes = List.of("001", "003", "055", "073");

            for (String media : validMediaCodes) {
                // Act: Parse CTR line with each media code
                FeedRecord line = mapper.mapLine(buildCtr(15, media), 1);

                // Assert: Media is successfully parsed
                assertThat(line.field(15)).isEqualTo(media);
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
        void shouldParseValidAccLineWhenAllFieldsAreProvided() {
            // Given: A valid ACC line with BILL subtype, BIC, and IBAN
            String rawLine = VALID_ACC;

            // Act: Map the line
            FeedRecord line = mapper.mapLine(rawLine, 1);

            // Assert: Parsed line has ACC type and correct subtype and BIC
            assertThat(line.type()).isEqualTo(FeedRecordType.ACC);
            assertThat(line.field(1)).isEqualTo("BILL");
            assertThat(line.field(2)).isEqualTo("BNPAFRPP");
        }

        @Test
        void shouldAcceptAccWhenSubtypeIsFee() {
            // Given: An ACC line with FEE subtype
            String rawLine = "ACC;FEE;BNPAFRPP;FR76300040219600001;";

            // Act: Map the line
            FeedRecord line = mapper.mapLine(rawLine, 1);

            // Assert: Subtype is FEE
            assertThat(line.field(1)).isEqualTo("FEE");
        }

        @Test
        void shouldRejectAccWhenSubtypeIsInvalid() {
            // Given: An ACC line with invalid subtype UNKNOWN
            String rawLine = "ACC;UNKNOWN;BIC;IBAN;";

            // Act & Assert: Parser throws ContractFormatException for unknown subtype
            assertThatThrownBy(() -> mapper.mapLine(rawLine, 1))
                    .isInstanceOf(ContractFormatException.class)
                    .hasMessageContaining("Sous-type");
        }

        @Test
        void shouldRejectAccWhenBicIsBlank() {
            // Given: An ACC line with blank BIC field
            String rawLine = "ACC;BILL;;FR76300040219600001;";

            // Act & Assert: Parser throws ContractFormatException for blank BIC
            assertThatThrownBy(() -> mapper.mapLine(rawLine, 1))
                    .isInstanceOf(ContractFormatException.class)
                    .hasMessageContaining("BIC");
        }

        @Test
        void shouldRejectAccWhenIbanIsBlank() {
            // Given: An ACC line with blank IBAN field
            String rawLine = "ACC;BILL;BNPAFRPP;;";

            // Act & Assert: Parser throws ContractFormatException for blank IBAN
            assertThatThrownBy(() -> mapper.mapLine(rawLine, 1))
                    .isInstanceOf(ContractFormatException.class)
                    .hasMessageContaining("IBAN");
        }

        @Test
        void shouldRejectAccWhenFieldCountIsLessThanFour() {
            // Given: An ACC line with only 3 fields
            String shortLine = "ACC;BILL;BIC";

            // Act & Assert: Parser throws ContractFormatException requiring at least 4 fields
            assertThatThrownBy(() -> mapper.mapLine(shortLine, 1))
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
        void shouldParseValidOmLineWhenIdentifierAndRelationshipArePresent() {
            // Given: A valid OM line with identifier and BusinessRelationship
            String rawLine = "OM;00058680432692016;000058680432692016";

            // Act: Map the line
            FeedRecord line = mapper.mapLine(rawLine, 1);

            // Assert: OM type is identified with correct fields
            assertThat(line.type()).isEqualTo(FeedRecordType.OM);
            assertThat(line.field(1)).isEqualTo("00058680432692016");
            assertThat(line.field(2)).isEqualTo("000058680432692016");
        }

        @Test
        void shouldRejectOmWhenIdentifierIsBlank() {
            // Given: An OM line with missing OM identifier
            String rawLine = "OM;;BR-001";

            // Act & Assert: Parser throws ContractFormatException
            assertThatThrownBy(() -> mapper.mapLine(rawLine, 1))
                    .isInstanceOf(ContractFormatException.class)
                    .hasMessageContaining("OM identifier");
        }

        @Test
        void shouldRejectOmWhenBusinessRelationshipIsBlank() {
            // Given: An OM line with missing BusinessRelationship
            String rawLine = "OM;OM-001;";

            // Act & Assert: Parser throws ContractFormatException
            assertThatThrownBy(() -> mapper.mapLine(rawLine, 1))
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
        void shouldParseValidOffLineWithMandatoryAndOptionalFields() {
            // Given: A valid OFF line with optional label
            String rawLine = "OFF;OFF-0000000001090;AP00111;Carte VISA PREMIER DI";

            // Act: Map the line
            FeedRecord line = mapper.mapLine(rawLine, 1);

            // Assert: OFF type is identified with all fields
            assertThat(line.type()).isEqualTo(FeedRecordType.OFF);
            assertThat(line.field(1)).isEqualTo("OFF-0000000001090");
            assertThat(line.field(3)).isEqualTo("Carte VISA PREMIER DI");
        }

        @Test
        void shouldParseValidOffLineWithoutOptionalLabel() {
            // Given: A valid OFF line without optional label
            String rawLine = "OFF;OFF-0000000001090;AP00111";

            // Act: Map the line
            FeedRecord line = mapper.mapLine(rawLine, 1);

            // Assert: OFF type is identified successfully
            assertThat(line.type()).isEqualTo(FeedRecordType.OFF);
        }

        @Test
        void shouldRejectOffWhenOfferIdIsBlank() {
            // Given: An OFF line with blank offer ID
            String rawLine = "OFF;;AP00111";

            // Act & Assert: Parser throws ContractFormatException
            assertThatThrownBy(() -> mapper.mapLine(rawLine, 1))
                    .isInstanceOf(ContractFormatException.class)
                    .hasMessageContaining("Identifiant offre");
        }

        @Test
        void shouldRejectOffWhenProviderIsBlank() {
            // Given: An OFF line with blank provider
            String rawLine = "OFF;OFF-001;";

            // Act & Assert: Parser throws ContractFormatException
            assertThatThrownBy(() -> mapper.mapLine(rawLine, 1))
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
        void shouldParseValidArtLineWithPositiveIntegerIndex() {
            // Given: An ART line with a valid positive integer index
            String rawLine = "ART;5";

            // Act: Map the line
            FeedRecord line = mapper.mapLine(rawLine, 1);

            // Assert: ART type is identified with index 5
            assertThat(line.type()).isEqualTo(FeedRecordType.ART);
            assertThat(line.field(1)).isEqualTo("5");
        }

        @Test
        void shouldRejectArtWhenIndexIsNotAnInteger() {
            // Given: An ART line with non-integer index
            String rawLine = "ART;abc";

            // Act & Assert: Parser throws ContractFormatException
            assertThatThrownBy(() -> mapper.mapLine(rawLine, 1))
                    .isInstanceOf(ContractFormatException.class)
                    .hasMessageContaining("integer");
        }

        @Test
        void shouldRejectArtWhenIndexIsZero() {
            // Given: An ART line with index 0
            String rawLine = "ART;0";

            // Act & Assert: Parser throws ContractFormatException requiring strictly positive integer
            assertThatThrownBy(() -> mapper.mapLine(rawLine, 1))
                    .isInstanceOf(ContractFormatException.class)
                    .hasMessageContaining("positive");
        }

        @Test
        void shouldRejectArtWhenIndexIsNegative() {
            // Given: An ART line with negative index
            String rawLine = "ART;-1";

            // Act & Assert: Parser throws ContractFormatException requiring strictly positive integer
            assertThatThrownBy(() -> mapper.mapLine(rawLine, 1))
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
        void shouldParseValidRolLineWithAllMandatoryFields() {
            // Given: A valid ROL line with all required fields
            String rawLine = VALID_ROL;

            // Act: Map the line
            FeedRecord line = mapper.mapLine(rawLine, 1);

            // Assert: ROL line type and fields are mapped correctly
            assertThat(line.type()).isEqualTo(FeedRecordType.ROL);
            assertThat(line.field(3)).isEqualTo("PRI");
            assertThat(line.field(4)).isEqualTo("01970013368500000");
            assertThat(line.field(5)).isEqualTo("01970013368500002");
        }

        @Test
        void shouldRejectRolWhenFieldCountIsLessThanSix() {
            // Given: A ROL line with only 5 fields
            String shortLine = "ROL;1;001;PRI;holderID";

            // Act & Assert: Parser throws ContractFormatException requiring 6 fields
            assertThatThrownBy(() -> mapper.mapLine(shortLine, 1))
                    .isInstanceOf(ContractFormatException.class)
                    .hasMessageContaining("6 fields");
        }

        @Test
        void shouldRejectRolWhenRoleIsBlank() {
            // Given & Act & Assert: Blank role field throws ContractFormatException
            assertRolFieldRequired(1, "Role");
        }

        @Test
        void shouldRejectRolWhenBrandIsBlank() {
            // Given & Act & Assert: Blank brand field throws ContractFormatException
            assertRolFieldRequired(2, "Brand");
        }

        @Test
        void shouldRejectRolWhenScopeIsBlank() {
            // Given & Act & Assert: Blank scope field throws ContractFormatException
            assertRolFieldRequired(3, "Scope");
        }

        @Test
        void shouldRejectRolWhenHolderIdIsBlank() {
            // Given & Act & Assert: Blank holder ID field throws ContractFormatException
            assertRolFieldRequired(4, "Holder ID");
        }

        @Test
        void shouldRejectRolWhenIkpiIsBlank() {
            // Given & Act & Assert: Blank IKPI field throws ContractFormatException
            assertRolFieldRequired(5, "IKPI");
        }

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
        void shouldParseMinimalTarLineWithOnlyPrefix() {
            // Given: A minimal TAR line containing only the prefix
            String rawLine = "TAR";

            // Act: Map the line
            FeedRecord line = mapper.mapLine(rawLine, 1);

            // Assert: Mapped type is TAR
            assertThat(line.type()).isEqualTo(FeedRecordType.TAR);
        }

        @Test
        void shouldParseFullSpecCompliantTarLine() {
            // Given: A complete TAR line with all fields matching specification
            String tar = "TAR;TARIF_001;001;2026-01-01T00:00:00.000000Z;2026-01-01T00:00:00.000000Z;" +
                         "EUR;1;001;007;001;;10.50;50.00;1.0;;;0;1000.00;1;";

            // Act: Map the line
            FeedRecord line = mapper.mapLine(tar, 1);

            // Assert: Mapped type is TAR
            assertThat(line.type()).isEqualTo(FeedRecordType.TAR);
        }

        @Test
        void shouldRejectTarWhenTypeFraisIsInvalid() {
            // Given: A TAR line with an unrecognized typeFrais
            String tar = "TAR;id;INVALID";

            // Act & Assert: Parser throws ContractFormatException for invalid typeFrais
            assertThatThrownBy(() -> mapper.mapLine(tar, 1))
                    .isInstanceOf(ContractFormatException.class)
                    .hasMessageContaining("typeFrais");
        }

        @Test
        void shouldRejectTarWhenFormatTarifIsInvalid() {
            // Given: A TAR line with an unrecognized formatTarif
            String tar = "TAR;id;001;;;EUR;1;999";

            // Act & Assert: Parser throws ContractFormatException for invalid formatTarif
            assertThatThrownBy(() -> mapper.mapLine(tar, 1))
                    .isInstanceOf(ContractFormatException.class)
                    .hasMessageContaining("formatTarif");
        }

        @Test
        void shouldRejectTarWhenTypeTauxTarifIsInvalid() {
            // Given: A TAR line with invalid typeTauxTarif at position 11
            String tar = "TAR;id;001;;;;;;007;001;INVALID";

            // Act & Assert: Parser throws ContractFormatException for invalid typeTauxTarif
            assertThatThrownBy(() -> mapper.mapLine(tar, 1))
                    .isInstanceOf(ContractFormatException.class)
                    .hasMessageContaining("typeTauxTarif");
        }

        @Test
        void shouldRejectTarWhenTypeUniteIsInvalid() {
            // Given: A TAR line with invalid typeUnite at position 16
            String tar = "TAR;id;;;;;;;007;001;;;;;;INVALID";

            // Act & Assert: Parser throws ContractFormatException for invalid typeUnite
            assertThatThrownBy(() -> mapper.mapLine(tar, 1))
                    .isInstanceOf(ContractFormatException.class)
                    .hasMessageContaining("typeUnite");
        }

        @Test
        void shouldRejectTarWhenIndicLimiteHauteIsInvalid() {
            // Given: A TAR line with invalid indicLimiteHaute value 2 at position 17
            String tar = "TAR;id;001;;;;1;001;007;001;;10.50;50.00;;;;2;1000.00;1;";

            // Act & Assert: Parser throws ContractFormatException for invalid indicLimiteHaute
            assertThatThrownBy(() -> mapper.mapLine(tar, 1))
                    .isInstanceOf(ContractFormatException.class)
                    .hasMessageContaining("indicLimiteHaute");
        }

        @Test
        void shouldRejectTarWhenFormatTarif003IsMissingTypeTauxTarif() {
            // Given: formatTarif=003 without required typeTauxTarif
            String tar = "TAR;id;001;;;;1;003;007;001;";

            // Act & Assert: Parser throws ContractFormatException
            assertThatThrownBy(() -> mapper.mapLine(tar, 1))
                    .isInstanceOf(ContractFormatException.class)
                    .hasMessageContaining("typeTauxTarif");
        }

        @Test
        void shouldRejectTarWhenFormatTarif001IsMissingTauxTarifOrMontantBase() {
            // Given: formatTarif=001 without required tauxTarif
            String tar = "TAR;id;001;;;;1;001;007;001;;";

            // Act & Assert: Parser throws ContractFormatException
            assertThatThrownBy(() -> mapper.mapLine(tar, 1))
                    .isInstanceOf(ContractFormatException.class)
                    .hasMessageContaining("tauxTarif");
        }

        @Test
        void shouldRejectTarWhenFormatTarif002IsMissingMontantUniteOrTypeUnite() {
            // Given: formatTarif=002 without required montantUnite
            String tar = "TAR;id;001;;;;1;002;007;001;;;;;";

            // Act & Assert: Parser throws ContractFormatException
            assertThatThrownBy(() -> mapper.mapLine(tar, 1))
                    .isInstanceOf(ContractFormatException.class)
                    .hasMessageContaining("montantUnite");
        }

        @Test
        void shouldAcceptTarWhenOptionalFieldsAreBlank() {
            // Given: A TAR line with trailing blank delimiters
            String tar = "TAR;;;;;;;;;;;;;;;;;;;;;";

            // Act: Map the line
            FeedRecord line = mapper.mapLine(tar, 1);

            // Assert: Line is parsed successfully as TAR
            assertThat(line.type()).isEqualTo(FeedRecordType.TAR);
        }
    }

    // =========================================================================
    // AVT — section 6
    // =========================================================================

    @Nested
    class AvtValidation {

        @Test
        void shouldParseAvtWithCode1AndIdOpra() {
            // Given: An AVT line with code 1 and valid idOpra
            String rawLine = "AVT;OPRA-000000000001;2026-01-01T00:00:00.000000Z;;1;;";

            // Act: Map the line
            FeedRecord line = mapper.mapLine(rawLine, 1);

            // Assert: Type is AVT with idOpra and code 1 preserved
            assertThat(line.type()).isEqualTo(FeedRecordType.AVT);
            assertThat(line.field(1)).isEqualTo("OPRA-000000000001");
            assertThat(line.field(4)).isEqualTo("1");
        }

        @Test
        void shouldParseAvtWithCode2AndValeurAndDevise() {
            // Given: An AVT line with code 2, valeur, and devise
            String rawLine = "AVT;;2026-01-01T00:00:00.000000Z;;2;50.00;EUR";

            // Act: Map the line
            FeedRecord line = mapper.mapLine(rawLine, 1);

            // Assert: Type is AVT with code 2, valeur, and devise mapped correctly
            assertThat(line.type()).isEqualTo(FeedRecordType.AVT);
            assertThat(line.field(4)).isEqualTo("2");
            assertThat(line.field(5)).isEqualTo("50.00");
            assertThat(line.field(6)).isEqualTo("EUR");
        }

        @Test
        void shouldRejectAvtWhenFewerThanFiveFields() {
            // Given: An AVT line with fewer than 5 fields
            String shortLine = "AVT;OPRA;2026-01-01T00:00:00.000000Z;";

            // Act & Assert: Parser throws ContractFormatException requiring 5 fields
            assertThatThrownBy(() -> mapper.mapLine(shortLine, 1))
                    .isInstanceOf(ContractFormatException.class)
                    .hasMessageContaining("5 fields");
        }

        @Test
        void shouldRejectAvtWhenDateDebutIsBlank() {
            // Given: An AVT line with blank dateDebut
            String rawLine = "AVT;OPRA;;2026-12-31T00:00:00.000000Z;1;;";

            // Act & Assert: Parser throws ContractFormatException
            assertThatThrownBy(() -> mapper.mapLine(rawLine, 1))
                    .isInstanceOf(ContractFormatException.class)
                    .hasMessageContaining("dateDebut");
        }

        @Test
        void shouldRejectAvtWhenCodeAvantageIsInvalid() {
            // Given: An AVT line with invalid codeAvantage 5
            String rawLine = "AVT;;2026-01-01T00:00:00.000000Z;;5;;";

            // Act & Assert: Parser throws ContractFormatException
            assertThatThrownBy(() -> mapper.mapLine(rawLine, 1))
                    .isInstanceOf(ContractFormatException.class)
                    .hasMessageContaining("codeAvantage");
        }

        @Test
        void shouldRejectAvtWhenCode1IsMissingIdOpra() {
            // Given: An AVT line with codeAvantage 1 but blank idOpra
            String rawLine = "AVT;;2026-01-01T00:00:00.000000Z;;1;;";

            // Act & Assert: Parser throws ContractFormatException
            assertThatThrownBy(() -> mapper.mapLine(rawLine, 1))
                    .isInstanceOf(ContractFormatException.class)
                    .hasMessageContaining("idOpraAvantage");
        }

        @Test
        void shouldRejectAvtWhenCode2IsMissingValeurAvantage() {
            // Given: An AVT line with codeAvantage 2 but blank valeurAvantage
            String rawLine = "AVT;;2026-01-01T00:00:00.000000Z;;2;;";

            // Act & Assert: Parser throws ContractFormatException
            assertThatThrownBy(() -> mapper.mapLine(rawLine, 1))
                    .isInstanceOf(ContractFormatException.class)
                    .hasMessageContaining("valeurAvantage");
        }

        @Test
        void shouldRejectAvtWhenValeurAvantageIsPresentWithoutDevise() {
            // Given: An AVT line with valeurAvantage but blank deviseAvantage
            String rawLine = "AVT;;2026-01-01T00:00:00.000000Z;;2;50.00;";

            // Act & Assert: Parser throws ContractFormatException
            assertThatThrownBy(() -> mapper.mapLine(rawLine, 1))
                    .isInstanceOf(ContractFormatException.class)
                    .hasMessageContaining("deviseAvantage");
        }

        @Test
        void shouldRejectAvtWhenDeviseIsLongerThanThreeCharacters() {
            // Given: An AVT line with devise longer than 3 characters
            String rawLine = "AVT;;2026-01-01T00:00:00.000000Z;;2;50.00;EURO";

            // Act & Assert: Parser throws ContractFormatException
            assertThatThrownBy(() -> mapper.mapLine(rawLine, 1))
                    .isInstanceOf(ContractFormatException.class)
                    .hasMessageContaining("deviseAvantage");
        }

        @Test
        void shouldAcceptAvtWithCode3And4WhenValeurAndDeviseAreProvided() {
            // Given: Allowed numeric advantage codes 3 and 4 with valeur and devise
            for (String code : List.of("3", "4")) {
                String rawLine = "AVT;;2026-01-01T00:00:00.000000Z;;" + code + ";10.00;USD";

                // Act: Map the line
                FeedRecord line = mapper.mapLine(rawLine, 1);

                // Assert: Advantage code is parsed properly
                assertThat(line.field(4)).isEqualTo(code);
            }
        }
    }

}

