package com.example.jobaggregator.reader;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.example.jobaggregator.domain.BusinessLine;
import com.example.jobaggregator.domain.LineType;
import com.example.jobaggregator.error.ContractFormatException;
import java.util.List;
import org.junit.jupiter.api.Test;

/**
 * Unit tests for CTR line field validation in {@link BusinessLineMapper},
 * based on the input file specification (section 4.2 — 16 fields).
 */
class BusinessLineMapperTest {

    private final BusinessLineMapper mapper = new BusinessLineMapper();

    /** A valid 16-field CTR raw line matching the spec. */
    private static final String VALID_CTR =
            "CTR;EUR;16;000;Carte VISA PREMIER;031030000;;BR-00001090;;MENSUELLE;;" +
            "abcdef0123456789;fedcba9876543210;1234567890abcdef;001;WEB";

    @Test
    void parsesValidCtrLine() {
        BusinessLine line = mapper.mapLine(VALID_CTR, 1);
        assertThat(line.type()).isEqualTo(LineType.CTR);
        assertThat(line.field(1)).isEqualTo("EUR");
        assertThat(line.field(2)).isEqualTo("16");
        assertThat(line.field(5)).isEqualTo("031030000");
        assertThat(line.field(7)).isEqualTo("BR-00001090");
        assertThat(line.field(11)).isEqualTo("abcdef0123456789");
        assertThat(line.field(12)).isEqualTo("fedcba9876543210");
        assertThat(line.field(13)).isEqualTo("1234567890abcdef");
        assertThat(line.field(14)).isEqualTo("001");
        assertThat(line.field(15)).isEqualTo("WEB");
    }

    @Test
    void rejectsCtrWithTooFewFields() {
        String tooShort = "CTR;EUR;16";
        assertThatThrownBy(() -> mapper.mapLine(tooShort, 2))
                .isInstanceOf(ContractFormatException.class)
                .hasMessageContaining("CTR requires 16 fields");
    }

    @Test
    void rejectsCtrWithBlankDevise() {
        String line = buildCtr(1, "");
        assertThatThrownBy(() -> mapper.mapLine(line, 3))
                .isInstanceOf(ContractFormatException.class)
                .hasMessageContaining("Devise");
    }

    @Test
    void rejectsCtrWithBlankState() {
        String line = buildCtr(2, "");
        assertThatThrownBy(() -> mapper.mapLine(line, 4))
                .isInstanceOf(ContractFormatException.class)
                .hasMessageContaining("State");
    }

    @Test
    void rejectsCtrWithBlankOuManagement() {
        String line = buildCtr(5, "");
        assertThatThrownBy(() -> mapper.mapLine(line, 5))
                .isInstanceOf(ContractFormatException.class)
                .hasMessageContaining("OuManagement");
    }

    @Test
    void rejectsCtrWithBlankBusinessRelationship() {
        String line = buildCtr(7, "");
        assertThatThrownBy(() -> mapper.mapLine(line, 6))
                .isInstanceOf(ContractFormatException.class)
                .hasMessageContaining("BusinessRelationship");
    }

    @Test
    void rejectsCtrWithInvalidTraceId() {
        String line = buildCtr(11, "not-a-hex-string!");
        assertThatThrownBy(() -> mapper.mapLine(line, 7))
                .isInstanceOf(ContractFormatException.class)
                .hasMessageContaining("X-B3-TraceId");
    }

    @Test
    void rejectsCtrWithTraceIdTooShort() {
        String line = buildCtr(11, "abcdef01");
        assertThatThrownBy(() -> mapper.mapLine(line, 8))
                .isInstanceOf(ContractFormatException.class)
                .hasMessageContaining("X-B3-TraceId");
    }

    @Test
    void rejectsCtrWithInvalidChannel() {
        String line = buildCtr(14, "999");
        assertThatThrownBy(() -> mapper.mapLine(line, 9))
                .isInstanceOf(ContractFormatException.class)
                .hasMessageContaining("Channel");
    }

    @Test
    void rejectsCtrWithBlankMedia() {
        String line = buildCtr(15, "");
        assertThatThrownBy(() -> mapper.mapLine(line, 10))
                .isInstanceOf(ContractFormatException.class)
                .hasMessageContaining("Media");
    }

    @Test
    void acceptsAllValidChannels() {
        for (String channel : List.of("001", "007", "008", "012")) {
            String line = buildCtr(14, channel);
            BusinessLine bl = mapper.mapLine(line, 1);
            assertThat(bl.field(14)).isEqualTo(channel);
        }
    }

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
                .hasMessageContaining("Unknown line code");
    }

    // -----------------------------------------------------------------------
    // Helper
    // -----------------------------------------------------------------------

    /**
     * Builds a valid 16-field CTR raw line, then replaces field at {@code fieldIndex}
     * (1-based per spec; 0-based in the array is fieldIndex) with {@code newValue}.
     */
    private static String buildCtr(int fieldIndex, String newValue) {
        String[] fields = {
            "CTR",                // 0 — Type
            "EUR",                // 1 — Devise
            "16",                 // 2 — State
            "000",                // 3 — Motif (optional)
            "VISA PREMIER",       // 4 — OuDistribution (optional)
            "031030000",          // 5 — OuManagement
            "",                   // 6 — AddressId (optional)
            "BR-00001090",        // 7 — BusinessRelationship
            "",                   // 8 — EffectiveDate (optional)
            "MENSUELLE",          // 9 — PeriodeFacturation (optional)
            "",                   // 10 — DatesFacturation (optional)
            "abcdef0123456789",   // 11 — X-B3-TraceId
            "fedcba9876543210",   // 12 — X-B3-SpanId
            "1234567890abcdef",   // 13 — UserId
            "001",                // 14 — Channel
            "WEB"                 // 15 — Media
        };
        fields[fieldIndex] = newValue;
        return String.join(";", fields);
    }
}
