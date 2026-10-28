package com.classroom.mindmeter.dto;

public class QuizAttendanceResponse {

    private Long quizId;
    private String quizTitle;
    private String subjectName;

    private long studentsAttended;
    private long totalAttempts;

    public QuizAttendanceResponse() {
    }

    public QuizAttendanceResponse(
            Long quizId,
            String quizTitle,
            String subjectName,
            long studentsAttended,
            long totalAttempts) {

        this.quizId = quizId;
        this.quizTitle = quizTitle;
        this.subjectName = subjectName;
        this.studentsAttended = studentsAttended;
        this.totalAttempts = totalAttempts;
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

    public long getStudentsAttended() {
        return studentsAttended;
    }

    public void setStudentsAttended(long studentsAttended) {
        this.studentsAttended = studentsAttended;
    }

    public long getTotalAttempts() {
        return totalAttempts;
    }

    public void setTotalAttempts(long totalAttempts) {
        this.totalAttempts = totalAttempts;
    }
}