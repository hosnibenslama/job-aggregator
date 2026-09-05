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
    void skipsUploadWhenCosDisabled() throws Exception {
        CloudObjectStorageService storageService = mock(CloudObjectStorageService.class);
        CosProperties properties = new CosProperties(
                false, "http://endpoint", "region", "bucket", "key", "secret",
                "remote-input.txt", "remote-reject.txt", tempDir);

        Path rejectFile = tempDir.resolve("reject.txt");
        Files.writeString(rejectFile, "rejected content");

        CosUploadTasklet tasklet = new CosUploadTasklet(storageService, properties, rejectFile);

        RepeatStatus status = tasklet.execute(null, null);

        assertThat(status).isEqualTo(RepeatStatus.FINISHED);
        verify(storageService, never()).upload(any(), any());
    }

    @Test
    void skipsUploadWhenRejectFileDoesNotExist() throws Exception {
        CloudObjectStorageService storageService = mock(CloudObjectStorageService.class);
        CosProperties properties = new CosProperties(
                true, "http://endpoint", "region", "bucket", "key", "secret",
                "remote-input.txt", "remote-reject.txt", tempDir);

        Path rejectFile = tempDir.resolve("non-existent-reject.txt");

        CosUploadTasklet tasklet = new CosUploadTasklet(storageService, properties, rejectFile);

        RepeatStatus status = tasklet.execute(null, null);

        assertThat(status).isEqualTo(RepeatStatus.FINISHED);
        verify(storageService, never()).upload(any(), any());
    }

    @Test
    void skipsUploadWhenRejectFileIsEmpty() throws Exception {
        CloudObjectStorageService storageService = mock(CloudObjectStorageService.class);
        CosProperties properties = new CosProperties(
                true, "http://endpoint", "region", "bucket", "key", "secret",
                "remote-input.txt", "remote-reject.txt", tempDir);

        Path rejectFile = tempDir.resolve("empty-reject.txt");
        Files.createFile(rejectFile);

        CosUploadTasklet tasklet = new CosUploadTasklet(storageService, properties, rejectFile);

        RepeatStatus status = tasklet.execute(null, null);

        assertThat(status).isEqualTo(RepeatStatus.FINISHED);
        verify(storageService, never()).upload(any(), any());
    }

    @Test
    void uploadsWhenRejectFileHasContent() throws Exception {
        CloudObjectStorageService storageService = mock(CloudObjectStorageService.class);
        CosProperties properties = new CosProperties(
                true, "http://endpoint", "region", "bucket", "key", "secret",
                "remote-input.txt", "remote-reject.txt", tempDir);

        Path rejectFile = tempDir.resolve("reject.txt");
        Files.writeString(rejectFile, "# REJECTED: Mandatory ACC line missing");

        CosUploadTasklet tasklet = new CosUploadTasklet(storageService, properties, rejectFile);

        RepeatStatus status = tasklet.execute(null, null);

        assertThat(status).isEqualTo(RepeatStatus.FINISHED);
        verify(storageService).upload(eq(rejectFile), eq("remote-reject.txt"));
    }
}
