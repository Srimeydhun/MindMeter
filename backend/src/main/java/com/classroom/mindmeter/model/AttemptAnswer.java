package com.classroom.mindmeter.model;

import jakarta.persistence.*;

@Entity
@Table(
        name = "attempt_answers",
        uniqueConstraints = @UniqueConstraint(
                name = "uk_attempt_answer_question",
                columnNames = {"attempt_id", "question_id"}
        )
)
public class AttemptAnswer {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "attempt_id", nullable = false)
    private Attempt attempt;

    @Column(name = "question_id", nullable = false)
    private Long questionId;

    @Column(nullable = false, length = 1)
    private String answer;

    public AttemptAnswer() {
    }

    public AttemptAnswer(Attempt attempt, Long questionId, String answer) {
        this.attempt = attempt;
        this.questionId = questionId;
        this.answer = answer;
    }

    public Long getId() {
        return id;
    }

    public Attempt getAttempt() {
        return attempt;
    }

    public Long getQuestionId() {
        return questionId;
    }

    public String getAnswer() {
        return answer;
    }

    public void setAnswer(String answer) {
        this.answer = answer;
    }
}
