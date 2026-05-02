package com.jobtracker.service;

import com.jobtracker.dto.DashboardStatsResponse;
import com.jobtracker.dto.JobApplicationRequest;
import com.jobtracker.dto.JobApplicationResponse;
import com.jobtracker.entity.ApplicationStatus;
import com.jobtracker.entity.JobApplication;
import com.jobtracker.entity.User;
import com.jobtracker.exception.BadRequestException;
import com.jobtracker.exception.ResourceNotFoundException;
import com.jobtracker.repository.JobApplicationRepository;
import com.jobtracker.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.cache.annotation.CacheEvict;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * Service for managing job applications.
 *
 * All operations are scoped to the authenticated user — a user can only
 * view, create, update, and delete their own applications.
 *
 * Key design decisions:
 * - User ID extracted from SecurityContext (passed from controller)
 * - Entity ↔ DTO mapping done here (controller never sees entities)
 * - Status string parsed with validation (BadRequestException on invalid)
 * - Dashboard stats use count queries (no full entity loading)
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class JobApplicationService {

    private final JobApplicationRepository applicationRepository;
    private final UserRepository userRepository;

    /**
     * Get all applications for the authenticated user with pagination and optional filters.
     *
     * @param userId  authenticated user's ID
     * @param status  optional status filter (null = all statuses)
     * @param company optional company name search (case-insensitive contains)
     * @param pageable pagination and sorting parameters
     * @return paginated list of application DTOs
     */
    @Transactional(readOnly = true)
    public Page<JobApplicationResponse> getApplications(
            Long userId, String status, String company, Pageable pageable) {

        ApplicationStatus statusEnum = parseStatus(status);

        Page<JobApplication> applications = applicationRepository.searchApplications(
                userId, statusEnum, company, pageable);

        log.debug("Found {} applications for user {} (page {}/{})",
                applications.getNumberOfElements(), userId,
                applications.getNumber() + 1, applications.getTotalPages());

        return applications.map(this::mapToResponse);
    }

    /**
     * Get a single application by ID, scoped to the authenticated user.
     *
     * @throws ResourceNotFoundException if application not found or doesn't belong to user
     */
    @Transactional(readOnly = true)
    public JobApplicationResponse getApplicationById(Long id, Long userId) {
        JobApplication application = applicationRepository.findByIdAndUserId(id, userId)
                .orElseThrow(() -> new ResourceNotFoundException("Job Application", "id", id));

        return mapToResponse(application);
    }

    /**
     * Create a new job application for the authenticated user.
     */
    @Transactional
    @CacheEvict(value = "dashboardStats", key = "#userId")
    public JobApplicationResponse createApplication(JobApplicationRequest request, Long userId) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new ResourceNotFoundException("User", "id", userId));

        ApplicationStatus statusEnum = parseStatusRequired(request.getStatus());

        JobApplication application = JobApplication.builder()
                .companyName(request.getCompanyName())
                .jobTitle(request.getJobTitle())
                .jobDescription(request.getJobDescription())
                .status(statusEnum)
                .appliedDate(request.getAppliedDate())
                .location(request.getLocation())
                .salaryRange(request.getSalaryRange())
                .applicationUrl(request.getApplicationUrl())
                .notes(request.getNotes())
                .user(user)
                .build();

        application = applicationRepository.save(application);
        log.info("Created application {} for user {} — {} at {}",
                application.getId(), userId, application.getJobTitle(), application.getCompanyName());

        return mapToResponse(application);
    }

    /**
     * Update an existing job application.
     * Only the owning user can update their application.
     *
     * @throws ResourceNotFoundException if application not found or doesn't belong to user
     */
    @Transactional
    @CacheEvict(value = "dashboardStats", key = "#userId")
    public JobApplicationResponse updateApplication(Long id, JobApplicationRequest request, Long userId) {
        JobApplication application = applicationRepository.findByIdAndUserId(id, userId)
                .orElseThrow(() -> new ResourceNotFoundException("Job Application", "id", id));

        ApplicationStatus statusEnum = parseStatusRequired(request.getStatus());

        application.setCompanyName(request.getCompanyName());
        application.setJobTitle(request.getJobTitle());
        application.setJobDescription(request.getJobDescription());
        application.setStatus(statusEnum);
        application.setAppliedDate(request.getAppliedDate());
        application.setLocation(request.getLocation());
        application.setSalaryRange(request.getSalaryRange());
        application.setApplicationUrl(request.getApplicationUrl());
        application.setNotes(request.getNotes());

        application = applicationRepository.save(application);
        log.info("Updated application {} for user {}", id, userId);

        return mapToResponse(application);
    }

    /**
     * Delete a job application.
     * Only the owning user can delete their application.
     *
     * @throws ResourceNotFoundException if application not found or doesn't belong to user
     */
    @Transactional
    @CacheEvict(value = "dashboardStats", key = "#userId")
    public void deleteApplication(Long id, Long userId) {
        JobApplication application = applicationRepository.findByIdAndUserId(id, userId)
                .orElseThrow(() -> new ResourceNotFoundException("Job Application", "id", id));

        applicationRepository.delete(application);
        log.info("Deleted application {} for user {}", id, userId);
    }

    /**
     * Get dashboard statistics for the authenticated user.
     * Returns counts for each application status plus total.
     */
    @Transactional(readOnly = true)
    @Cacheable(value = "dashboardStats", key = "#userId")
    public DashboardStatsResponse getDashboardStats(Long userId) {
        DashboardStatsResponse stats = DashboardStatsResponse.builder()
                .totalApplications(applicationRepository.countByUserId(userId))
                .appliedCount(applicationRepository.countByUserIdAndStatus(userId, ApplicationStatus.APPLIED))
                .screeningCount(applicationRepository.countByUserIdAndStatus(userId, ApplicationStatus.SCREENING))
                .interviewCount(applicationRepository.countByUserIdAndStatus(userId, ApplicationStatus.INTERVIEW))
                .offerCount(applicationRepository.countByUserIdAndStatus(userId, ApplicationStatus.OFFER))
                .rejectedCount(applicationRepository.countByUserIdAndStatus(userId, ApplicationStatus.REJECTED))
                .withdrawnCount(applicationRepository.countByUserIdAndStatus(userId, ApplicationStatus.WITHDRAWN))
                .build();

        log.debug("Dashboard stats for user {}: {} total applications", userId, stats.getTotalApplications());
        return stats;
    }

    // ---- Helper methods ----

    /**
     * Parse a status string to ApplicationStatus enum.
     * Returns null if input is null or blank (used for optional filtering).
     *
     * @throws BadRequestException if the status string is not a valid enum value
     */
    private ApplicationStatus parseStatus(String status) {
        if (status == null || status.isBlank()) {
            return null;
        }
        try {
            return ApplicationStatus.valueOf(status.toUpperCase());
        } catch (IllegalArgumentException e) {
            throw new BadRequestException("Invalid status: '" + status +
                    "'. Valid values: APPLIED, SCREENING, INTERVIEW, OFFER, REJECTED, WITHDRAWN");
        }
    }

    /**
     * Parse a status string that is required (non-null).
     *
     * @throws BadRequestException if the status is null, blank, or invalid
     */
    private ApplicationStatus parseStatusRequired(String status) {
        if (status == null || status.isBlank()) {
            throw new BadRequestException("Status is required");
        }
        return parseStatus(status);
    }

    /**
     * Map JobApplication entity to response DTO.
     */
    private JobApplicationResponse mapToResponse(JobApplication application) {
        return JobApplicationResponse.builder()
                .id(application.getId())
                .companyName(application.getCompanyName())
                .jobTitle(application.getJobTitle())
                .jobDescription(application.getJobDescription())
                .status(application.getStatus().name())
                .appliedDate(application.getAppliedDate())
                .location(application.getLocation())
                .salaryRange(application.getSalaryRange())
                .applicationUrl(application.getApplicationUrl())
                .notes(application.getNotes())
                .createdAt(application.getCreatedAt())
                .updatedAt(application.getUpdatedAt())
                .build();
    }
}
