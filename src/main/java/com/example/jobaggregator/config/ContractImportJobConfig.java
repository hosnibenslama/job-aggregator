package com.example.jobaggregator.config;

import com.example.jobaggregator.domain.BusinessLine;
import com.example.jobaggregator.domain.Contract;
import com.example.jobaggregator.processor.ContractProcessor;
import com.example.jobaggregator.reader.BusinessLineMapper;
import com.example.jobaggregator.reader.ContractFileReader;
import com.example.jobaggregator.validator.ContractFileValidationTasklet;
import com.example.jobaggregator.writer.ContractJdbcWriter;
import java.nio.charset.Charset;
import java.nio.file.Path;
import org.springframework.batch.core.configuration.annotation.StepScope;
import org.springframework.batch.core.job.Job;
import org.springframework.batch.core.job.builder.JobBuilder;
import org.springframework.batch.core.repository.JobRepository;
import org.springframework.batch.core.step.Step;
import org.springframework.batch.core.step.builder.StepBuilder;
import org.springframework.batch.core.step.tasklet.TaskletStep;
import org.springframework.batch.infrastructure.item.file.FlatFileItemReader;
import org.springframework.batch.infrastructure.item.support.SingleItemPeekableItemReader;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.io.FileSystemResource;
import org.springframework.transaction.PlatformTransactionManager;

@Configuration
@EnableConfigurationProperties(ContractImportProperties.class)
public class ContractImportJobConfig {

    private final Path inputContractFile;
    private final Charset charset;

    public ContractImportJobConfig(ContractImportProperties props) {
        this.inputContractFile = props.inputFile();
        this.charset = props.charset();
    }

    @Bean
    public Job contractImportJob(JobRepository jobRepository, Step validationStep, Step contractImportStep) {
        return new JobBuilder("contractImportJob", jobRepository)
                .start(validationStep)
                .next(contractImportStep)
                .build();
    }

    @Bean
    public TaskletStep validationStep(
            JobRepository jobRepository,
            PlatformTransactionManager transactionManager) {
        ContractFileValidationTasklet tasklet = new ContractFileValidationTasklet(
                inputContractFile,
                charset);
        return new StepBuilder("validationStep", jobRepository)
                .tasklet(tasklet, transactionManager)
                .build();
    }

    @Bean
    public Step contractImportStep(
            JobRepository jobRepository,
            PlatformTransactionManager transactionManager,
            ContractFileReader contractItemReader,
            ContractProcessor processor,
            ContractJdbcWriter writer) {
        return new StepBuilder("contractImportStep", jobRepository)
                .<Contract, Contract>chunk(100, transactionManager)
                .reader(contractItemReader)
                .processor(processor)
                .writer(writer)
                .build();
    }

    @Bean
    @StepScope
    public SingleItemPeekableItemReader<BusinessLine> peekableLineReader() {
        FlatFileItemReader<BusinessLine> flatFileReader = new FlatFileItemReader<>(new BusinessLineMapper());
        flatFileReader.setResource(new FileSystemResource(inputContractFile));
        flatFileReader.setEncoding(charset.name());
        flatFileReader.setStrict(true);
        flatFileReader.setComments(new String[]{});

        return new SingleItemPeekableItemReader<>(flatFileReader);
    }

    @Bean
    @StepScope
    public ContractFileReader contractItemReader(SingleItemPeekableItemReader<BusinessLine> peekableLineReader) {
        return new ContractFileReader(peekableLineReader);
    }
}
