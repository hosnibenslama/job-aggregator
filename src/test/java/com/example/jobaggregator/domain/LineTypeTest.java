package com.example.jobaggregator.domain;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;

class LineTypeTest {

    @ParameterizedTest
    @ValueSource(strings = {"HDR", "CTR", "ACC", "ROL", "OFF", "OM", "OID", "ART", "IKAC", "COND", "TAR", "AVT", "TRL"})
    void determinesStandardLineTypes(String typeName) {
        LineType type = LineType.determineFromFields(new String[]{typeName, "extra"});
        assertThat(type.name()).isEqualTo(typeName);
    }

    @Test
    void determinesArticleLineType() {
        LineType type = LineType.determineFromFields(new String[]{"ART", "1"});
        assertThat(type).isEqualTo(LineType.ART);
    }

    @Test
    void returnsUnknownForUnrecognizedOrEmptyFields() {
        assertThat(LineType.determineFromFields(null)).isEqualTo(LineType.UNKNOWN);
        assertThat(LineType.determineFromFields(new String[]{})).isEqualTo(LineType.UNKNOWN);
        assertThat(LineType.determineFromFields(new String[]{""})).isEqualTo(LineType.UNKNOWN);
        assertThat(LineType.determineFromFields(new String[]{"FOOBAR"})).isEqualTo(LineType.UNKNOWN);
    }
}
