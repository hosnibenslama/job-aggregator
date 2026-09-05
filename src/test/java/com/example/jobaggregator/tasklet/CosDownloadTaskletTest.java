package com.example.jobaggregator.tasklet;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;

import com.example.jobaggregator.config.CosProperties;
import com.example.jobaggregator.storage.CloudObjectStorageService;
import java.nio.file.Path;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import org.springframework.batch.infrastructure.repeat.RepeatStatus;

class CosDownloadTaskletTest {

    @TempDir
    Path tempDir;

    @Test
    void skipsDownloadWhenCosDisabled() throws Exception {
        CloudObjectStorageService storageService = mock(CloudObjectStorageService.class);
        CosProperties properties = new CosProperties(
                false, "http://endpoint", "region", "bucket", "key", "secret",
                "remote-input.txt", "remote-reject.txt", tempDir);

        Path target = tempDir.resolve("target.txt");
        CosDownloadTasklet tasklet = new CosDownloadTasklet(storageService, properties, target);

        RepeatStatus status = tasklet.execute(null, null);

        assertThat(status).isEqualTo(RepeatStatus.FINISHED);
        verify(storageService, never()).download(any(), any());
    }

    @Test
    void downloadsFileWhenCosEnabled() throws Exception {
        CloudObjectStorageService storageService = mock(CloudObjectStorageService.class);
        CosProperties properties = new CosProperties(
                true, "http://endpoint", "region", "bucket", "key", "secret",
                "remote-input.txt", "remote-reject.txt", tempDir);

        Path target = tempDir.resolve("target.txt");
        CosDownloadTasklet tasklet = new CosDownloadTasklet(storageService, properties, target);

        RepeatStatus status = tasklet.execute(null, null);

        assertThat(status).isEqualTo(RepeatStatus.FINISHED);
        verify(storageService).download(eq("remote-input.txt"), eq(target));
    }
}
