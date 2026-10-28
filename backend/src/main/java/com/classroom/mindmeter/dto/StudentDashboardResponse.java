package com.classroom.mindmeter.dto;

import java.util.List;

public class StudentDashboardResponse {

    private long totalAttempts;

    private int cumulativeScore;

    private int cumulativeTotalMarks;

    private long totalPracticeSeconds;

    private long totalPracticeMinutes;

    private double totalPracticeHours;

    private double averagePercentage;

    private int learningStreakDays;

    private List<SubjectPerformanceResponse> subjectPerformance;

    public StudentDashboardResponse() {
    }

    public StudentDashboardResponse(
            long totalAttempts,
            int cumulativeScore,
            int cumulativeTotalMarks,
            long totalPracticeSeconds,
            long totalPracticeMinutes,
            double totalPracticeHours,
            double averagePercentage,
            int learningStreakDays,
            List<SubjectPerformanceResponse> subjectPerformance) {

        this.totalAttempts = totalAttempts;
        this.cumulativeScore = cumulativeScore;
        this.cumulativeTotalMarks = cumulativeTotalMarks;
        this.totalPracticeSeconds = totalPracticeSeconds;
        this.totalPracticeMinutes = totalPracticeMinutes;
        this.totalPracticeHours = totalPracticeHours;
        this.averagePercentage = averagePercentage;
        this.learningStreakDays = learningStreakDays;
        this.subjectPerformance = subjectPerformance;
    }

    public long getTotalAttempts() {
        return totalAttempts;
    }

    public void setTotalAttempts(long totalAttempts) {
        this.totalAttempts = totalAttempts;
    }

    public int getCumulativeScore() {
        return cumulativeScore;
    }

    public void setCumulativeScore(int cumulativeScore) {
        this.cumulativeScore = cumulativeScore;
    }

    public int getCumulativeTotalMarks() {
        return cumulativeTotalMarks;
    }

    public void setCumulativeTotalMarks(int cumulativeTotalMarks) {
        this.cumulativeTotalMarks = cumulativeTotalMarks;
    }

    public long getTotalPracticeMinutes() {
        return totalPracticeMinutes;
    }

    public long getTotalPracticeSeconds() {
        return totalPracticeSeconds;
    }

    public void setTotalPracticeSeconds(long totalPracticeSeconds) {
        this.totalPracticeSeconds = totalPracticeSeconds;
    }

    public void setTotalPracticeMinutes(long totalPracticeMinutes) {
        this.totalPracticeMinutes = totalPracticeMinutes;
    }

    public double getTotalPracticeHours() {
        return totalPracticeHours;
    }

    public void setTotalPracticeHours(double totalPracticeHours) {
        this.totalPracticeHours = totalPracticeHours;
    }

    public double getAveragePercentage() {
        return averagePercentage;
    }

    public void setAveragePercentage(double averagePercentage) {
        this.averagePercentage = averagePercentage;
    }

    public int getLearningStreakDays() {
        return learningStreakDays;
    }

    public void setLearningStreakDays(int learningStreakDays) {
        this.learningStreakDays = learningStreakDays;
    }

    public List<SubjectPerformanceResponse> getSubjectPerformance() {
        return subjectPerformance;
    }

    public void setSubjectPerformance(
            List<SubjectPerformanceResponse> subjectPerformance) {

        this.subjectPerformance = subjectPerformance;
    }
}