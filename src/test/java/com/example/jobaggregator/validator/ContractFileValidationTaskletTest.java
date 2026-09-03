package com.example.jobaggregator.validator;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.example.jobaggregator.error.ContractFormatException;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import org.springframework.batch.infrastructure.repeat.RepeatStatus;

class ContractFileValidationTaskletTest {

    @TempDir
    Path tempDir;

    @Test
    void acceptsValidFile() throws Exception {
        Path file = tempDir.resolve("valid.txt");
        Files.writeString(file, """
                HDR;20260415;LOT01;VERSION1
                CTR;16;EUR;000;Carte VISA PREMIER Di;003;00058680
                  ACC;BILL;BNPAFRPP;FR76300040219600000167638828;300040005800004021286086
                  ROL;1;001;PRI016000078925000000;01600007892500000
                  OFF;OFF-0000000001090;AP00111
                  OM;00058680432692016;000058680432692016
                    OID;PRO-0000000000557
                    ART;1
                TRL;1;8
                """, StandardCharsets.UTF_8);

        ContractFileValidationTasklet tasklet = new ContractFileValidationTasklet(file, StandardCharsets.UTF_8);
        RepeatStatus status = tasklet.execute(null, null);

        assertThat(status).isEqualTo(RepeatStatus.FINISHED);
    }

    @Test
    void rejectsFileWithoutHeader() throws IOException {
        Path file = tempDir.resolve("no-header.txt");
        Files.writeString(file, """
                CTR;16;EUR
                ACC;BILL
                TRL;1;2
                """, StandardCharsets.UTF_8);

        ContractFileValidationTasklet tasklet = new ContractFileValidationTasklet(file, StandardCharsets.UTF_8);

        assertThatThrownBy(() -> tasklet.execute(null, null))
                .isInstanceOf(ContractFormatException.class)
                .hasMessageContaining("Missing HDR");
    }

    @Test
    void rejectsFileWithoutTrailer() throws IOException {
        Path file = tempDir.resolve("no-trailer.txt");
        Files.writeString(file, """
                HDR;20260415;LOT01;VERSION1
                CTR;16;EUR;000;Carte VISA PREMIER Di;003;00058680
                  ACC;BILL;BNPAFRPP;FR76300040219600000167638828;300040005800004021286086
                  OM;00058680432692016;000058680432692016
                  ART;1
                """, StandardCharsets.UTF_8);

        ContractFileValidationTasklet tasklet = new ContractFileValidationTasklet(file, StandardCharsets.UTF_8);

        assertThatThrownBy(() -> tasklet.execute(null, null))
                .isInstanceOf(ContractFormatException.class)
                .hasMessageContaining("Missing TRL");
    }
}
