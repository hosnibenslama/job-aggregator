package com.example.jobaggregator.tasklet;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;

import com.example.jobaggregator.config.CosProperties;
import com.example.jobaggregator.storage.CloudObjectStorageService;
import java.nio.file.Files;
import java.nio.file.Path;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import org.springframework.batch.infrastructure.repeat.RepeatStatus;

class CosUploadTaskletTest {

    @TempDir
    Path tempDir;

    @Test
    void shouldSkipUploadWhenCosIsDisabled() throws Exception {
        // Given: Cloud Object Storage is disabled in configuration even though reject file exists
        CloudObjectStorageService storageService = mock(CloudObjectStorageService.class);
        CosProperties properties = new CosProperties(
                false, "http://endpoint", "region", "bucket", "key", "secret",
                "remote-input.txt", "remote-reject.txt", tempDir);
        Path rejectFile = tempDir.resolve("reject.txt");
        Files.writeString(rejectFile, "rejected content");
        CosUploadTasklet tasklet = new CosUploadTasklet(storageService, properties, rejectFile);

        // Act: Execute the upload tasklet
        RepeatStatus status = tasklet.execute(null, null);

        // Assert: Tasklet finishes and remote upload is never invoked
        assertThat(status).isEqualTo(RepeatStatus.FINISHED);
        verify(storageService, never()).upload(any(), any());
    }

    @Test
    void shouldSkipUploadWhenRejectFileDoesNotExist() throws Exception {
        // Given: Cloud Object Storage is enabled but local reject file does not exist
        CloudObjectStorageService storageService = mock(CloudObjectStorageService.class);
        CosProperties properties = new CosProperties(
                true, "http://endpoint", "region", "bucket", "key", "secret",
                "remote-input.txt", "remote-reject.txt", tempDir);
        Path rejectFile = tempDir.resolve("non-existent-reject.txt");
        CosUploadTasklet tasklet = new CosUploadTasklet(storageService, properties, rejectFile);

        // Act: Execute the upload tasklet
        RepeatStatus status = tasklet.execute(null, null);

        // Assert: Tasklet finishes and remote upload is never invoked
        assertThat(status).isEqualTo(RepeatStatus.FINISHED);
        verify(storageService, never()).upload(any(), any());
    }

    @Test
    void shouldSkipUploadWhenRejectFileIsEmpty() throws Exception {
        // Given: Cloud Object Storage is enabled but local reject file is 0 bytes (no rejections)
        CloudObjectStorageService storageService = mock(CloudObjectStorageService.class);
        CosProperties properties = new CosProperties(
                true, "http://endpoint", "region", "bucket", "key", "secret",
                "remote-input.txt", "remote-reject.txt", tempDir);
        Path rejectFile = tempDir.resolve("empty-reject.txt");
        Files.createFile(rejectFile);
        CosUploadTasklet tasklet = new CosUploadTasklet(storageService, properties, rejectFile);

        // Act: Execute the upload tasklet
        RepeatStatus status = tasklet.execute(null, null);

        // Assert: Tasklet finishes and remote upload is never invoked
        assertThat(status).isEqualTo(RepeatStatus.FINISHED);
        verify(storageService, never()).upload(any(), any());
    }

    @Test
    void shouldUploadRejectFileToCosWhenRejectFileHasContent() throws Exception {
        // Given: Cloud Object Storage is enabled and local reject file has content
        CloudObjectStorageService storageService = mock(CloudObjectStorageService.class);
        CosProperties properties = new CosProperties(
                true, "http://endpoint", "region", "bucket", "key", "secret",
                "remote-input.txt", "remote-reject.txt", tempDir);
        Path rejectFile = tempDir.resolve("reject.txt");
        Files.writeString(rejectFile, "# ERROR: Mandatory ACC line missing");
        CosUploadTasklet tasklet = new CosUploadTasklet(storageService, properties, rejectFile);

        // Act: Execute the upload tasklet
        RepeatStatus status = tasklet.execute(null, null);

        // Assert: Tasklet finishes and reject file is uploaded to remote storage
        assertThat(status).isEqualTo(RepeatStatus.FINISHED);
        verify(storageService).upload(eq(rejectFile), eq("remote-reject.txt"));
    }
}
