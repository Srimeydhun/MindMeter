package com.classroom.mindmeter.dto;

import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.AssertTrue;

public class PracticeSessionRequest {

    @Min(value = 1, message = "Duration must be at least 1 minute")
    private Integer durationMinutes;

    @Min(value = 1, message = "Duration must be at least 1 second")
    @Max(value = 128849018820L, message = "Duration is too large")
    private Long durationSeconds;

    private Long subjectId;

    public PracticeSessionRequest() {
    }

    public PracticeSessionRequest(
            Integer durationMinutes,
            Long subjectId) {

        this.durationMinutes = durationMinutes;
        this.subjectId = subjectId;
    }

    public Integer getDurationMinutes() {
        return durationMinutes;
    }

    public void setDurationMinutes(Integer durationMinutes) {
        this.durationMinutes = durationMinutes;
    }

    public Long getDurationSeconds() {
        return durationSeconds;
    }

    public void setDurationSeconds(Long durationSeconds) {
        this.durationSeconds = durationSeconds;
    }

    @AssertTrue(message = "Provide either durationMinutes or durationSeconds")
    public boolean isDurationValid() {
        return (durationMinutes == null) != (durationSeconds == null);
    }

    public Long getSubjectId() {
        return subjectId;
    }

    public void setSubjectId(Long subjectId) {
        this.subjectId = subjectId;
    }
}