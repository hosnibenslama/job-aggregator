package com.example.jobaggregator.config;

import com.example.jobaggregator.domain.ContractBlock;
import com.example.jobaggregator.domain.feed.FeedRecord;
import com.example.jobaggregator.listener.ContractFileIntegrityListener;
import com.example.jobaggregator.processor.ContractStructureValidator;
import com.example.jobaggregator.reader.ContractBlockReader;
import com.example.jobaggregator.reader.ContractLineMapper;
import com.example.jobaggregator.writer.ContractPersistenceWriter;
import java.nio.charset.Charset;
import org.springframework.batch.core.configuration.annotation.StepScope;
import org.springframework.batch.core.job.Job;
import org.springframework.batch.core.job.builder.JobBuilder;
import org.springframework.batch.core.repository.JobRepository;
import org.springframework.batch.core.step.Step;
import org.springframework.batch.core.step.builder.StepBuilder;
import org.springframework.batch.infrastructure.item.file.FlatFileItemReader;
import org.springframework.batch.infrastructure.item.support.SingleItemPeekableItemReader;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.io.FileSystemResource;
import org.springframework.core.io.Resource;
import org.springframework.core.io.ResourceLoader;
import org.springframework.transaction.PlatformTransactionManager;

@Configuration
@EnableConfigurationProperties(ContractImportProperties.class)
public class ContractImportJobConfig {

    private final Resource inputContractResource;
    private final Charset charset;

    public ContractImportJobConfig(
            ContractImportProperties props,
            ResourceLoader resourceLoader) {
        this.inputContractResource = resolveResource(props.inputFile(), resourceLoader);
        this.charset = props.charset();
    }

    private static Resource resolveResource(String location, ResourceLoader resourceLoader) {
        if (location == null || location.isBlank()) {
            return null;
        }
        if (location.startsWith("classpath:") || location.startsWith("file:")) {
            return resourceLoader.getResource(location);
        }
        return new FileSystemResource(location);
    }

    /**
     * Main contract import job.
     *
     * =========================================================================
     * INTEGRATION POINT: Cloud Object Storage (COS) Tasklets
     * =========================================================================
     * When integrating your existing COS download and upload tasklets into this job:
     *
     * 1. DOWNLOAD TASKLET (runs before the reader/import step):
     *    - Step definition: Create a tasklet step (e.g. `cosDownloadStep`) using your existing download tasklet
     *      that fetches the incoming contract file from COS and stores it locally.
     *    - Wire in the Job flow:
     *        return new JobBuilder("contractImportJob", jobRepository)
     *                .start(cosDownloadStep)
     *                .on("FAILED").fail()
     *                .from(cosDownloadStep).on("*").to(contractImportStep)
     *                .from(contractImportStep).on("FAILED").fail()
     *                .from(contractImportStep).on("*").to(cosUploadStep)
     *                .end()
     *                .build();
     *    - Reader connection: Ensure `peekableLineReader()` (see below) points to the local file
     *      downloaded by the tasklet.
     *
     * 2. UPLOAD TASKLET (runs after the import step):
     *    - Step definition: Create a tasklet step (e.g. `cosUploadStep`) using your existing upload tasklet
     *      to upload the reject file (configured in `contract.import.invalid-file`) back to COS.
     *    - Wire in the Job flow: execute as the final step after `contractImportStep`.
     * =========================================================================
     */
    @Bean
    public Job contractImportJob(JobRepository jobRepository, Step contractImportStep) {
        return new JobBuilder("contractImportJob", jobRepository)
                .start(contractImportStep)
                .build();
    }

    @Bean
    public Step contractImportStep(
            JobRepository jobRepository,
            PlatformTransactionManager transactionManager,
            ContractBlockReader contractItemReader,
            ContractStructureValidator processor,
            ContractPersistenceWriter writer,
            ContractFileIntegrityListener integrityListener) {
        return new StepBuilder("contractImportStep", jobRepository)
                .<ContractBlock, ContractBlock>chunk(100)
                .transactionManager(transactionManager)
                .reader(contractItemReader)
                .processor(processor)
                .writer(writer)
                .listener(integrityListener)
                .build();
    }

    /**
     * Creates the line reader that streams individual semicolon-delimited feed records.
     *
     * =========================================================================
     * INTEGRATION POINT: COS Download Target -> Reader Resource
     * =========================================================================
     * The `flatFileReader` reads from `inputContractResource`.
     * To connect your COS download tasklet here:
     * - Option A (via configuration): Set `contract.import.input-file=file:/path/to/downloaded-contracts.txt`
     *   in application.yml to point to the local file produced by your download tasklet.
     * - Option B (dynamic): If your download tasklet places the file in a fixed staging path or passes
     *   the path in JobExecutionContext, you can inject it here and set:
     *       flatFileReader.setResource(new FileSystemResource(downloadedFilePath));
     * =========================================================================
     */
    @Bean
    @StepScope
    public SingleItemPeekableItemReader<FeedRecord> peekableLineReader() {
        FlatFileItemReader<FeedRecord> flatFileReader = new FlatFileItemReader<>(new ContractLineMapper());
        flatFileReader.setResource(inputContractResource);
        flatFileReader.setEncoding(charset.name());
        flatFileReader.setStrict(true);
        flatFileReader.setComments(new String[]{});

        return new SingleItemPeekableItemReader<>(flatFileReader);
    }

    /**
     * Block reader that aggregates line records into contract hierarchy blocks.
     *
     * Downstream of the download tasklet: This reader consumes the stream produced
     * by `peekableLineReader()` (which reads the downloaded file).
     */
    @Bean
    @StepScope
    public ContractBlockReader contractItemReader(
            SingleItemPeekableItemReader<FeedRecord> peekableLineReader) {
        return new ContractBlockReader(peekableLineReader);
    }
}
