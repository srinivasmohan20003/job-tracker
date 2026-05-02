package com.jobtracker.entity;

/**
 * Enum representing the status of a job application.
 * Tracks the application lifecycle from submission to final outcome.
 */
public enum ApplicationStatus {
    APPLIED,
    SCREENING,
    INTERVIEW,
    OFFER,
    REJECTED,
    WITHDRAWN
}
