package com.example.jobaggregator.config;

import com.example.jobaggregator.partition.ContractPartitionSplitterTasklet;
import com.example.jobaggregator.partition.GeneratedFilePartitioner;
import com.example.jobaggregator.reader.BusinessLineMapper;
import com.example.jobaggregator.reader.ContractFileReader;
import com.example.jobaggregator.processor.ContractProcessor;
import com.example.jobaggregator.writer.ContractJdbcWriter;
import java.nio.charset.Charset;
import java.nio.file.Path;
import org.springframework.batch.core.job.Job;
import org.springframework.batch.core.step.Step;
import org.springframework.batch.core.job.builder.JobBuilder;
import org.springframework.batch.core.partition.support.SimplePartitioner;
import org.springframework.batch.core.repository.JobRepository;
import org.springframework.batch.core.step.builder.StepBuilder;
import org.springframework.batch.core.step.tasklet.TaskletStep;
import org.springframework.batch.infrastructure.item.ExecutionContext;
import org.springframework.batch.infrastructure.item.ItemProcessor;
import org.springframework.batch.infrastructure.item.ItemStreamException;
import org.springframework.batch.infrastructure.item.ItemStreamReader;
import org.springframework.batch.infrastructure.item.ItemWriter;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.transaction.PlatformTransactionManager;

@Configuration
@EnableConfigurationProperties(ContractImportProperties.class)
public class ContractImportJobConfig {

    private final Path inputContractFile;
    private final Path partitionDirectory;
    private final int requestedPartitions;
    private final Charset charset;

    public ContractImportJobConfig(ContractImportProperties props) {
        this.inputContractFile = props.inputFile();
        this.partitionDirectory = props.partitionDirectory();
        this.requestedPartitions = props.requestedPartitions();
        this.charset = props.charset();
    }

    @Bean
    public Job contractImportJob(JobRepository jobRepository, Step splitStep, Step workerStep) {
        return new JobBuilder("contractImportJob", jobRepository)
                .start(splitStep)
                .next(workerStep)
                .build();
    }

    @Bean
    public TaskletStep splitStep(
            StepBuilder stepBuilder,
            PlatformTransactionManager transactionManager,
            JobRepository jobRepository) {
        ContractPartitionSplitterTasklet tasklet = new ContractPartitionSplitterTasklet(
                inputContractFile,
                partitionDirectory,
                requestedPartitions,
                charset);

        return stepBuilder
                .tasklet(tasklet, transactionManager)
                .build();
    }

    @Bean
    public Step workerStep(
            StepBuilder stepBuilder,
            PlatformTransactionManager transactionManager,
            ContractProcessor processor,
            ContractJdbcWriter writer) {

        // Reader that delegates to a ContractFileReader, obtaining the partition file
        // path from the step execution context populated by GeneratedFilePartitioner.
        ItemStreamReader<com.example.jobaggregator.domain.Contract> reader =
                new ItemStreamReader<>() {
                    private ContractFileReader delegate;

                    @Override
                    public void open(ExecutionContext executionContext) throws ItemStreamException {
                        String partitionFile = executionContext.getString("partitionFile");
                        delegate = new ContractFileReader(
                                Path.of(partitionFile), charset, new BusinessLineMapper());
                        delegate.open(executionContext);
                    }

                    @Override
                    public com.example.jobaggregator.domain.Contract read() throws Exception {
                        return delegate.read();
                    }

                    @Override
                    public void update(ExecutionContext executionContext) throws ItemStreamException {
                        delegate.update(executionContext);
                    }

                    @Override
                    public void close() throws ItemStreamException {
                        if (delegate != null) {
                            delegate.close();
                        }
                    }
                };

        ItemProcessor<com.example.jobaggregator.domain.Contract, com.example.jobaggregator.domain.Contract> processorDelegate =
                processor;

        ItemWriter<com.example.jobaggregator.domain.Contract> writerDelegate = writer;

        return stepBuilder
                .<com.example.jobaggregator.domain.Contract, com.example.jobaggregator.domain.Contract>chunk(100, transactionManager)
                .reader(reader)
                .processor(processorDelegate)
                .writer(writerDelegate)
                .build();
    }

    @Bean
    public org.springframework.core.task.TaskExecutor taskExecutor() {
        org.springframework.scheduling.concurrent.ThreadPoolTaskExecutor executor =
                new org.springframework.scheduling.concurrent.ThreadPoolTaskExecutor();
        executor.setCorePoolSize(requestedPartitions);
        executor.setMaxPoolSize(requestedPartitions);
        executor.setThreadNamePrefix("contract-import-worker-");
        executor.initialize();
        return executor;
    }

    @Bean
    public GeneratedFilePartitioner contractPartitioner(
            JobRepository jobRepository,
            PlatformTransactionManager transactionManager) {
        return new GeneratedFilePartitioner(null);
    }
}
