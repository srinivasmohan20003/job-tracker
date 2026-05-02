package com.jobtracker.entity;

import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;

import java.time.LocalDate;
import java.time.LocalDateTime;

/**
 * Entity representing a job application tracked by a user.
 *
 * Design decisions:
 * - Many-to-one with User (each application belongs to one user)
 * - Composite index on (user_id, status) for dashboard stats queries
 * - Index on (user_id, company_name) for company search queries
 * - appliedDate is LocalDate (no time component needed)
 * - jobDescription stored as TEXT for long job postings (used by AI analysis in Phase 3)
 * - Audit fields (createdAt, updatedAt) auto-managed by Hibernate
 */
@Entity
@Table(name = "job_applications", indexes = {
        @Index(name = "idx_app_user_id", columnList = "user_id"),
        @Index(name = "idx_app_user_status", columnList = "user_id, status"),
        @Index(name = "idx_app_user_company", columnList = "user_id, company_name"),
        @Index(name = "idx_app_applied_date", columnList = "user_id, applied_date")
})
@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class JobApplication {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "company_name", nullable = false, length = 150)
    private String companyName;

    @Column(name = "job_title", nullable = false, length = 200)
    private String jobTitle;

    @Column(name = "job_description", columnDefinition = "TEXT")
    private String jobDescription;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private ApplicationStatus status;

    @Column(name = "applied_date", nullable = false)
    private LocalDate appliedDate;

    @Column(length = 150)
    private String location;

    @Column(name = "salary_range", length = 100)
    private String salaryRange;

    @Column(name = "application_url", length = 500)
    private String applicationUrl;

    @Column(columnDefinition = "TEXT")
    private String notes;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id", nullable = false)
    private User user;

    @CreationTimestamp
    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;

    @UpdateTimestamp
    @Column(name = "updated_at")
    private LocalDateTime updatedAt;
}
