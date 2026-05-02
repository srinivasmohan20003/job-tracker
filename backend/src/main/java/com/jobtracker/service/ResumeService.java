package com.jobtracker.service;

import com.jobtracker.dto.AnalysisResponse;
import com.jobtracker.dto.ResumeResponse;
import com.jobtracker.entity.JobApplication;
import com.jobtracker.entity.Resume;
import com.jobtracker.entity.User;
import com.jobtracker.exception.BadRequestException;
import com.jobtracker.exception.ResourceNotFoundException;
import com.jobtracker.repository.JobApplicationRepository;
import com.jobtracker.repository.ResumeRepository;
import com.jobtracker.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Path;
import java.util.List;
import java.util.stream.Collectors;

/**
 * Service for resume management: upload, list, delete, and AI analysis.
 *
 * Orchestrates:
 * - FileStorageService (disk operations)
 * - TextExtractionService (Tika parsing)
 * - ResumeAnalysisService (keyword matching)
 *
 * All operations are scoped to the authenticated user.
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class ResumeService {

    private final ResumeRepository resumeRepository;
    private final UserRepository userRepository;
    private final JobApplicationRepository jobApplicationRepository;
    private final FileStorageService fileStorageService;
    private final TextExtractionService textExtractionService;
    private final ResumeAnalysisService resumeAnalysisService;

    /**
     * Upload a resume file.
     *
     * Flow:
     * 1. Store file on disk (UUID-named)
     * 2. Extract text using Apache Tika
     * 3. Save resume metadata + extracted text to DB
     * 4. Return response DTO
     */
    @Transactional
    public ResumeResponse uploadResume(MultipartFile file, Long userId) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new ResourceNotFoundException("User", "id", userId));

        // Store file on disk
        String storedFileName = fileStorageService.storeFile(file);
        Path filePath = fileStorageService.getFilePath(storedFileName);

        // Extract text from file
        String extractedText = "";
        try (InputStream inputStream = file.getInputStream()) {
            extractedText = textExtractionService.extractText(inputStream, file.getOriginalFilename());
        } catch (IOException e) {
            log.warn("Could not extract text from file: {}", file.getOriginalFilename(), e);
        }

        // Save resume record
        Resume resume = Resume.builder()
                .originalFileName(file.getOriginalFilename())
                .storedFileName(storedFileName)
                .fileType(file.getContentType())
                .filePath(filePath.toString())
                .fileSize(file.getSize())
                .extractedText(extractedText)
                .user(user)
                .build();

        resume = resumeRepository.save(resume);
        log.info("Resume uploaded: {} (id={}) for user {}",
                file.getOriginalFilename(), resume.getId(), userId);

        return mapToResponse(resume);
    }

    /**
     * Get all resumes for the authenticated user, ordered by upload date (newest first).
     */
    @Transactional(readOnly = true)
    public List<ResumeResponse> getUserResumes(Long userId) {
        List<Resume> resumes = resumeRepository.findByUserIdOrderByUploadedAtDesc(userId);

        log.debug("Found {} resumes for user {}", resumes.size(), userId);

        return resumes.stream()
                .map(this::mapToResponse)
                .collect(Collectors.toList());
    }

    /**
     * Delete a resume (file from disk + record from DB).
     *
     * @throws ResourceNotFoundException if resume not found or doesn't belong to user
     */
    @Transactional
    public void deleteResume(Long id, Long userId) {
        Resume resume = resumeRepository.findByIdAndUserId(id, userId)
                .orElseThrow(() -> new ResourceNotFoundException("Resume", "id", id));

        // Delete file from disk
        fileStorageService.deleteFile(resume.getStoredFileName());

        // Delete DB record
        resumeRepository.delete(resume);
        log.info("Deleted resume {} for user {}", id, userId);
    }

    /**
     * Analyze a resume against a job application's description.
     *
     * @param resumeId the resume to analyze
     * @param jobApplicationId the job application containing the description
     * @param userId the authenticated user's ID
     * @return analysis results with match percentage, skills, and suggestions
     *
     * @throws ResourceNotFoundException if resume or job application not found
     * @throws BadRequestException if the job application has no description
     */
    @Transactional(readOnly = true)
    public AnalysisResponse analyzeResumeAgainstJob(Long resumeId, Long jobApplicationId, Long userId) {
        Resume resume = resumeRepository.findByIdAndUserId(resumeId, userId)
                .orElseThrow(() -> new ResourceNotFoundException("Resume", "id", resumeId));

        JobApplication jobApplication = jobApplicationRepository.findByIdAndUserId(jobApplicationId, userId)
                .orElseThrow(() -> new ResourceNotFoundException("Job Application", "id", jobApplicationId));

        if (resume.getExtractedText() == null || resume.getExtractedText().isBlank()) {
            throw new BadRequestException(
                    "Could not extract text from this resume. Please re-upload in PDF or DOCX format.");
        }

        if (jobApplication.getJobDescription() == null || jobApplication.getJobDescription().isBlank()) {
            throw new BadRequestException(
                    "This job application has no description. Please add a job description first.");
        }

        log.info("Analyzing resume {} against job application {} for user {}",
                resumeId, jobApplicationId, userId);

        return resumeAnalysisService.analyzeResume(
                resume.getExtractedText(),
                jobApplication.getJobDescription()
        );
    }

    /**
     * Map Resume entity to response DTO.
     */
    private ResumeResponse mapToResponse(Resume resume) {
        return ResumeResponse.builder()
                .id(resume.getId())
                .originalFileName(resume.getOriginalFileName())
                .fileType(resume.getFileType())
                .fileSize(resume.getFileSize())
                .uploadedAt(resume.getUploadedAt())
                .build();
    }
}
