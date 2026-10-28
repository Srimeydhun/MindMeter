package com.classroom.mindmeter.dto;

import java.time.LocalDateTime;

public class PracticeSessionResponse {

    private Long id;

    private Long studentId;
    private String studentName;
    private String studentEmail;

    private Long subjectId;
    private String subjectName;

    private LocalDateTime startTime;
    private LocalDateTime endTime;

    private Integer durationMinutes;

    private Long durationSeconds;

    public PracticeSessionResponse() {
    }

    public PracticeSessionResponse(
            Long id,
            Long studentId,
            String studentName,
            String studentEmail,
            Long subjectId,
            String subjectName,
            LocalDateTime startTime,
            LocalDateTime endTime,
            Integer durationMinutes) {
        this(
                id,
                studentId,
                studentName,
                studentEmail,
                subjectId,
                subjectName,
                startTime,
                endTime,
                durationMinutes,
                durationMinutes == null ? null : durationMinutes * 60L
        );
    }

    public PracticeSessionResponse(
            Long id,
            Long studentId,
            String studentName,
            String studentEmail,
            Long subjectId,
            String subjectName,
            LocalDateTime startTime,
            LocalDateTime endTime,
            Integer durationMinutes,
            Long durationSeconds) {

        this.id = id;
        this.studentId = studentId;
        this.studentName = studentName;
        this.studentEmail = studentEmail;
        this.subjectId = subjectId;
        this.subjectName = subjectName;
        this.startTime = startTime;
        this.endTime = endTime;
        this.durationMinutes = durationMinutes;
        this.durationSeconds = durationSeconds;
    }

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public Long getStudentId() {
        return studentId;
    }

    public void setStudentId(Long studentId) {
        this.studentId = studentId;
    }

    public String getStudentName() {
        return studentName;
    }

    public void setStudentName(String studentName) {
        this.studentName = studentName;
    }

    public String getStudentEmail() {
        return studentEmail;
    }

    public void setStudentEmail(String studentEmail) {
        this.studentEmail = studentEmail;
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

    public LocalDateTime getStartTime() {
        return startTime;
    }

    public void setStartTime(LocalDateTime startTime) {
        this.startTime = startTime;
    }

    public LocalDateTime getEndTime() {
        return endTime;
    }

    public void setEndTime(LocalDateTime endTime) {
        this.endTime = endTime;
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
}