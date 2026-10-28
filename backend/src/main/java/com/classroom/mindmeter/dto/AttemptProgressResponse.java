package com.classroom.mindmeter.dto;

import java.util.Map;

public class AttemptProgressResponse {

    private Long attemptId;
    private Long quizId;
    private Integer attemptNumber;
    private String status;
    private Map<Long, String> answers;
    private long remainingSeconds;
    private long pauseSecondsRemaining;
    private AttemptResponse result;

    public AttemptProgressResponse() {
    }

    public AttemptProgressResponse(
            Long attemptId,
            Long quizId,
            Integer attemptNumber,
            String status,
            Map<Long, String> answers,
            long remainingSeconds,
            long pauseSecondsRemaining,
            AttemptResponse result
    ) {
        this.attemptId = attemptId;
        this.quizId = quizId;
        this.attemptNumber = attemptNumber;
        this.status = status;
        this.answers = answers;
        this.remainingSeconds = remainingSeconds;
        this.pauseSecondsRemaining = pauseSecondsRemaining;
        this.result = result;
    }

    public Long getAttemptId() {
        return attemptId;
    }

    public Long getQuizId() {
        return quizId;
    }

    public Integer getAttemptNumber() {
        return attemptNumber;
    }

    public String getStatus() {
        return status;
    }

    public Map<Long, String> getAnswers() {
        return answers;
    }

    public long getRemainingSeconds() {
        return remainingSeconds;
    }

    public long getPauseSecondsRemaining() {
        return pauseSecondsRemaining;
    }

    public AttemptResponse getResult() {
        return result;
    }
}
