package com.jobtracker.controller;

import com.jobtracker.dto.DashboardStatsResponse;
import com.jobtracker.dto.JobApplicationRequest;
import com.jobtracker.dto.JobApplicationResponse;
import com.jobtracker.entity.User;
import com.jobtracker.service.JobApplicationService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

/**
 * REST controller for job application management.
 * All endpoints require JWT authentication (configured in SecurityConfig).
 *
 * The authenticated user is injected via @AuthenticationPrincipal.
 * All data operations are scoped to the authenticated user — a user
 * can only access their own applications.
 *
 * Endpoints:
 * GET    /api/applications        → list with pagination, sorting, filtering
 * GET    /api/applications/stats  → dashboard statistics
 * GET    /api/applications/{id}   → get single application
 * POST   /api/applications        → create new application
 * PUT    /api/applications/{id}   → update existing application
 * DELETE /api/applications/{id}   → delete application
 */
@RestController
@RequestMapping("/api/applications")
@RequiredArgsConstructor
@Slf4j
public class JobApplicationController {

    private final JobApplicationService applicationService;

    /**
     * Get paginated list of job applications with optional filtering.
     *
     * Query parameters:
     * - page (default: 0) — page number (zero-based)
     * - size (default: 10) — page size
     * - sortBy (default: appliedDate) — field to sort by
     * - sortDir (default: desc) — sort direction (asc/desc)
     * - status (optional) — filter by application status
     * - company (optional) — search by company name (contains, case-insensitive)
     *
     * Example: GET /api/applications?page=0&size=10&sortBy=appliedDate&sortDir=desc&status=INTERVIEW&company=Google
     */
    @GetMapping
    public ResponseEntity<Page<JobApplicationResponse>> getApplications(
            @AuthenticationPrincipal User user,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int size,
            @RequestParam(defaultValue = "appliedDate") String sortBy,
            @RequestParam(defaultValue = "desc") String sortDir,
            @RequestParam(required = false) String status,
            @RequestParam(required = false) String company) {

        Sort sort = sortDir.equalsIgnoreCase("asc")
                ? Sort.by(sortBy).ascending()
                : Sort.by(sortBy).descending();

        Pageable pageable = PageRequest.of(page, size, sort);

        log.info("GET /api/applications — user={}, page={}, size={}, status={}, company={}",
                user.getEmail(), page, size, status, company);

        Page<JobApplicationResponse> applications =
                applicationService.getApplications(user.getId(), status, company, pageable);

        return ResponseEntity.ok(applications);
    }

    /**
     * Get dashboard statistics for the authenticated user.
     * Returns counts for each application status.
     *
     * Note: This endpoint is mapped BEFORE /{id} to avoid path conflicts.
     */
    @GetMapping("/stats")
    public ResponseEntity<DashboardStatsResponse> getDashboardStats(
            @AuthenticationPrincipal User user) {

        log.info("GET /api/applications/stats — user={}", user.getEmail());

        DashboardStatsResponse stats = applicationService.getDashboardStats(user.getId());
        return ResponseEntity.ok(stats);
    }

    /**
     * Get a single job application by ID.
     * Returns 404 if not found or doesn't belong to the authenticated user.
     */
    @GetMapping("/{id}")
    public ResponseEntity<JobApplicationResponse> getApplicationById(
            @PathVariable Long id,
            @AuthenticationPrincipal User user) {

        log.info("GET /api/applications/{} — user={}", id, user.getEmail());

        JobApplicationResponse application =
                applicationService.getApplicationById(id, user.getId());
        return ResponseEntity.ok(application);
    }

    /**
     * Create a new job application.
     * The application is automatically associated with the authenticated user.
     */
    @PostMapping
    public ResponseEntity<JobApplicationResponse> createApplication(
            @Valid @RequestBody JobApplicationRequest request,
            @AuthenticationPrincipal User user) {

        log.info("POST /api/applications — user={}, company={}, title={}",
                user.getEmail(), request.getCompanyName(), request.getJobTitle());

        JobApplicationResponse application =
                applicationService.createApplication(request, user.getId());
        return ResponseEntity.status(HttpStatus.CREATED).body(application);
    }

    /**
     * Update an existing job application.
     * Returns 404 if not found or doesn't belong to the authenticated user.
     */
    @PutMapping("/{id}")
    public ResponseEntity<JobApplicationResponse> updateApplication(
            @PathVariable Long id,
            @Valid @RequestBody JobApplicationRequest request,
            @AuthenticationPrincipal User user) {

        log.info("PUT /api/applications/{} — user={}", id, user.getEmail());

        JobApplicationResponse application =
                applicationService.updateApplication(id, request, user.getId());
        return ResponseEntity.ok(application);
    }

    /**
     * Delete a job application.
     * Returns 404 if not found or doesn't belong to the authenticated user.
     */
    @DeleteMapping("/{id}")
    public ResponseEntity<Void> deleteApplication(
            @PathVariable Long id,
            @AuthenticationPrincipal User user) {

        log.info("DELETE /api/applications/{} — user={}", id, user.getEmail());

        applicationService.deleteApplication(id, user.getId());
        return ResponseEntity.noContent().build();
    }
}
