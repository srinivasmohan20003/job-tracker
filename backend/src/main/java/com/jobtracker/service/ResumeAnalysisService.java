package com.jobtracker.service;

import com.jobtracker.dto.AnalysisResponse;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.util.*;
import java.util.stream.Collectors;

/**
 * AI-powered resume analysis service.
 *
 * Compares resume text against a job description using keyword matching
 * with text normalization and stopword removal.
 *
 * Algorithm:
 * 1. Normalize both texts (lowercase, strip punctuation)
 * 2. Remove common English stopwords
 * 3. Extract unique keywords from each text
 * 4. Calculate weighted match percentage:
 *    - Exact keyword matches (weight: 1.0)
 *    - Partial/substring matches (weight: 0.5)
 * 5. Identify missing skills from the job description
 * 6. Generate improvement suggestions based on gaps
 */
@Service
@Slf4j
public class ResumeAnalysisService {

    /**
     * Common English stopwords to filter out from analysis.
     */
    private static final Set<String> STOPWORDS = Set.of(
            "a", "an", "the", "and", "or", "but", "in", "on", "at", "to", "for",
            "of", "with", "by", "from", "is", "it", "as", "be", "was", "were",
            "are", "been", "being", "have", "has", "had", "do", "does", "did",
            "will", "would", "could", "should", "may", "might", "shall", "can",
            "this", "that", "these", "those", "i", "you", "he", "she", "we",
            "they", "me", "him", "her", "us", "them", "my", "your", "his",
            "its", "our", "their", "what", "which", "who", "whom", "when",
            "where", "why", "how", "all", "each", "every", "both", "few",
            "more", "most", "other", "some", "such", "no", "not", "only",
            "own", "same", "so", "than", "too", "very", "just", "about",
            "above", "after", "again", "against", "also", "am", "any",
            "because", "before", "between", "below", "down", "during",
            "further", "get", "if", "into", "new", "now", "off", "once",
            "out", "over", "then", "there", "through", "under", "until",
            "up", "while", "able", "etc", "per", "via", "using", "used",
            "must", "need", "including", "well", "work", "working", "role",
            "position", "company", "team", "experience", "years", "year",
            "strong", "good", "excellent", "preferred", "required", "plus",
            "looking", "join", "opportunity", "responsibilities", "requirements",
            "qualifications", "description", "job", "apply", "candidate"
    );

    /**
     * Common technical skills and their variations for better matching.
     */
    private static final Map<String, Set<String>> SKILL_ALIASES = Map.ofEntries(
            Map.entry("javascript", Set.of("js", "ecmascript", "es6", "es2015")),
            Map.entry("typescript", Set.of("ts")),
            Map.entry("python", Set.of("py", "python3")),
            Map.entry("java", Set.of("jdk", "jre", "jvm")),
            Map.entry("react", Set.of("reactjs", "react.js")),
            Map.entry("angular", Set.of("angularjs", "angular.js")),
            Map.entry("vue", Set.of("vuejs", "vue.js")),
            Map.entry("node", Set.of("nodejs", "node.js")),
            Map.entry("spring", Set.of("springboot", "spring-boot")),
            Map.entry("aws", Set.of("amazon-web-services")),
            Map.entry("gcp", Set.of("google-cloud")),
            Map.entry("docker", Set.of("containerization", "containers")),
            Map.entry("kubernetes", Set.of("k8s")),
            Map.entry("postgresql", Set.of("postgres")),
            Map.entry("mongodb", Set.of("mongo")),
            Map.entry("ci/cd", Set.of("cicd", "continuous-integration", "continuous-deployment")),
            Map.entry("rest", Set.of("restful", "rest-api")),
            Map.entry("graphql", Set.of("gql")),
            Map.entry("machine-learning", Set.of("ml")),
            Map.entry("artificial-intelligence", Set.of("ai")),
            Map.entry("sql", Set.of("mysql", "mssql"))
    );

    /**
     * Analyze a resume against a job description.
     *
     * @param resumeText     extracted text from the resume
     * @param jobDescription job description text
     * @return analysis results with match percentage, skills, and suggestions
     */
    public AnalysisResponse analyzeResume(String resumeText, String jobDescription) {
        if (resumeText == null || resumeText.isBlank()) {
            log.warn("Resume text is empty, returning zero match");
            return buildEmptyResponse("Resume text is empty. Please re-upload your resume.");
        }
        if (jobDescription == null || jobDescription.isBlank()) {
            log.warn("Job description is empty, returning zero match");
            return buildEmptyResponse("Job description is empty. Please add a job description to the application.");
        }

        // Step 1: Normalize and extract keywords
        Set<String> resumeKeywords = extractKeywords(resumeText);
        Set<String> jobKeywords = extractKeywords(jobDescription);

        log.debug("Resume keywords: {} | Job keywords: {}", resumeKeywords.size(), jobKeywords.size());

        if (jobKeywords.isEmpty()) {
            return buildEmptyResponse("Could not extract meaningful keywords from the job description.");
        }

        // Step 2: Find matched and missing skills with alias support
        Set<String> matchedSkills = new LinkedHashSet<>();
        Set<String> missingSkills = new LinkedHashSet<>();

        for (String jobKeyword : jobKeywords) {
            if (isKeywordPresent(jobKeyword, resumeKeywords)) {
                matchedSkills.add(jobKeyword);
            } else {
                missingSkills.add(jobKeyword);
            }
        }

        // Step 3: Calculate match percentage with weighted scoring
        int matchPercentage = calculateMatchPercentage(matchedSkills.size(), jobKeywords.size(), resumeKeywords, jobKeywords);

        // Step 4: Generate suggestions
        List<String> suggestions = generateSuggestions(matchedSkills, missingSkills, matchPercentage);

        log.info("Resume analysis complete: {}% match, {} matched, {} missing",
                matchPercentage, matchedSkills.size(), missingSkills.size());

        return AnalysisResponse.builder()
                .matchPercentage(matchPercentage)
                .matchedSkills(new ArrayList<>(matchedSkills))
                .missingSkills(new ArrayList<>(missingSkills))
                .suggestions(suggestions)
                .build();
    }

