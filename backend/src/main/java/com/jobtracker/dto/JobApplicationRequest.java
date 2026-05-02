package com.jobtracker.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDate;

/**
 * Request DTO for creating or updating a job application.
 * Validates all required fields using Jakarta Validation.
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
public class JobApplicationRequest {

    @NotBlank(message = "Company name is required")
    @Size(max = 150, message = "Company name must not exceed 150 characters")
    private String companyName;

    @NotBlank(message = "Job title is required")
    @Size(max = 200, message = "Job title must not exceed 200 characters")
    private String jobTitle;

    private String jobDescription;

    @NotNull(message = "Status is required")
    private String status;

    @NotNull(message = "Applied date is required")
    private LocalDate appliedDate;

    @Size(max = 150, message = "Location must not exceed 150 characters")
    private String location;

    @Size(max = 100, message = "Salary range must not exceed 100 characters")
    private String salaryRange;

    @Size(max = 500, message = "Application URL must not exceed 500 characters")
    private String applicationUrl;

    private String notes;
}
