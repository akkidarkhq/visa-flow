package com.akkidarkhq.visaflow.service;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.io.Resource;
import org.springframework.core.io.UrlResource;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.net.MalformedURLException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardCopyOption;
import java.util.UUID;

@Service
public class FileStorageService {

    private final Path storageDirectory;

    public FileStorageService(@Value("${visaflow.storage.local-dir:uploads/documents}") String uploadDir) {
        this.storageDirectory = Paths.get(uploadDir).toAbsolutePath().normalize();
        try {
            Files.createDirectories(this.storageDirectory);
        } catch (IOException e) {
            throw new RuntimeException("Could not initialize storage directory at " + this.storageDirectory, e);
        }
    }

    /**
     * Stores a file on disk (or cloud/S3) and returns a unique file key/path.
     */
    public String storeFile(MultipartFile file, UUID applicationId, Long requirementId) {
        String originalFilename = file.getOriginalFilename() != null ? file.getOriginalFilename() : "document";
        String cleanExtension = "";
        int dotIndex = originalFilename.lastIndexOf('.');
        if (dotIndex > 0) {
            cleanExtension = originalFilename.substring(dotIndex);
        }

        // Generate isolated file key: {applicationId}/{requirementId}_{uniqueId}.ext
        String uniqueFileName = requirementId + "_" + UUID.randomUUID() + cleanExtension;
        Path targetFolder = this.storageDirectory.resolve(applicationId.toString());

        try {
            Files.createDirectories(targetFolder);
            Path targetLocation = targetFolder.resolve(uniqueFileName);
            Files.copy(file.getInputStream(), targetLocation, StandardCopyOption.REPLACE_EXISTING);

            return "applications/" + applicationId + "/" + uniqueFileName;
        } catch (IOException ex) {
            throw new RuntimeException("Could not store file " + originalFilename + ". Please try again!", ex);
        }
    }

    /**
     * Loads a file as a Spring Resource for viewing or downloading.
     */
    public Resource loadFileAsResource(String s3FileKey) {
        try {
            // Expected s3FileKey format: "applications/{applicationId}/{fileName}"
            String relativePath = s3FileKey.startsWith("applications/")
                    ? s3FileKey.substring("applications/".length())
                    : s3FileKey;

            Path filePath = this.storageDirectory.resolve(relativePath).normalize();
            Resource resource = new UrlResource(filePath.toUri());

            if (resource.exists() && resource.isReadable()) {
                return resource;
            } else {
                throw new RuntimeException("File not found or not readable: " + s3FileKey);
            }
        } catch (MalformedURLException ex) {
            throw new RuntimeException("File path invalid: " + s3FileKey, ex);
        }
    }
}
