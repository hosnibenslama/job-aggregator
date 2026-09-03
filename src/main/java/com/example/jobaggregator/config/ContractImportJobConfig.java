package com.example.jobaggregator.config;

import com.example.jobaggregator.domain.Contract;
import com.example.jobaggregator.processor.ContractProcessor;
import com.example.jobaggregator.reader.BusinessLineMapper;
import com.example.jobaggregator.reader.ContractFileReader;
import com.example.jobaggregator.writer.ContractJdbcWriter;
import java.nio.charset.Charset;
import java.nio.file.Path;
import org.springframework.batch.core.configuration.annotation.StepScope;
import org.springframework.batch.core.job.Job;
import org.springframework.batch.core.job.builder.JobBuilder;
import org.springframework.batch.core.repository.JobRepository;
import org.springframework.batch.core.step.Step;
import org.springframework.batch.core.step.builder.StepBuilder;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
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
    public Job contractImportJob(JobRepository jobRepository, Step contractImportStep) {
        return new JobBuilder("contractImportJob", jobRepository)
                .start(contractImportStep)
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
    public ContractFileReader contractItemReader() {
        return new ContractFileReader(inputContractFile, charset, new BusinessLineMapper());
    }
}