    /**
     * Extract meaningful keywords from text.
     * Normalizes to lowercase, removes punctuation, filters stopwords,
     * and keeps words with 2+ characters.
     */
    private Set<String> extractKeywords(String text) {
        String normalized = text.toLowerCase()
                .replaceAll("[^a-z0-9+#./\\-\\s]", " ")  // Keep tech chars: +, #, ., /, -
                .replaceAll("\\s+", " ")
                .trim();

        return Arrays.stream(normalized.split("\\s+"))
                .map(String::trim)
                .filter(word -> word.length() >= 2)
                .filter(word -> !STOPWORDS.contains(word))
                .filter(word -> !word.matches("\\d+"))  // Remove pure numbers
                .collect(Collectors.toCollection(LinkedHashSet::new));
    }

    /**
     * Check if a keyword is present in the resume keywords,
     * considering aliases and partial matches.
     */
    private boolean isKeywordPresent(String keyword, Set<String> resumeKeywords) {
        // Direct match
        if (resumeKeywords.contains(keyword)) {
            return true;
        }

        // Check aliases
        for (Map.Entry<String, Set<String>> entry : SKILL_ALIASES.entrySet()) {
            String canonical = entry.getKey();
            Set<String> aliases = entry.getValue();

            boolean keywordMatchesEntry = keyword.equals(canonical) || aliases.contains(keyword);
            if (keywordMatchesEntry) {
                // Check if resume has any form of this skill
                if (resumeKeywords.contains(canonical)) return true;
                for (String alias : aliases) {
                    if (resumeKeywords.contains(alias)) return true;
                }
            }
        }

        // Partial/substring match (e.g., "microservices" contains "service")
        for (String resumeKeyword : resumeKeywords) {
            if (resumeKeyword.contains(keyword) || keyword.contains(resumeKeyword)) {
                if (Math.min(resumeKeyword.length(), keyword.length()) >= 3) {
                    return true;
                }
            }
        }

        return false;
    }

    /**
     * Calculate weighted match percentage.
     * Base score from direct matches, bonus for partial/semantic overlap.
     */
    private int calculateMatchPercentage(int matchedCount, int totalJobKeywords,
                                         Set<String> resumeKeywords, Set<String> jobKeywords) {
        if (totalJobKeywords == 0) return 0;

        // Base match percentage
        double baseScore = (double) matchedCount / totalJobKeywords * 100;

        // Bonus for resume having many relevant extra keywords (indicates breadth)
        long relevantExtras = resumeKeywords.stream()
                .filter(rk -> !STOPWORDS.contains(rk))
                .filter(rk -> rk.length() >= 3)
                .count();

        double breadthBonus = Math.min(5, relevantExtras * 0.05);  // Max 5% bonus

        int finalScore = (int) Math.round(Math.min(100, baseScore + breadthBonus));
        return Math.max(0, finalScore);
    }

    /**
     * Generate contextual improvement suggestions based on analysis results.
     */
    private List<String> generateSuggestions(Set<String> matchedSkills,
                                              Set<String> missingSkills,
                                              int matchPercentage) {
        List<String> suggestions = new ArrayList<>();

        if (matchPercentage >= 80) {
            suggestions.add("Your resume is a strong match for this position. Focus on tailoring your summary/objective.");
        } else if (matchPercentage >= 60) {
            suggestions.add("Good foundation. Address the missing skills to strengthen your application.");
        } else if (matchPercentage >= 40) {
            suggestions.add("Moderate match. Consider adding relevant projects or certifications for the missing skills.");
        } else {
            suggestions.add("Low match. This role may require significant skill development or resume restructuring.");
        }

        // Specific missing skill suggestions
        if (!missingSkills.isEmpty()) {
            List<String> topMissing = missingSkills.stream().limit(5).toList();
            suggestions.add("Add these key skills to your resume: " + String.join(", ", topMissing));

            if (missingSkills.size() > 5) {
                suggestions.add("Consider online courses or certifications for: " +
                        missingSkills.stream().skip(5).limit(5)
                                .collect(Collectors.joining(", ")));
            }
        }

        // General improvement tips
        if (matchPercentage < 70) {
            suggestions.add("Use the exact terminology from the job description in your resume where applicable.");
            suggestions.add("Quantify your achievements (e.g., 'Improved performance by 40%') to stand out.");
        }

        if (!matchedSkills.isEmpty() && matchPercentage < 90) {
            suggestions.add("Highlight your matched skills (" +
                    matchedSkills.stream().limit(3).collect(Collectors.joining(", ")) +
                    ") prominently in your summary section.");
        }

        return suggestions;
    }

    /**
     * Build an empty analysis response with a single suggestion.
     */
    private AnalysisResponse buildEmptyResponse(String message) {
        return AnalysisResponse.builder()
                .matchPercentage(0)
                .matchedSkills(Collections.emptyList())
                .missingSkills(Collections.emptyList())
                .suggestions(List.of(message))
                .build();
    }
}
