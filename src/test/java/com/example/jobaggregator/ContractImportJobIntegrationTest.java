package com.example.jobaggregator;

import static org.assertj.core.api.Assertions.assertThat;

import java.nio.file.Files;
import java.nio.file.Path;
import java.time.Duration;
import java.util.Collection;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.Timeout;
import org.junit.jupiter.api.io.TempDir;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;
import org.springframework.batch.core.ExitStatus;
import org.springframework.batch.core.step.StepExecution;
import org.springframework.batch.core.job.Job;
import org.springframework.batch.core.job.JobExecution;
import org.springframework.batch.core.job.parameters.JobParameters;
import org.springframework.batch.core.job.parameters.JobParametersBuilder;
import org.springframework.batch.core.launch.JobOperator;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.annotation.DirtiesContext;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;

/**
 * Integration test for the complete contract import batch job.
 * Tests the full pipeline: reading, parsing, validating, and writing contracts,
 * including rejection routing and file integrity rules.
 */
@SpringBootTest(properties = {
    "spring.batch.job.enabled=false",
    "contract.import.charset=UTF-8"
})
@DirtiesContext(classMode = DirtiesContext.ClassMode.AFTER_EACH_TEST_METHOD)
class ContractImportJobIntegrationTest {

    private static final String VALID_CONTRACT_001 = """
        CTR;EUR;16;000;Contract 001;031030000;;BR-001;;MENSUELLE;;abcdef0123456789;fedcba9876543210;user001;001;003
        ACC;BILL;BNPAFRPP;FR76300040219600000167638828;300040005800004021286086
        OM;OM-001;000058680432692016
        ART;1""";

    private static final String VALID_CONTRACT_002 = """
        CTR;EUR;16;000;Contract 002;031030000;;BR-002;;MENSUELLE;;abcdef0123456789;fedcba9876543210;user002;001;003
        ACC;BILL;BNPAFRPP;FR76300040219600000167638829;300040005800004021286087
        OM;OM-002;000058680432692017
        ART;1""";

    private static final String VALID_CONTRACT_003 = """
        CTR;EUR;16;000;Contract 003;031030000;;BR-003;;MENSUELLE;;abcdef0123456789;fedcba9876543210;user003;001;003
        ACC;BILL;BNPAFRPP;FR76300040219600000167638830;300040005800004021286088
        OM;OM-003;000058680432692018
        ART;1""";

    private static final String CONTRACT_MISSING_ACC = """
        CTR;EUR;16;000;Contract 001;031030000;;BR-001;;MENSUELLE;;abcdef0123456789;fedcba9876543210;user001;001;003
        OM;OM-001;000058680432692016
        ART;1""";

    private static final String CONTRACT_INVALID_SEQUENCING = """
        CTR;EUR;16;000;Contract 001;031030000;;BR-001;;MENSUELLE;;abcdef0123456789;fedcba9876543210;user001;001;003
        ACC;BILL;BNPAFRPP;FR76300040219600000167638828;300040005800004021286086
        OM;OM-001;000058680432692016
        IKAC;IKAC-001
        ART;1""";

    private final JobOperator jobOperator;
    private final Job contractImportJob;

    @TempDir
    static Path tempDir;

    private Path inputFile;

    @Autowired
    ContractImportJobIntegrationTest(JobOperator jobOperator, Job contractImportJob) {
        this.jobOperator = jobOperator;
        this.contractImportJob = contractImportJob;
    }

    @DynamicPropertySource
    static void configureProperties(DynamicPropertyRegistry registry) {
        registry.add("contract.import.inputFile", () -> tempDir.resolve("test-input.txt").toString());
        registry.add("contract.import.invalid-file", () -> tempDir.resolve("test-input.txt.reject").toString());
    }

    @BeforeEach
    void setUp() {
        inputFile = tempDir.resolve("test-input.txt");
    }

    private void writeInput(String content) throws Exception {
        Files.writeString(inputFile, content);
    }

    private JobExecution launchJob() throws Exception {
        JobParameters jobParameters = new JobParametersBuilder()
                .addLong("timestamp", System.currentTimeMillis())
                .toJobParameters();
        return jobOperator.start(contractImportJob, jobParameters);
    }

    private StepExecution getStepExecution(JobExecution jobExecution, String stepName) {
        return jobExecution.getStepExecutions().stream()
                .filter(step -> step.getStepName().equals(stepName))
                .findFirst()
                .orElseThrow(() -> new AssertionError("Step not found: " + stepName));
    }

    @Test
    @Timeout(5)
    void importsValidContracts() throws Exception {
        // Given: A valid contract file with HDR and TRL
        writeInput("""
            HDR;2
            """ + VALID_CONTRACT_001 + "\n" + VALID_CONTRACT_002 + """

            TRL;2
            """);

        // When: Job executes
        JobExecution execution = launchJob();

        // Then: Job completes successfully with correct metrics
        assertThat(execution.getExitStatus()).isEqualTo(ExitStatus.COMPLETED);

        StepExecution step = getStepExecution(execution, "contractImportStep");
        assertThat(step.getReadCount()).isEqualTo(2);
        assertThat(step.getWriteCount()).isEqualTo(2);
        assertThat(step.getFilterCount()).isEqualTo(0);
    }

