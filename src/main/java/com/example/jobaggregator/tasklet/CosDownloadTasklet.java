package com.example.jobaggregator.tasklet;

import com.example.jobaggregator.config.CosProperties;
import com.example.jobaggregator.storage.CloudObjectStorageService;
import java.nio.file.Path;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.batch.core.step.StepContribution;
import org.springframework.batch.core.scope.context.ChunkContext;
import org.springframework.batch.core.step.tasklet.Tasklet;
import org.springframework.batch.infrastructure.repeat.RepeatStatus;

/**
 * Spring Batch Tasklet executed before the import step to download the contract
 * input file from Cloud Object Storage (COS) into the local staging directory.
 */
public class CosDownloadTasklet implements Tasklet {

    private static final Logger log = LoggerFactory.getLogger(CosDownloadTasklet.class);

    private final CloudObjectStorageService storageService;
    private final CosProperties cosProperties;
    private final Path localTargetFile;

    public CosDownloadTasklet(
            CloudObjectStorageService storageService,
            CosProperties cosProperties,
            Path localTargetFile) {
        this.storageService = storageService;
        this.cosProperties = cosProperties;
        this.localTargetFile = localTargetFile;
    }

    @Override
    public RepeatStatus execute(StepContribution contribution, ChunkContext chunkContext) throws Exception {
        if (!cosProperties.enabled()) {
            log.info("COS integration is disabled (cos.enabled=false). Skipping COS download step.");
            return RepeatStatus.FINISHED;
        }

        String inputKey = cosProperties.inputKey();
        log.info("Starting COS download for remote key '{}' into local path '{}'", inputKey, localTargetFile);

        storageService.download(inputKey, localTargetFile);

        log.info("Completed COS download step for key '{}'", inputKey);
        return RepeatStatus.FINISHED;
    }
}
