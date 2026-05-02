package com.jobtracker.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDate;
import java.time.LocalDateTime;

/**
 * Response DTO for job application data.
 * Excludes internal fields (user entity, hibernate proxies).
 * Includes all fields the frontend needs to display application cards.
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class JobApplicationResponse {

    private Long id;
    private String companyName;
    private String jobTitle;
    private String jobDescription;
    private String status;
    private LocalDate appliedDate;
    private String location;
    private String salaryRange;
    private String applicationUrl;
    private String notes;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
}
