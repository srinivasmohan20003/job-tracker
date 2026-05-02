package com.jobtracker.controller;

import com.jobtracker.dto.AnalysisResponse;
import com.jobtracker.dto.ResumeResponse;
import com.jobtracker.entity.User;
import com.jobtracker.service.ResumeService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.util.List;

/**
 * REST controller for resume management and AI analysis.
 * All endpoints require JWT authentication.
 *
 * Endpoints:
 * POST   /api/resumes/upload              → upload a resume (PDF/DOCX)
 * GET    /api/resumes                      → list user's resumes
 * DELETE /api/resumes/{id}                 → delete a resume
 * POST   /api/resumes/{resumeId}/analyze/{jobId} → AI analysis
 */
@RestController
@RequestMapping("/api/resumes")
@RequiredArgsConstructor
@Slf4j
public class ResumeController {

    private final ResumeService resumeService;

    /**
     * Upload a resume file.
     * Accepts multipart/form-data with a 'file' field.
     * Only PDF and DOCX files are allowed (max 10MB configured in application.properties).
     *
     * Example: POST /api/resumes/upload with form-data key="file"
     */
    @PostMapping(value = "/upload", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public ResponseEntity<ResumeResponse> uploadResume(
            @RequestParam("file") MultipartFile file,
            @AuthenticationPrincipal User user) {

        log.info("POST /api/resumes/upload — user={}, file={}, size={}",
                user.getEmail(), file.getOriginalFilename(), file.getSize());

        ResumeResponse response = resumeService.uploadResume(file, user.getId());
        return ResponseEntity.status(HttpStatus.CREATED).body(response);
    }

    /**
     * Get all resumes for the authenticated user.
     * Returns a list ordered by upload date (newest first).
     */
    @GetMapping
    public ResponseEntity<List<ResumeResponse>> getUserResumes(
            @AuthenticationPrincipal User user) {

        log.info("GET /api/resumes — user={}", user.getEmail());

        List<ResumeResponse> resumes = resumeService.getUserResumes(user.getId());
        return ResponseEntity.ok(resumes);
    }

    /**
     * Delete a resume (removes file from disk and record from database).
     * Returns 404 if resume doesn't exist or doesn't belong to the user.
     */
    @DeleteMapping("/{id}")
    public ResponseEntity<Void> deleteResume(
            @PathVariable Long id,
            @AuthenticationPrincipal User user) {

        log.info("DELETE /api/resumes/{} — user={}", id, user.getEmail());

        resumeService.deleteResume(id, user.getId());
        return ResponseEntity.noContent().build();
    }

    /**
     * Analyze a resume against a job application's description.
     * Returns match percentage, matched/missing skills, and improvement suggestions.
     *
     * Both the resume and job application must belong to the authenticated user.
     */
    @PostMapping("/{resumeId}/analyze/{jobId}")
    public ResponseEntity<AnalysisResponse> analyzeResume(
            @PathVariable Long resumeId,
            @PathVariable Long jobId,
            @AuthenticationPrincipal User user) {

        log.info("POST /api/resumes/{}/analyze/{} — user={}",
                resumeId, jobId, user.getEmail());

        AnalysisResponse analysis =
                resumeService.analyzeResumeAgainstJob(resumeId, jobId, user.getId());
        return ResponseEntity.ok(analysis);
    }
}
