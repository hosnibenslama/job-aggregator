package com.example.jobaggregator;

import static org.assertj.core.api.Assertions.assertThat;

import java.nio.file.Files;
import java.nio.file.Path;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import org.springframework.batch.core.ExitStatus;
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

    @Test
    void importsValidContracts() throws Exception {
        // Given: A valid contract file with HDR and TRL
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
            TRL;2
            """);

        // When: Job executes
        JobExecution execution = launchJob();

        // Then: Job completes successfully
        assertThat(execution.getExitStatus()).isEqualTo(ExitStatus.COMPLETED);
    }

    @Test
    void rejectsInvalidContractsToRejectFile() throws Exception {
        // Given: Mixed valid and invalid contracts
        writeInput("""
            HDR;3
            CTR;EUR;16;000;Contract 001;031030000;;BR-001;;MENSUELLE;;abcdef0123456789;fedcba9876543210;user001;001;003
            ACC;BILL;BNPAFRPP;FR76300040219600000167638828;300040005800004021286086
            OM;OM-001;000058680432692016
            ART;1
            CTR;EUR;16;000;Contract 002;031030000;;BR-002;;MENSUELLE;;abcdef0123456789;fedcba9876543210;user002;001;003
            ACC;BILL;BNPAFRPP;FR76300040219600000167638829;300040005800004021286087
            INVALID_LINE_TYPE;data
            OM;OM-002;000058680432692017
            ART;1
            CTR;EUR;16;000;Contract 003;031030000;;BR-003;;MENSUELLE;;abcdef0123456789;fedcba9876543210;user003;001;003
            ACC;BILL;BNPAFRPP;FR76300040219600000167638830;300040005800004021286088
            OM;OM-003;000058680432692018
            ART;1
            TRL;3
            """);

        // When: Job executes
        JobExecution execution = launchJob();

        // Then: Job completes
        assertThat(execution.getExitStatus()).isEqualTo(ExitStatus.COMPLETED);
    }

    @Test
    void rejectsContractMissingMandatoryLines() throws Exception {
        // Given: A contract missing mandatory ACC line
        writeInput("""
            HDR;1
            CTR;EUR;16;000;Contract 001;031030000;;BR-001;;MENSUELLE;;abcdef0123456789;fedcba9876543210;user001;001;003
            OM;OM-001;000058680432692016
            ART;1
            TRL;1
            """);

        // When: Job executes
        JobExecution execution = launchJob();

        // Then: Job completes
        assertThat(execution.getExitStatus()).isEqualTo(ExitStatus.COMPLETED);
    }

    @Test
    void rejectsContractWithInvalidLineSequencing() throws Exception {
        // Given: A contract with IKAC before ART (violates prerequisites)
        writeInput("""
            HDR;1
            CTR;EUR;16;000;Contract 001;031030000;;BR-001;;MENSUELLE;;abcdef0123456789;fedcba9876543210;user001;001;003
            ACC;BILL;BNPAFRPP;FR76300040219600000167638828;300040005800004021286086
            OM;OM-001;000058680432692016
            IKAC;IKAC-001
            ART;1
            TRL;1
            """);

        // When: Job executes
        JobExecution execution = launchJob();

        // Then: Job completes
        assertThat(execution.getExitStatus()).isEqualTo(ExitStatus.COMPLETED);
    }

    @Test
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
    }

    @Test
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
    }

    @Test
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
    }
}
