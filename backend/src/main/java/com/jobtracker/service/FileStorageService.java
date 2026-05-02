package com.jobtracker.service;

import com.jobtracker.exception.BadRequestException;
import jakarta.annotation.PostConstruct;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardCopyOption;
import java.util.Set;
import java.util.UUID;

/**
 * Service for secure file storage operations.
 *
 * Design decisions:
 * - Files stored with UUID names to prevent path traversal and collisions
 * - Original filename preserved in DB, never used for disk storage
 * - Upload directory created at startup if missing
 * - Strict MIME type validation (PDF and DOCX only)
 * - File size validated at controller level (Spring multipart config)
 */
@Service
@Slf4j
public class FileStorageService {

    @Value("${file.upload-dir}")
    private String uploadDir;

    private Path uploadPath;

    private static final Set<String> ALLOWED_MIME_TYPES = Set.of(
            "application/pdf",
            "application/vnd.openxmlformats-officedocument.wordprocessingml.document"  // .docx
    );

    private static final Set<String> ALLOWED_EXTENSIONS = Set.of("pdf", "docx");

    /**
     * Initialize upload directory on application startup.
     */
    @PostConstruct
    public void init() {
        try {
            uploadPath = Paths.get(uploadDir).toAbsolutePath().normalize();
            Files.createDirectories(uploadPath);
            log.info("File upload directory initialized: {}", uploadPath);
        } catch (IOException e) {
            throw new RuntimeException("Could not create upload directory: " + uploadDir, e);
        }
    }

    /**
     * Store a file securely on disk.
     *
     * @param file the uploaded multipart file
     * @return the generated unique filename used for storage
     * @throws BadRequestException if file type is not allowed
     */
    public String storeFile(MultipartFile file) {
        validateFile(file);

        String originalFilename = file.getOriginalFilename();
        String extension = getFileExtension(originalFilename);
        String storedFileName = UUID.randomUUID().toString() + "." + extension;

        try {
            Path targetLocation = uploadPath.resolve(storedFileName);
            Files.copy(file.getInputStream(), targetLocation, StandardCopyOption.REPLACE_EXISTING);

            log.info("Stored file: {} → {} ({} bytes)",
                    originalFilename, storedFileName, file.getSize());

            return storedFileName;
        } catch (IOException e) {
            throw new RuntimeException("Failed to store file: " + originalFilename, e);
        }
    }

    /**
     * Delete a file from disk.
     */
    public void deleteFile(String storedFileName) {
        try {
            Path filePath = uploadPath.resolve(storedFileName).normalize();
            if (Files.exists(filePath)) {
                Files.delete(filePath);
                log.info("Deleted file: {}", storedFileName);
            }
        } catch (IOException e) {
            log.warn("Failed to delete file: {}", storedFileName, e);
        }
    }

    /**
     * Get the absolute path to a stored file.
     */
    public Path getFilePath(String storedFileName) {
        return uploadPath.resolve(storedFileName).normalize();
    }

    /**
     * Validate file type and content.
     *
     * @throws BadRequestException if file is empty, has no name, or wrong type
     */
    private void validateFile(MultipartFile file) {
        if (file.isEmpty()) {
            throw new BadRequestException("Cannot upload an empty file");
        }

        String originalFilename = file.getOriginalFilename();
        if (originalFilename == null || originalFilename.isBlank()) {
            throw new BadRequestException("File name is missing");
        }

        // Validate extension
        String extension = getFileExtension(originalFilename);
        if (!ALLOWED_EXTENSIONS.contains(extension.toLowerCase())) {
            throw new BadRequestException(
                    "Invalid file type: ." + extension + ". Only PDF and DOCX files are allowed");
        }

        // Validate MIME type
        String contentType = file.getContentType();
        if (contentType == null || !ALLOWED_MIME_TYPES.contains(contentType)) {
            throw new BadRequestException(
                    "Invalid content type: " + contentType + ". Only PDF and DOCX files are allowed");
        }
    }

    /**
     * Extract file extension from filename.
     */
    private String getFileExtension(String filename) {
        if (filename == null || !filename.contains(".")) {
            return "";
        }
        return filename.substring(filename.lastIndexOf(".") + 1);
    }
}