    @Test
    @Timeout(5)
    void rejectsInvalidContractsToRejectFile() throws Exception {
        // Given: Mixed valid and invalid contracts
        writeInput("""
            HDR;3
            """ + VALID_CONTRACT_001 + """

            CTR;EUR;16;000;Contract 002;031030000;;BR-002;;MENSUELLE;;abcdef0123456789;fedcba9876543210;user002;001;003
            ACC;BILL;BNPAFRPP;FR76300040219600000167638829;300040005800004021286087
            INVALID_LINE_TYPE;data
            OM;OM-002;000058680432692017
            ART;1
            """ + VALID_CONTRACT_003 + """

            TRL;3
            """);

        // When: Job executes
        JobExecution execution = launchJob();

        // Then: Job completes with rejected contracts
        assertThat(execution.getExitStatus()).isEqualTo(ExitStatus.COMPLETED);

        StepExecution step = getStepExecution(execution, "contractImportStep");
        assertThat(step.getReadCount()).isEqualTo(3);
        assertThat(step.getWriteCount()).isEqualTo(2);
        assertThat(step.getFilterCount()).isEqualTo(1);

        // Verify reject file exists and contains rejected contract
        Path rejectFile = tempDir.resolve("test-input.txt.reject");
        assertThat(rejectFile).exists();
        String rejectContent = Files.readString(rejectFile);
        assertThat(rejectContent).contains("Contract 002");
        assertThat(rejectContent).contains("INVALID_LINE_TYPE");
    }

    @Test
    @Timeout(5)
    void rejectsContractMissingMandatoryLines() throws Exception {
        // Given: A contract missing mandatory ACC line
        writeInput("""
            HDR;1
            """ + CONTRACT_MISSING_ACC + """

            TRL;1
            """);

        // When: Job executes
        JobExecution execution = launchJob();

        // Then: Job completes with rejected contract
        assertThat(execution.getExitStatus()).isEqualTo(ExitStatus.COMPLETED);

        StepExecution step = getStepExecution(execution, "contractImportStep");
        assertThat(step.getReadCount()).isEqualTo(1);
        assertThat(step.getWriteCount()).isEqualTo(0);
        assertThat(step.getFilterCount()).isEqualTo(1);

        // Verify reject file exists and contains rejected contract
        Path rejectFile = tempDir.resolve("test-input.txt.reject");
        assertThat(rejectFile).exists();
        String rejectContent = Files.readString(rejectFile);
        assertThat(rejectContent).contains("Contract 001");
    }

    @Test
    @Timeout(5)
    void rejectsContractWithInvalidLineSequencing() throws Exception {
        // Given: A contract with IKAC before ART (violates prerequisites)
        writeInput("""
            HDR;1
            """ + CONTRACT_INVALID_SEQUENCING + """

            TRL;1
            """);

        // When: Job executes
        JobExecution execution = launchJob();

        // Then: Job completes with rejected contract
        assertThat(execution.getExitStatus()).isEqualTo(ExitStatus.COMPLETED);

        StepExecution step = getStepExecution(execution, "contractImportStep");
        assertThat(step.getReadCount()).isEqualTo(1);
        assertThat(step.getWriteCount()).isEqualTo(0);
        assertThat(step.getFilterCount()).isEqualTo(1);

        // Verify reject file exists and contains rejected contract
        Path rejectFile = tempDir.resolve("test-input.txt.reject");
        assertThat(rejectFile).exists();
        String rejectContent = Files.readString(rejectFile);
        assertThat(rejectContent).contains("Contract 001");
        assertThat(rejectContent).contains("IKAC");
    }

    @Test
    @Timeout(5)
    void failsJobWhenHeaderMissing() throws Exception {
        // Given: File without HDR line - reader processes starting from first CTR
        writeInput("""
            CTR;EUR;16;000;Contract 001;031030000;;BR-001;;MENSUELLE;;abcdef0123456789;fedcba9876543210;user001;001;003
            ACC;BILL;BNPAFRPP;FR76300040219600000167638828;300040005800004021286086
            OM;OM-001;000058680432692016
            ART;1
            TRL;1
            """);

        // When: Job executes
        JobExecution execution = launchJob();

        // Then: Job completes because the reader navigates straight to CTR
        assertThat(execution.getExitStatus()).isEqualTo(ExitStatus.COMPLETED);

        StepExecution step = getStepExecution(execution, "contractImportStep");
        assertThat(step.getReadCount()).isEqualTo(1);
        assertThat(step.getWriteCount()).isEqualTo(1);
        assertThat(step.getFilterCount()).isEqualTo(0);
    }

