package com.example.jobaggregator.tasklet;

import com.example.jobaggregator.config.CosProperties;
import com.example.jobaggregator.storage.CloudObjectStorageService;
import java.nio.file.Files;
import java.nio.file.Path;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.batch.core.step.StepContribution;
import org.springframework.batch.core.scope.context.ChunkContext;
import org.springframework.batch.core.step.tasklet.Tasklet;
import org.springframework.batch.infrastructure.repeat.RepeatStatus;

/**
 * Spring Batch Tasklet executed after the contract import step to upload the
 * rejected contracts file to Cloud Object Storage (COS) if any rejections occurred.
 */
public class CosUploadTasklet implements Tasklet {

    private static final Logger log = LoggerFactory.getLogger(CosUploadTasklet.class);

    private final CloudObjectStorageService storageService;
    private final CosProperties cosProperties;
    private final Path localRejectFile;

    public CosUploadTasklet(
            CloudObjectStorageService storageService,
            CosProperties cosProperties,
            Path localRejectFile) {
        this.storageService = storageService;
        this.cosProperties = cosProperties;
        this.localRejectFile = localRejectFile;
    }

    @Override
    public RepeatStatus execute(StepContribution contribution, ChunkContext chunkContext) throws Exception {
        if (!cosProperties.enabled()) {
            log.info("COS integration is disabled (cos.enabled=false). Skipping COS upload step.");
            return RepeatStatus.FINISHED;
        }

        if (!Files.exists(localRejectFile) || Files.size(localRejectFile) == 0) {
            log.info("No rejected contracts file found or file is empty at '{}'. Skipping COS upload.", localRejectFile);
            return RepeatStatus.FINISHED;
        }

        String rejectKey = cosProperties.rejectKey();
        log.info("Uploading rejected contracts file '{}' ({} bytes) to COS key '{}'",
                localRejectFile, Files.size(localRejectFile), rejectKey);

        storageService.upload(localRejectFile, rejectKey);

        log.info("Successfully uploaded rejected contracts to s3://{}/{}", cosProperties.bucket(), rejectKey);
        return RepeatStatus.FINISHED;
    }
}
