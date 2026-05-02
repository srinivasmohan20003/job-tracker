package com.jobtracker.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

/**
 * Response DTO for AI resume analysis results.
 * Contains match percentage, matched/missing skills, and improvement suggestions.
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class AnalysisResponse {

    private int matchPercentage;
    private List<String> matchedSkills;
    private List<String> missingSkills;
    private List<String> suggestions;
}
