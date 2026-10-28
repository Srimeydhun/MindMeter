package com.classroom.mindmeter.dto;

public class QuizResponse {

    private Long id;
    private String title;
    private String description;
    private Integer durationMinutes;
    private Integer maxAttempts;
    private Long subjectId;
    private String subjectName;

    public QuizResponse() {
    }

    public QuizResponse(
            Long id,
            String title,
            String description,
            Integer durationMinutes,
            Integer maxAttempts,
            Long subjectId,
            String subjectName) {

        this.id = id;
        this.title = title;
        this.description = description;
        this.durationMinutes = durationMinutes;
        this.maxAttempts = maxAttempts;
        this.subjectId = subjectId;
        this.subjectName = subjectName;
    }

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public String getTitle() {
        return title;
    }

    public void setTitle(String title) {
        this.title = title;
    }

    public String getDescription() {
        return description;
    }

    public void setDescription(String description) {
        this.description = description;
    }

    public Integer getDurationMinutes() {
        return durationMinutes;
    }

    public void setDurationMinutes(Integer durationMinutes) {
        this.durationMinutes = durationMinutes;
    }

    public Integer getMaxAttempts() {
        return maxAttempts;
    }

    public void setMaxAttempts(Integer maxAttempts) {
        this.maxAttempts = maxAttempts;
    }

    public Long getSubjectId() {
        return subjectId;
    }

    public void setSubjectId(Long subjectId) {
        this.subjectId = subjectId;
    }

    public String getSubjectName() {
        return subjectName;
    }

    public void setSubjectName(String subjectName) {
        this.subjectName = subjectName;
    }
}