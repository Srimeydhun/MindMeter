package com.classroom.mindmeter.dto;

import java.time.LocalDateTime;
import java.time.OffsetDateTime;

public class AttemptResponse {

    private Long id;

    private Long quizId;
    private String quizTitle;
    private String subjectName;

    private Long studentId;
    private String studentName;
    private String studentEmail;
    private String registerNumber;

    private Integer attemptNumber;

    private Integer score;
    private Integer totalMarks;
    private Integer correctAnswers;
    private Integer incorrectAnswers;

    private LocalDateTime startTime;
    private LocalDateTime endTime;
    private OffsetDateTime submittedAt;

    private Boolean submitted;

    public AttemptResponse() {
    }

    public AttemptResponse(
            Long id,
            Long quizId,
            String quizTitle,
            Long studentId,
            String studentName,
            String studentEmail,
            String registerNumber,
            Integer attemptNumber,
            Integer score,
            Integer totalMarks,
            LocalDateTime startTime,
            LocalDateTime endTime,
            Boolean submitted) {

        this.id = id;
        this.quizId = quizId;
        this.quizTitle = quizTitle;
        this.studentId = studentId;
        this.studentName = studentName;
        this.studentEmail = studentEmail;
        this.registerNumber = registerNumber;
        this.attemptNumber = attemptNumber;
        this.score = score;
        this.totalMarks = totalMarks;
        this.startTime = startTime;
        this.endTime = endTime;
        this.submitted = submitted;
    }

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public Long getQuizId() {
        return quizId;
    }

    public void setQuizId(Long quizId) {
        this.quizId = quizId;
    }

    public String getQuizTitle() {
        return quizTitle;
    }

    public void setQuizTitle(String quizTitle) {
        this.quizTitle = quizTitle;
    }

    public String getSubjectName() {
        return subjectName;
    }

    public void setSubjectName(String subjectName) {
        this.subjectName = subjectName;
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

    public String getRegisterNumber() {
        return registerNumber;
    }

    public void setRegisterNumber(String registerNumber) {
        this.registerNumber = registerNumber;
    }

    public Integer getAttemptNumber() {
        return attemptNumber;
    }

    public void setAttemptNumber(Integer attemptNumber) {
        this.attemptNumber = attemptNumber;
    }

    public Integer getScore() {
        return score;
    }

    public void setScore(Integer score) {
        this.score = score;
    }

    public Integer getTotalMarks() {
        return totalMarks;
    }

    public void setTotalMarks(Integer totalMarks) {
        this.totalMarks = totalMarks;
    }

    public Integer getCorrectAnswers() {
        return correctAnswers;
    }

    public void setCorrectAnswers(Integer correctAnswers) {
        this.correctAnswers = correctAnswers;
    }

    public Integer getIncorrectAnswers() {
        return incorrectAnswers;
    }

    public void setIncorrectAnswers(Integer incorrectAnswers) {
        this.incorrectAnswers = incorrectAnswers;
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

    public OffsetDateTime getSubmittedAt() {
        return submittedAt;
    }

    public void setSubmittedAt(OffsetDateTime submittedAt) {
        this.submittedAt = submittedAt;
    }

    public Boolean getSubmitted() {
        return submitted;
    }

    public void setSubmitted(Boolean submitted) {
        this.submitted = submitted;
    }
}