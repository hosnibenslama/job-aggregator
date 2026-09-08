package com.bnpparibas.dsibddf.ap23992.modelevente.batch.injector.domain.feed;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.List;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;

class FeedRecordTypeTest {

    @ParameterizedTest
    @ValueSource(strings = {"HDR", "CTR", "ACC", "ROL", "OFF", "OM", "OID", "ART", "IKAC", "COND", "TAR", "AVT", "TRL"})
    void shouldDetermineStandardRecordTypeWhenHeaderFieldMatches(String typeName) {
        // Given: A list of fields starting with a recognized standard record prefix
        List<String> fields = List.of(typeName, "extra");

        // Act: Determine the record type from the parsed fields
        FeedRecordType type = FeedRecordType.determineFromFields(fields);

        // Assert: The resolved record type matches the expected enum name
        assertThat(type.name()).isEqualTo(typeName);
    }

    @Test
    void shouldDetermineArticleRecordTypeWhenArtPrefixProvided() {
        // Given: A list of fields starting with the ART prefix
        List<String> fields = List.of("ART", "1");

        // Act: Determine the record type from the parsed fields
        FeedRecordType type = FeedRecordType.determineFromFields(fields);

        // Assert: Resolved type is FeedRecordType.ART
        assertThat(type).isEqualTo(FeedRecordType.ART);
    }

    @Test
    void shouldReturnUnknownWhenFieldsAreNullEmptyOrUnrecognized() {
        // Given & Act & Assert: Null, empty, blank, or unrecognized prefix inputs return FeedRecordType.UNKNOWN
        assertThat(FeedRecordType.determineFromFields(null)).isEqualTo(FeedRecordType.UNKNOWN);
        assertThat(FeedRecordType.determineFromFields(List.of())).isEqualTo(FeedRecordType.UNKNOWN);
        assertThat(FeedRecordType.determineFromFields(List.of(""))).isEqualTo(FeedRecordType.UNKNOWN);
        assertThat(FeedRecordType.determineFromFields(List.of("FOOBAR"))).isEqualTo(FeedRecordType.UNKNOWN);
    }
}
