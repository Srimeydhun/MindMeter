package com.classroom.mindmeter.dto;

public class AnswerSaveRequest {

    private String answer;

    public AnswerSaveRequest() {
    }

    public AnswerSaveRequest(String answer) {
        this.answer = answer;
    }

    public String getAnswer() {
        return answer;
    }

    public void setAnswer(String answer) {
        this.answer = answer;
    }
}