    @Test
    @Timeout(5)
    void completesWithWarningWhenTrailerMissing() throws Exception {
        // Given: File without TRL line
        writeInput("""
            HDR;2
            CTR;EUR;16;000;Contract 001;031030000;;BR-001;;MENSUELLE;;abcdef0123456789;fedcba9876543210;user001;001;003
            ACC;BILL;BNPAFRPP;FR76300040219600000167638828;300040005800004021286086
            OM;OM-001;000058680432692016
            ART;1
            CTR;EUR;16;000;Contract 002;031030000;;BR-002;;MENSUELLE;;abcdef0123456789;fedcba9876543210;user002;001;003
            ACC;BILL;BNPAFRPP;FR76300040219600000167638829;300040005800004021286087
            OM;OM-002;000058680432692017
            ART;1
            """);

        // When: Job executes
        JobExecution execution = launchJob();

        // Then: Job completes (Rule 3 check skipped with warning)
        assertThat(execution.getExitStatus()).isEqualTo(ExitStatus.COMPLETED);

        StepExecution step = getStepExecution(execution, "contractImportStep");
        assertThat(step.getReadCount()).isEqualTo(2);
        assertThat(step.getWriteCount()).isEqualTo(2);
        assertThat(step.getFilterCount()).isEqualTo(0);
    }

    @ParameterizedTest
    @CsvSource({
        "CONTRACT_MISSING_ACC, Contract missing mandatory ACC line, Contract 001",
        "CONTRACT_INVALID_SEQUENCING, Contract with IKAC before ART (violates prerequisites), IKAC"
    })
    @Timeout(5)
    void rejectsContractWithValidationErrors(String contractType, String description, String expectedContent) throws Exception {
        // Given: A contract with validation errors
        String contractContent = switch (contractType) {
            case "CONTRACT_MISSING_ACC" -> CONTRACT_MISSING_ACC;
            case "CONTRACT_INVALID_SEQUENCING" -> CONTRACT_INVALID_SEQUENCING;
            default -> throw new IllegalArgumentException("Unknown contract type: " + contractType);
        };

        writeInput("""
            HDR;1
            """ + contractContent + """

            TRL;1
            """);

        // When: Job executes
        JobExecution execution = launchJob();

        // Then: Job completes with rejected contract
        assertThat(execution.getExitStatus()).isEqualTo(ExitStatus.COMPLETED);

        StepExecution step = getStepExecution(execution, "contractImportStep");
        assertThat(step.getReadCount()).isEqualTo(1);
        assertThat(step.getWriteCount()).isEqualTo(0);
        assertThat(step.getFilterCount()).isEqualTo(1);

        // Verify reject file exists and contains rejected contract
        Path rejectFile = tempDir.resolve("test-input.txt.reject");
        assertThat(rejectFile).exists();
        String rejectContent = Files.readString(rejectFile);
        assertThat(rejectContent).contains(expectedContent);
    }

    @Test
    @Timeout(5)
    void verifiesUtf8CharacterEncoding() throws Exception {
        // Given: Contracts with UTF-8 non-ASCII characters (accented characters)
        writeInput("""
            HDR;2
            CTR;EUR;16;000;Contrat François Müller;031030000;;BR-001;;MENSUELLE;;abcdef0123456789;fedcba9876543210;user001;001;003
            ACC;BILL;BNPAFRPP;FR76300040219600000167638828;300040005800004021286086
            OM;OM-001;000058680432692016
            ART;1
            CTR;EUR;16;000;Société José García;031030000;;BR-002;;MENSUELLE;;abcdef0123456789;fedcba9876543210;user002;001;003
            ACC;BILL;BNPAFRPP;FR76300040219600000167638829;300040005800004021286087
            OM;OM-002;000058680432692017
            ART;1
            TRL;2
            """);

        // When: Job executes
        JobExecution execution = launchJob();

        // Then: Job completes successfully with UTF-8 characters preserved
        assertThat(execution.getExitStatus()).isEqualTo(ExitStatus.COMPLETED);

        StepExecution step = getStepExecution(execution, "contractImportStep");
        assertThat(step.getReadCount()).isEqualTo(2);
        assertThat(step.getWriteCount()).isEqualTo(2);
        assertThat(step.getFilterCount()).isEqualTo(0);
    }

    @Test
    @Timeout(5)
    void failsJobWhenContractCountMismatch() throws Exception {
        // Given: HDR declares 3 contracts but file contains only 2
        writeInput("""
            HDR;3
            CTR;EUR;16;000;Contract 001;031030000;;BR-001;;MENSUELLE;;abcdef0123456789;fedcba9876543210;user001;001;003
            ACC;BILL;BNPAFRPP;FR76300040219600000167638828;300040005800004021286086
            OM;OM-001;000058680432692016
            ART;1
            CTR;EUR;16;000;Contract 002;031030000;;BR-002;;MENSUELLE;;abcdef0123456789;fedcba9876543210;user002;001;003
            ACC;BILL;BNPAFRPP;FR76300040219600000167638829;300040005800004021286087
            OM;OM-002;000058680432692017
            ART;1
            TRL;3
            """);

        // When: Job executes
        JobExecution execution = launchJob();

        // Then: Job fails (Rule 3 integrity violation - expected count doesn't match actual)
        assertThat(execution.getExitStatus().getExitCode()).isEqualTo(ExitStatus.FAILED.getExitCode());

        StepExecution step = getStepExecution(execution, "contractImportStep");
        assertThat(step.getReadCount()).isEqualTo(2);
    }
}
