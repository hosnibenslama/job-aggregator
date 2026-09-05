package com.example.jobaggregator.config;

import com.example.jobaggregator.domain.Contract;
import com.example.jobaggregator.domain.ParsedLine;
import com.example.jobaggregator.listener.ContractFileIntegrityListener;
import com.example.jobaggregator.processor.ContractStructureValidator;
import com.example.jobaggregator.reader.ContractBlockReader;
import com.example.jobaggregator.reader.SemicolonLineParser;
import com.example.jobaggregator.storage.CloudObjectStorageService;
import com.example.jobaggregator.tasklet.CosDownloadTasklet;
import com.example.jobaggregator.tasklet.CosUploadTasklet;
import com.example.jobaggregator.writer.ContractPersistenceWriter;
import java.nio.charset.Charset;
import java.nio.file.Path;
import org.springframework.batch.core.configuration.annotation.StepScope;
import org.springframework.batch.core.job.Job;
import org.springframework.batch.core.job.builder.JobBuilder;
import org.springframework.batch.core.repository.JobRepository;
import org.springframework.batch.core.step.Step;
import org.springframework.batch.core.step.builder.StepBuilder;
import org.springframework.batch.infrastructure.item.file.FlatFileItemReader;
import org.springframework.batch.infrastructure.item.support.SingleItemPeekableItemReader;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.io.FileSystemResource;
import org.springframework.core.io.Resource;
import org.springframework.core.io.ResourceLoader;
import org.springframework.transaction.PlatformTransactionManager;

@Configuration
@EnableConfigurationProperties({
        com.example.jobaggregator.config.ContractImportProperties.class,
        com.example.jobaggregator.config.CosProperties.class
})
public class ContractImportJobConfig {

    private final Resource inputContractResource;
    private final Charset charset;
    private final Path localStagingInputFile;

    public ContractImportJobConfig(
            ContractImportProperties props,
            CosProperties cosProps,
            ResourceLoader resourceLoader) {
        this.localStagingInputFile = cosProps.stagingDirectory().resolve("contracts-input.txt");
        if (cosProps.enabled()) {
            this.inputContractResource = new FileSystemResource(this.localStagingInputFile);
        } else {
            this.inputContractResource = resolveResource(props.inputFile(), resourceLoader);
        }
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

    @Bean
    public Job contractImportJob(
            JobRepository jobRepository,
            Step cosDownloadStep,
            Step contractImportStep,
            Step cosUploadStep) {
        return new JobBuilder("contractImportJob", jobRepository)
                .start(cosDownloadStep)
                .on("FAILED").fail()
                .from(cosDownloadStep).on("*").to(contractImportStep)
                .from(contractImportStep).on("FAILED").fail()
                .from(contractImportStep).on("*").to(cosUploadStep)
                .end()
                .build();
    }

    @Bean
    public Step cosDownloadStep(
            JobRepository jobRepository,
            PlatformTransactionManager transactionManager,
            CloudObjectStorageService storageService,
            CosProperties cosProperties) {
        return new StepBuilder("cosDownloadStep", jobRepository)
                .tasklet(new CosDownloadTasklet(storageService, cosProperties, localStagingInputFile), transactionManager)
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
                .<Contract, Contract>chunk(100)
                .transactionManager(transactionManager)
                .reader(contractItemReader)
                .processor(processor)
                .writer(writer)
                .listener(integrityListener)
                .build();
    }

    @Bean
    public Step cosUploadStep(
            JobRepository jobRepository,
            PlatformTransactionManager transactionManager,
            CloudObjectStorageService storageService,
            CosProperties cosProperties,
            @Value("${contract.import.invalid-file:src/main/resources/invalid-contracts.txt}") String rejectFile) {
        return new StepBuilder("cosUploadStep", jobRepository)
                .tasklet(new CosUploadTasklet(storageService, cosProperties, Path.of(rejectFile)), transactionManager)
                .build();
    }

    @Bean
    @StepScope
    public SingleItemPeekableItemReader<ParsedLine> peekableLineReader() {
        FlatFileItemReader<ParsedLine> flatFileReader = new FlatFileItemReader<>(new SemicolonLineParser());
        flatFileReader.setResource(inputContractResource);
        flatFileReader.setEncoding(charset.name());
        flatFileReader.setStrict(true);
        flatFileReader.setComments(new String[]{});

        return new SingleItemPeekableItemReader<>(flatFileReader);
    }

    @Bean
    @StepScope
    public ContractBlockReader contractItemReader(
            SingleItemPeekableItemReader<ParsedLine> peekableLineReader) {
        return new ContractBlockReader(peekableLineReader);
    }
}
