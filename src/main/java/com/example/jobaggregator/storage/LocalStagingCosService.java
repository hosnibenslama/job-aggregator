package com.example.jobaggregator.storage;

import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.core.io.Resource;
import org.springframework.core.io.ResourceLoader;

/**
 * Local filesystem simulation of {@link CloudObjectStorageService}.
 * Used when {@code cos.enabled=false} for local development, testing, and offline batch execution.
 */
public class LocalStagingCosService implements CloudObjectStorageService {

    private static final Logger log = LoggerFactory.getLogger(LocalStagingCosService.class);

    private final Path mockStorageDirectory;
    private final ResourceLoader resourceLoader;
    private final String fallbackInputLocation;

    public LocalStagingCosService(Path stagingDirectory, ResourceLoader resourceLoader, String fallbackInputLocation) {
        this.mockStorageDirectory = stagingDirectory.resolve("mock-cos");
        this.resourceLoader = resourceLoader;
        this.fallbackInputLocation = fallbackInputLocation;
    }

    @Override
    public void download(String remoteKey, Path targetLocalFile) throws IOException {
        log.info("[SIMULATION] COS download requested for key '{}' to '{}'", remoteKey, targetLocalFile);

        if (targetLocalFile.getParent() != null) {
            Files.createDirectories(targetLocalFile.getParent());
        }

        // 1. Check if the object exists in the mock storage directory
        Path mockFile = mockStorageDirectory.resolve(remoteKey);
        if (Files.exists(mockFile)) {
            Files.copy(mockFile, targetLocalFile, StandardCopyOption.REPLACE_EXISTING);
            log.info("[SIMULATION] Downloaded from mock storage: {}", mockFile);
            return;
        }

        // 2. If target already exists, do not overwrite
        if (Files.exists(targetLocalFile) && Files.size(targetLocalFile) > 0) {
            log.info("[SIMULATION] Target local file already populated ({} bytes), skipping download",
                    Files.size(targetLocalFile));
            return;
        }

        // 3. Fall back to reading from configured input resource (e.g. classpath:visa-premier.txt)
        if (fallbackInputLocation != null && !fallbackInputLocation.isBlank()) {
            Resource resource = resourceLoader.getResource(fallbackInputLocation);
            if (resource.exists()) {
                try (InputStream in = resource.getInputStream()) {
                    Files.copy(in, targetLocalFile, StandardCopyOption.REPLACE_EXISTING);
                    log.info("[SIMULATION] Staged input from fallback resource: {}", fallbackInputLocation);
                    return;
                }
            }
        }

        log.warn("[SIMULATION] No source content found to download for key '{}'", remoteKey);
    }

    @Override
    public void upload(Path localSourceFile, String remoteKey) throws IOException {
        log.info("[SIMULATION] COS upload requested for '{}' to key '{}'", localSourceFile, remoteKey);

        if (!Files.exists(localSourceFile)) {
            log.info("[SIMULATION] Source file '{}' does not exist, skipping mock upload", localSourceFile);
            return;
        }

        Path mockDestination = mockStorageDirectory.resolve(remoteKey);
        if (mockDestination.getParent() != null) {
            Files.createDirectories(mockDestination.getParent());
        }

        Files.copy(localSourceFile, mockDestination, StandardCopyOption.REPLACE_EXISTING);
        log.info("[SIMULATION] Successfully stored upload copy in mock storage: {}", mockDestination);
    }

    @Override
    public boolean exists(String remoteKey) {
        return Files.exists(mockStorageDirectory.resolve(remoteKey));
    }
}
