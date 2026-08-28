package com.example.jobaggregator.config;

import com.example.jobaggregator.domain.Contract;
import com.example.jobaggregator.partition.ContractPartitionSplitterTasklet;
import com.example.jobaggregator.partition.GeneratedFilePartitioner;
import com.example.jobaggregator.processor.ContractProcessor;
import com.example.jobaggregator.reader.BusinessLineMapper;
import com.example.jobaggregator.reader.ContractFileReader;
import com.example.jobaggregator.writer.ContractJdbcWriter;
import java.nio.charset.Charset;
import java.nio.file.Path;
import org.springframework.batch.core.configuration.annotation.StepScope;
import org.springframework.batch.core.job.Job;
import org.springframework.batch.core.job.builder.JobBuilder;
import org.springframework.batch.core.partition.Partitioner;
import org.springframework.batch.core.repository.JobRepository;
import org.springframework.batch.core.step.Step;
import org.springframework.batch.core.step.builder.StepBuilder;
import org.springframework.batch.core.step.tasklet.TaskletStep;
import org.springframework.batch.infrastructure.item.ItemStreamReader;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.task.TaskExecutor;
import org.springframework.scheduling.concurrent.ThreadPoolTaskExecutor;
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
    public Job contractImportJob(JobRepository jobRepository, Step splitStep, Step managerStep) {
        return new JobBuilder("contractImportJob", jobRepository)
                .start(splitStep)
                .next(managerStep)
                .build();
    }

    @Bean
    public TaskletStep splitStep(
            JobRepository jobRepository,
            PlatformTransactionManager transactionManager) {
        ContractPartitionSplitterTasklet tasklet = new ContractPartitionSplitterTasklet(
                inputContractFile,
                partitionDirectory,
                requestedPartitions,
                charset);

        return new StepBuilder("splitStep", jobRepository)
                .tasklet(tasklet, transactionManager)
                .build();
    }

    @Bean
    public Step managerStep(
            JobRepository jobRepository,
            Step workerStep,
            Partitioner contractPartitioner,
            TaskExecutor taskExecutor) {
        return new StepBuilder("managerStep", jobRepository)
                .partitioner("workerStep", contractPartitioner)
                .step(workerStep)
                .gridSize(requestedPartitions)
                .taskExecutor(taskExecutor)
                .build();
    }

    @Bean
    public Step workerStep(
            JobRepository jobRepository,
            PlatformTransactionManager transactionManager,
            ItemStreamReader<Contract> contractItemReader,
            ContractProcessor processor,
            ContractJdbcWriter writer) {
        return new StepBuilder("workerStep", jobRepository)
                .<Contract, Contract>chunk(100, transactionManager)
                .reader(contractItemReader)
                .processor(processor)
                .writer(writer)
                .build();
    }

    @Bean
    @StepScope
    public ItemStreamReader<Contract> contractItemReader(
            @Value("#{stepExecutionContext['partitionFile']}") String partitionFile) {
        return new ContractFileReader(Path.of(partitionFile), charset, new BusinessLineMapper());
    }

    @Bean
    @StepScope
    public Partitioner contractPartitioner(
            @Value("#{jobExecutionContext['" + ContractPartitionSplitterTasklet.PARTITION_FILES_KEY + "']}") String partitionFiles) {
        return new GeneratedFilePartitioner(partitionFiles);
    }

    @Bean
    public TaskExecutor taskExecutor() {
        ThreadPoolTaskExecutor executor = new ThreadPoolTaskExecutor();
        executor.setCorePoolSize(requestedPartitions);
        executor.setMaxPoolSize(requestedPartitions);
        executor.setThreadNamePrefix("contract-import-worker-");
        executor.initialize();
        return executor;
    }
}
