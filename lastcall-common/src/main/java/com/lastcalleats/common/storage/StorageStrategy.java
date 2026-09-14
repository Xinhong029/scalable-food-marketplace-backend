package com.lastcalleats.common.storage;

import org.springframework.web.multipart.MultipartFile;

/**
 * Interface for file storage (Strategy Pattern).
 * Can switch between local disk, MinIO, or S3 without changing business code.
 */
public interface StorageStrategy {

    /**
     * Upload a file to the given sub-folder and return its access URL.
     *
     * @param file   the file to upload, supplied as a MultipartFile
     * @param folder the sub-folder name to store the file under (e.g. "avatars", "covers")
     * @return the URL that can be used to access the uploaded file
     */
    String upload(MultipartFile file, String folder);
}
