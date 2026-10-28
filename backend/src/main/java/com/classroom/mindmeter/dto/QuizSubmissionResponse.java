package com.classroom.mindmeter.dto;

import java.time.OffsetDateTime;

public class QuizSubmissionResponse {

    private String studentName;
    private String registerNumber;
    private String quizTitle;
    private String subjectName;
    private OffsetDateTime submittedAt;
    private Integer marksObtained;
    private Integer totalMarks;
    private String status;

    public QuizSubmissionResponse(
            String studentName,
            String registerNumber,
            String quizTitle,
            String subjectName,
            OffsetDateTime submittedAt,
            Integer marksObtained,
            Integer totalMarks,
            String status) {
        this.studentName = studentName;
        this.registerNumber = registerNumber;
        this.quizTitle = quizTitle;
        this.subjectName = subjectName;
        this.submittedAt = submittedAt;
        this.marksObtained = marksObtained;
        this.totalMarks = totalMarks;
        this.status = status;
    }

    public String getStudentName() {
        return studentName;
    }

    public String getRegisterNumber() {
        return registerNumber;
    }

    public String getQuizTitle() {
        return quizTitle;
    }

    public String getSubjectName() {
        return subjectName;
    }

    public OffsetDateTime getSubmittedAt() {
        return submittedAt;
    }

    public Integer getMarksObtained() {
        return marksObtained;
    }

    public Integer getTotalMarks() {
        return totalMarks;
    }

    public String getStatus() {
        return status;
    }
}
