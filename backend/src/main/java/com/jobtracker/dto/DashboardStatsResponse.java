package com.jobtracker.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * Response DTO for dashboard statistics.
 * Provides aggregate counts of applications grouped by status.
 * Used by the frontend Dashboard page to display summary cards.
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class DashboardStatsResponse {

    private long totalApplications;
    private long appliedCount;
    private long screeningCount;
    private long interviewCount;
    private long offerCount;
    private long rejectedCount;
    private long withdrawnCount;
}
