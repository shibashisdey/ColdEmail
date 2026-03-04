package com.shibashis.coldmailer.v1.services;

import com.shibashis.coldmailer.v1.models.Campaign;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.io.Resource;
import org.springframework.core.io.UrlResource;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;
import org.springframework.web.server.ResponseStatusException;

import java.io.IOException;
import java.net.MalformedURLException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardCopyOption;
import java.time.LocalDateTime;
import java.util.Locale;
import java.util.Set;
import java.util.UUID;

@Service
public class CampaignResumeService {

    private static final Set<String> ALLOWED_MIME_TYPES = Set.of(
            "application/pdf",
            "application/msword",
            "application/vnd.openxmlformats-officedocument.wordprocessingml.document"
    );

    private static final Set<String> ALLOWED_EXTENSIONS = Set.of(".pdf", ".doc", ".docx");

    private final Path storageDir;
    private final long maxResumeBytes;

    public CampaignResumeService(@Value("${app.resume.storage-dir:uploads/resumes}") String storageDir,
                                 @Value("${app.resume.max-bytes:5242880}") long maxResumeBytes) {
        this.storageDir = Paths.get(storageDir).toAbsolutePath().normalize();
        this.maxResumeBytes = maxResumeBytes;
    }

    public Campaign attachResume(Campaign campaign, MultipartFile file) {
        validateFile(file);
        ensureDirectoryExists();

        String originalName = safeOriginalFilename(file.getOriginalFilename());
        String extension = extractExtension(originalName);
        String storedName = UUID.randomUUID() + extension;
        Path target = storageDir.resolve(storedName).normalize();

        if (!target.startsWith(storageDir)) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Invalid file path");
        }

        try {
            Files.copy(file.getInputStream(), target, StandardCopyOption.REPLACE_EXISTING);
            deletePreviousFileIfPresent(campaign);
        } catch (IOException ex) {
            throw new ResponseStatusException(HttpStatus.INTERNAL_SERVER_ERROR, "Failed to store resume");
        }

        campaign.setResumeOriginalFileName(originalName);
        campaign.setResumeStoredFileName(storedName);
        campaign.setResumeContentType(file.getContentType());
        campaign.setResumeUploadedAt(LocalDateTime.now());
        return campaign;
    }

    public Resource loadResumeResource(Campaign campaign) {
        if (campaign.getResumeStoredFileName() == null || campaign.getResumeStoredFileName().isBlank()) {
            throw new ResponseStatusException(HttpStatus.NOT_FOUND, "Resume not uploaded");
        }
        Path file = storageDir.resolve(campaign.getResumeStoredFileName()).normalize();
        if (!file.startsWith(storageDir) || !Files.exists(file)) {
            throw new ResponseStatusException(HttpStatus.NOT_FOUND, "Resume file not found");
        }
        try {
            return new UrlResource(file.toUri());
        } catch (MalformedURLException ex) {
            throw new ResponseStatusException(HttpStatus.INTERNAL_SERVER_ERROR, "Failed to read resume");
        }
    }

    private void validateFile(MultipartFile file) {
        if (file == null || file.isEmpty()) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Resume file cannot be empty");
        }
        if (file.getSize() > maxResumeBytes) {
            throw new ResponseStatusException(HttpStatus.PAYLOAD_TOO_LARGE, "Resume file exceeds allowed size");
        }

        String contentType = file.getContentType();
        if (contentType == null || !ALLOWED_MIME_TYPES.contains(contentType.toLowerCase(Locale.ROOT))) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Only PDF, DOC, DOCX MIME types are allowed");
        }

        String originalName = safeOriginalFilename(file.getOriginalFilename());
        String extension = extractExtension(originalName);
        if (!ALLOWED_EXTENSIONS.contains(extension.toLowerCase(Locale.ROOT))) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Only .pdf, .doc, .docx files are allowed");
        }
    }

    private void ensureDirectoryExists() {
        try {
            Files.createDirectories(storageDir);
        } catch (IOException ex) {
            throw new ResponseStatusException(HttpStatus.INTERNAL_SERVER_ERROR, "Failed to initialize resume storage");
        }
    }

    private void deletePreviousFileIfPresent(Campaign campaign) {
        if (campaign.getResumeStoredFileName() == null || campaign.getResumeStoredFileName().isBlank()) {
            return;
        }
        Path previous = storageDir.resolve(campaign.getResumeStoredFileName()).normalize();
        try {
            if (previous.startsWith(storageDir)) {
                Files.deleteIfExists(previous);
            }
        } catch (IOException ignored) {
        }
    }

    private String safeOriginalFilename(String original) {
        String fallback = "resume.pdf";
        if (original == null || original.isBlank()) {
            return fallback;
        }
        return Paths.get(original).getFileName().toString();
    }

    private String extractExtension(String fileName) {
        int idx = fileName.lastIndexOf('.');
        if (idx < 0) {
            return "";
        }
        return fileName.substring(idx);
    }
}
