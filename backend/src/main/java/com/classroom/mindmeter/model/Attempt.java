package com.classroom.mindmeter.model;

import jakarta.persistence.*;

import java.time.LocalDateTime;

@Entity
@Table(name = "attempts")
public class Attempt {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "user_id", nullable = false)
    private User user;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "quiz_id", nullable = false)
    private Quiz quiz;

    @Column(nullable = false)
    private Integer attemptNumber;

    @Column(nullable = false)
    private Integer score;

    @Column(nullable = false)
    private Integer totalMarks;

    @Column(nullable = false)
    private LocalDateTime startTime;

    @Column
    private LocalDateTime endTime;

    @Column(name = "duration_seconds", nullable = false,
            columnDefinition = "bigint default 0")
    private Long durationSeconds = 0L;

    @Column(nullable = false)
    private Boolean submitted;

    @Column(nullable = false, length = 20, columnDefinition = "varchar(20) default 'SUBMITTED'")
    private String status = "SUBMITTED";

    @Column
    private LocalDateTime deadlineTime;

    @Column
    private LocalDateTime pausedAt;

    @Column(nullable = false, columnDefinition = "integer default 0")
    private Integer pauseSecondsUsed = 0;

    public Attempt() {
    }

    public Attempt(User user, Quiz quiz, Integer attemptNumber,
                   Integer score, Integer totalMarks,
                   LocalDateTime startTime, LocalDateTime endTime,
                   Boolean submitted) {
        this.user = user;
        this.quiz = quiz;
        this.attemptNumber = attemptNumber;
        this.score = score;
        this.totalMarks = totalMarks;
        this.startTime = startTime;
        this.endTime = endTime;
        this.submitted = submitted;
        this.status = Boolean.TRUE.equals(submitted)
                ? "SUBMITTED"
                : "IN_PROGRESS";
    }

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public User getUser() {
        return user;
    }

    public void setUser(User user) {
        this.user = user;
    }

    public Quiz getQuiz() {
        return quiz;
    }

    public void setQuiz(Quiz quiz) {
        this.quiz = quiz;
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

    public Long getDurationSeconds() {
        return durationSeconds;
    }

    public void setDurationSeconds(Long durationSeconds) {
        this.durationSeconds = durationSeconds;
    }

    public Boolean getSubmitted() {
        return submitted;
    }

    public void setSubmitted(Boolean submitted) {
        this.submitted = submitted;
    }

    public String getStatus() {
        return status;
    }

    public void setStatus(String status) {
        this.status = status;
    }

    public LocalDateTime getDeadlineTime() {
        return deadlineTime;
    }

    public void setDeadlineTime(LocalDateTime deadlineTime) {
        this.deadlineTime = deadlineTime;
    }

    public LocalDateTime getPausedAt() {
        return pausedAt;
    }

    public void setPausedAt(LocalDateTime pausedAt) {
        this.pausedAt = pausedAt;
    }

    public Integer getPauseSecondsUsed() {
        return pauseSecondsUsed;
    }

    public void setPauseSecondsUsed(Integer pauseSecondsUsed) {
        this.pauseSecondsUsed = pauseSecondsUsed;
    }
}