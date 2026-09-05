package com.example.jobaggregator.domain;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;

class LineTypeTest {

    @ParameterizedTest
    @ValueSource(strings = {"HDR", "CTR", "ACC", "ROL", "OFF", "OM", "OID", "ART", "IKAC", "COND", "TAR", "AVT", "TRL"})
    void shouldDetermineStandardLineTypeWhenHeaderFieldMatches(String typeName) {
        // Given: An array of fields starting with a recognized standard line prefix
        String[] fields = new String[]{typeName, "extra"};

        // Act: Determine the line type from the parsed fields
        LineType type = LineType.determineFromFields(fields);

        // Assert: The resolved line type matches the expected enum name
        assertThat(type.name()).isEqualTo(typeName);
    }

    @Test
    void shouldDetermineArticleLineTypeWhenArtPrefixProvided() {
        // Given: An array of fields starting with the ART prefix
        String[] fields = new String[]{"ART", "1"};

        // Act: Determine the line type from the parsed fields
        LineType type = LineType.determineFromFields(fields);

        // Assert: Resolved type is LineType.ART
        assertThat(type).isEqualTo(LineType.ART);
    }

    @Test
    void shouldReturnUnknownWhenFieldsAreNullEmptyOrUnrecognized() {
        // Given & Act & Assert: Null, empty, blank, or unrecognized prefix inputs return LineType.UNKNOWN
        assertThat(LineType.determineFromFields(null)).isEqualTo(LineType.UNKNOWN);
        assertThat(LineType.determineFromFields(new String[]{})).isEqualTo(LineType.UNKNOWN);
        assertThat(LineType.determineFromFields(new String[]{""})).isEqualTo(LineType.UNKNOWN);
        assertThat(LineType.determineFromFields(new String[]{"FOOBAR"})).isEqualTo(LineType.UNKNOWN);
    }
}
