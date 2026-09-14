package com.lastcalleats.common.storage;

import com.lastcalleats.common.exception.BusinessException;
import com.lastcalleats.common.exception.ErrorCode;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.UUID;

/**
 * Implementation of StorageStrategy that saves files to local disk.
 * Use this for development; can replace with MinIO or S3 implementation later.
 */
@Component
public class LocalStorageStrategy implements StorageStrategy {

    @Value("${storage.local.base-path:./uploads}")
    private String basePath;    // root directory where files are stored; configurable via application.yml

    @Value("${storage.local.url-prefix:/uploads}")
    private String urlPrefix;   // URL prefix prepended to the file path in the returned URL

    /**
     * Save file to local disk under the given folder and return its URL.
     * Use UUID as filename to avoid name conflicts.
     *
     * @param file   the file to upload
     * @param folder sub-folder name under basePath (e.g. "avatars")
     * @return URL path to access the uploaded file
     * @throws BusinessException if file write fails
     */
    @Override
    public String upload(MultipartFile file, String folder) {
        String originalFilename = file.getOriginalFilename();
        String extension = (originalFilename != null && originalFilename.contains("."))
                ? originalFilename.substring(originalFilename.lastIndexOf(".")) : "";
        String newFilename = UUID.randomUUID() + extension;

        Path targetDir = Paths.get(basePath, folder);
        Path targetFile = targetDir.resolve(newFilename);

        try {
            Files.createDirectories(targetDir);
            file.transferTo(targetFile.toFile());
        } catch (IOException e) {
            throw new BusinessException(ErrorCode.INTERNAL_ERROR, "File upload failed");
        }

        return urlPrefix + "/" + folder + "/" + newFilename;
    }
}
