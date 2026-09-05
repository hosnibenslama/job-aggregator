package com.example.jobaggregator.storage;

import com.example.jobaggregator.config.CosProperties;
import java.io.IOException;
import java.net.URI;
import java.nio.file.Files;
import java.nio.file.Path;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import software.amazon.awssdk.auth.credentials.AwsBasicCredentials;
import software.amazon.awssdk.auth.credentials.StaticCredentialsProvider;
import software.amazon.awssdk.core.exception.SdkException;
import software.amazon.awssdk.regions.Region;
import software.amazon.awssdk.services.s3.S3Client;
import software.amazon.awssdk.services.s3.S3ClientBuilder;
import software.amazon.awssdk.services.s3.S3Configuration;
import software.amazon.awssdk.services.s3.model.GetObjectRequest;
import software.amazon.awssdk.services.s3.model.HeadObjectRequest;
import software.amazon.awssdk.services.s3.model.NoSuchKeyException;
import software.amazon.awssdk.services.s3.model.PutObjectRequest;

/**
 * Production implementation of {@link CloudObjectStorageService} communicating with
 * S3-compatible Cloud Object Storage (IBM COS, AWS S3, MinIO, Ceph).
 */
public class S3CompatibleCosService implements CloudObjectStorageService, AutoCloseable {

    private static final Logger log = LoggerFactory.getLogger(S3CompatibleCosService.class);

    private final S3Client s3Client;
    private final String bucket;

    public S3CompatibleCosService(CosProperties properties) {
        this.bucket = properties.bucket();

        S3ClientBuilder builder = S3Client.builder();

        if (properties.endpoint() != null && !properties.endpoint().isBlank()) {
            builder.endpointOverride(URI.create(properties.endpoint()));
        }

        String regionName = (properties.region() != null && !properties.region().isBlank())
                ? properties.region()
                : "us-east-1";
        builder.region(Region.of(regionName));

        if (properties.accessKey() != null && !properties.accessKey().isBlank()
                && properties.secretKey() != null && !properties.secretKey().isBlank()) {
            builder.credentialsProvider(StaticCredentialsProvider.create(
                    AwsBasicCredentials.create(properties.accessKey(), properties.secretKey())));
        }

        // Enable path-style access for IBM COS / MinIO compatibility
        builder.serviceConfiguration(S3Configuration.builder()
                .pathStyleAccessEnabled(true)
                .build());

        this.s3Client = builder.build();
    }

    public S3CompatibleCosService(S3Client s3Client, String bucket) {
        this.s3Client = s3Client;
        this.bucket = bucket;
    }

    @Override
    public void download(String remoteKey, Path targetLocalFile) throws IOException {
        log.info("Downloading s3://{}/{} to {}", bucket, remoteKey, targetLocalFile);
        try {
            if (targetLocalFile.getParent() != null) {
                Files.createDirectories(targetLocalFile.getParent());
            }

            GetObjectRequest request = GetObjectRequest.builder()
                    .bucket(bucket)
                    .key(remoteKey)
                    .build();

            s3Client.getObject(request, targetLocalFile);
            log.info("Successfully downloaded s3://{}/{} ({} bytes)",
                    bucket, remoteKey, Files.size(targetLocalFile));
        } catch (SdkException e) {
            throw new IOException("Failed to download s3://" + bucket + "/" + remoteKey, e);
        }
    }

    @Override
    public void upload(Path localSourceFile, String remoteKey) throws IOException {
        if (!Files.exists(localSourceFile)) {
            throw new IOException("Source file does not exist for upload: " + localSourceFile);
        }

        log.info("Uploading {} to s3://{}/{}", localSourceFile, bucket, remoteKey);
        try {
            PutObjectRequest request = PutObjectRequest.builder()
                    .bucket(bucket)
                    .key(remoteKey)
                    .build();

            s3Client.putObject(request, localSourceFile);
            log.info("Successfully uploaded {} ({} bytes) to s3://{}/{}",
                    localSourceFile, Files.size(localSourceFile), bucket, remoteKey);
        } catch (SdkException e) {
            throw new IOException("Failed to upload " + localSourceFile + " to s3://" + bucket + "/" + remoteKey, e);
        }
    }

    @Override
    public boolean exists(String remoteKey) {
        try {
            s3Client.headObject(HeadObjectRequest.builder()
                    .bucket(bucket)
                    .key(remoteKey)
                    .build());
            return true;
        } catch (NoSuchKeyException e) {
            return false;
        } catch (SdkException e) {
            log.warn("Error checking existence of s3://{}/{}: {}", bucket, remoteKey, e.getMessage());
            return false;
        }
    }

    @Override
    public void close() {
        if (s3Client != null) {
            s3Client.close();
        }
    }
}
