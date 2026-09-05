package com.example.jobaggregator.storage;

import java.io.IOException;
import java.nio.file.Path;

/**
 * High-level service abstraction for Cloud Object Storage (COS) operations.
 * Allows transparent switching between cloud-based S3/COS and local staging during testing.
 */
public interface CloudObjectStorageService {

    /**
     * Downloads an object identified by remoteKey from the configured bucket to the specified local file path.
     *
     * @param remoteKey       the key/path of the object in the storage bucket
     * @param targetLocalFile the local file destination path
     * @throws IOException if downloading fails or file cannot be written
     */
    void download(String remoteKey, Path targetLocalFile) throws IOException;

    /**
     * Uploads a local file to the configured storage bucket under the specified remoteKey.
     *
     * @param localSourceFile the local file to upload
     * @param remoteKey       the destination key/path in the bucket
     * @throws IOException if the file cannot be read or upload fails
     */
    void upload(Path localSourceFile, String remoteKey) throws IOException;

    /**
     * Checks whether an object with the specified remoteKey exists in the bucket.
     *
     * @param remoteKey the key/path of the object in the bucket
     * @return true if the object exists, false otherwise
     */
    boolean exists(String remoteKey);
}
