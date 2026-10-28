package com.classroom.mindmeter.dto;

public class TeacherDashboardResponse {

    private long totalStudents;
    private long totalSubjects;
    private long totalQuizzes;
    private long totalQuestions;
    private long totalAttempts;

    public TeacherDashboardResponse() {
    }

    public TeacherDashboardResponse(
            long totalStudents,
            long totalSubjects,
            long totalQuizzes,
            long totalQuestions,
            long totalAttempts) {

        this.totalStudents = totalStudents;
        this.totalSubjects = totalSubjects;
        this.totalQuizzes = totalQuizzes;
        this.totalQuestions = totalQuestions;
        this.totalAttempts = totalAttempts;
    }

    public long getTotalStudents() {
        return totalStudents;
    }

    public void setTotalStudents(long totalStudents) {
        this.totalStudents = totalStudents;
    }

    public long getTotalSubjects() {
        return totalSubjects;
    }

    public void setTotalSubjects(long totalSubjects) {
        this.totalSubjects = totalSubjects;
    }

    public long getTotalQuizzes() {
        return totalQuizzes;
    }

    public void setTotalQuizzes(long totalQuizzes) {
        this.totalQuizzes = totalQuizzes;
    }

    public long getTotalQuestions() {
        return totalQuestions;
    }

    public void setTotalQuestions(long totalQuestions) {
        this.totalQuestions = totalQuestions;
    }

    public long getTotalAttempts() {
        return totalAttempts;
    }

    public void setTotalAttempts(long totalAttempts) {
        this.totalAttempts = totalAttempts;
    }
}