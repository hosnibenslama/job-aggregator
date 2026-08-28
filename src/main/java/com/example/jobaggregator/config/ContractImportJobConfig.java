package com.example.jobaggregator.config;

import com.example.jobaggregator.partition.ContractPartitionSplitterTasklet;
import com.example.jobaggregator.partition.GeneratedFilePartitioner;
import com.example.jobaggregator.reader.BusinessLineMapper;
import com.example.jobaggregator.reader.ContractFileReader;
import com.example.jobaggregator.processor.ContractProcessor;
import com.example.jobaggregator.writer.ContractJdbcWriter;
import java.nio.charset.Charset;
import java.nio.charset.StandardCharsets;
import java.nio.file.Path;
import org.springframework.batch.core.Job;
import org.springframework.batch.core.Step;
import org.springframework.batch.core.job.builder.JobBuilder;
import org.springframework.batch.core.partition.PartitionStep;
import org.springframework.batch.core.partition.builder.PartitionStepBuilder;
import org.springframework.batch.core.partition.support.TaskletPartitioner;
import org.springframework.batch.core.repository.JobRepository;
import org.springframework.batch.core.step.builder.StepBuilder;
import org.springframework.batch.core.step.tasklet.TaskletStep;
import org.springframework.batch.item.ItemProcessor;
import org.springframework.batch.item.ItemReader;
import org.springframework.batch.item.ItemWriter;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.transaction.PlatformTransactionManager;

@Configuration
public class ContractImportJobConfig {

    private final Path inputContractFile;
    private final Path partitionDirectory;
    private final int requestedPartitions;
    private final Charset charset;

    public ContractImportJobConfig(
            Path inputContractFile,
            Path partitionDirectory,
            int requestedPartitions,
            Charset charset) {
        this.inputContractFile = inputContractFile;
        this.partitionDirectory = partitionDirectory;
        this.requestedPartitions = requestedPartitions;
        this.charset = charset;
    }

    @Bean
    public Job contractImportJob(JobRepository jobRepository, Step splitStep, PartitionStep workerStep) {
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
                .repository(jobRepository)
                .build();
    }

    @Bean
    public PartitionStep workerStep(
            StepBuilder stepBuilder,
            PlatformTransactionManager transactionManager,
            JobRepository jobRepository,
            ContractProcessor processor,
            ContractJdbcWriter writer) {

        ItemReader<com.example.jobaggregator.domain.Contract> reader =
                partitionFile -> new ContractFileReader(
                        Path.of(partitionFile),
                        charset,
                        new BusinessLineMapper());

        ItemProcessor<com.example.jobaggregator.domain.Contract, com.example.jobaggregator.domain.Contract> processorDelegate =
                processor;

        ItemWriter<com.example.jobaggregator.domain.Contract> writerDelegate = writer;

        Step worker = stepBuilder
                .<com.example.jobaggregator.domain.Contract, com.example.jobaggregator.domain.Contract>chunk(100, transactionManager)
                .reader(reader)
                .processor(processorDelegate)
                .writer(writerDelegate)
                .repository(jobRepository)
                .build();

        PartitionStepBuilder builder = stepBuilder
                .partitioner("workerStep", new TaskletPartitioner())
                .step(worker)
                .gridSize(requestedPartitions)
                .taskExecutor(taskExecutor());

        return builder.build();
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
