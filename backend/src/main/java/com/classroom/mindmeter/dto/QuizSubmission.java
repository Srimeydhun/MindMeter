package com.classroom.mindmeter.dto;

import java.util.Map;

public class QuizSubmission {

    private Long quizId;

    private Long attemptId;

    private Map<Long, String> answers;

    public QuizSubmission() {
    }

    public QuizSubmission(
            Long quizId,
            Map<Long, String> answers) {

        this.quizId = quizId;
        this.answers = answers;
    }

    public Long getAttemptId() {
        return attemptId;
    }

    public void setAttemptId(Long attemptId) {
        this.attemptId = attemptId;
    }

    public Long getQuizId() {
        return quizId;
    }

    public void setQuizId(Long quizId) {
        this.quizId = quizId;
    }

    public Map<Long, String> getAnswers() {
        return answers;
    }

    public void setAnswers(Map<Long, String> answers) {
        this.answers = answers;
    }
}