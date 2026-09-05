package com.example.jobaggregator.config;

import java.nio.file.Path;
import org.springframework.boot.context.properties.ConfigurationProperties;

/**
 * Configuration properties for Cloud Object Storage (COS) integration.
 * Supports S3-compatible endpoints such as IBM Cloud Object Storage, AWS S3, MinIO, or Ceph.
 */
@ConfigurationProperties(prefix = "cos")
public record CosProperties(
        boolean enabled,
        String endpoint,
        String region,
        String bucket,
        String accessKey,
        String secretKey,
        String inputKey,
        String rejectKey,
        Path stagingDirectory) {

    public CosProperties {
        if (stagingDirectory == null) {
            stagingDirectory = Path.of("target", "staging");
        }
    }
}
