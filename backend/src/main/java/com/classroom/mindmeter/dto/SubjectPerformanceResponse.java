package com.classroom.mindmeter.dto;

public class SubjectPerformanceResponse {

    private Long subjectId;
    private String subjectName;

    private long attempts;
    private int totalScore;
    private int totalMarks;

    private double percentage;

    public SubjectPerformanceResponse() {
    }

    public SubjectPerformanceResponse(
            Long subjectId,
            String subjectName,
            long attempts,
            int totalScore,
            int totalMarks,
            double percentage) {

        this.subjectId = subjectId;
        this.subjectName = subjectName;
        this.attempts = attempts;
        this.totalScore = totalScore;
        this.totalMarks = totalMarks;
        this.percentage = percentage;
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

    public long getAttempts() {
        return attempts;
    }

    public void setAttempts(long attempts) {
        this.attempts = attempts;
    }

    public int getTotalScore() {
        return totalScore;
    }

    public void setTotalScore(int totalScore) {
        this.totalScore = totalScore;
    }

    public int getTotalMarks() {
        return totalMarks;
    }

    public void setTotalMarks(int totalMarks) {
        this.totalMarks = totalMarks;
    }

    public double getPercentage() {
        return percentage;
    }

    public void setPercentage(double percentage) {
        this.percentage = percentage;
    }
}