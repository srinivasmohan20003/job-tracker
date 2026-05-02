package com.jobtracker.repository;

import com.jobtracker.entity.ApplicationStatus;
import com.jobtracker.entity.JobApplication;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.Optional;

/**
 * Repository for JobApplication entity.
 *
 * All queries are scoped to a specific user (user_id) to enforce
 * data isolation — users can only access their own applications.
 *
 * Custom queries:
 * - Paginated search with optional status and company filters
 * - Dashboard stats (count by status)
 * - Single application lookup scoped to user
 */
@Repository
public interface JobApplicationRepository extends JpaRepository<JobApplication, Long> {

    /**
     * Find all applications for a user with pagination.
     */
    Page<JobApplication> findByUserId(Long userId, Pageable pageable);

    /**
     * Find all applications for a user filtered by status.
     */
    Page<JobApplication> findByUserIdAndStatus(Long userId, ApplicationStatus status, Pageable pageable);

    /**
     * Search applications by company name (case-insensitive contains).
     */
    Page<JobApplication> findByUserIdAndCompanyNameContainingIgnoreCase(
            Long userId, String companyName, Pageable pageable);

    /**
     * Search applications with both status and company name filters.
     */
    Page<JobApplication> findByUserIdAndStatusAndCompanyNameContainingIgnoreCase(
            Long userId, ApplicationStatus status, String companyName, Pageable pageable);

    /**
     * Combined search query supporting optional status and company filters.
     * Uses JPQL with conditional WHERE clauses.
     * This single query replaces the need for multiple method combinations.
     */
    @Query("SELECT j FROM JobApplication j WHERE j.user.id = :userId " +
            "AND (:status IS NULL OR j.status = :status) " +
            "AND (:company IS NULL OR LOWER(j.companyName) LIKE LOWER(CONCAT('%', :company, '%')))")
    Page<JobApplication> searchApplications(
            @Param("userId") Long userId,
            @Param("status") ApplicationStatus status,
            @Param("company") String company,
            Pageable pageable);

    /**
     * Find a single application by ID, scoped to a specific user.
     * Returns Optional.empty() if the application doesn't belong to the user.
     */
    Optional<JobApplication> findByIdAndUserId(Long id, Long userId);

    /**
     * Count applications by status for a specific user (for dashboard stats).
     */
    long countByUserIdAndStatus(Long userId, ApplicationStatus status);

    /**
     * Count total applications for a user.
     */
    long countByUserId(Long userId);
}
