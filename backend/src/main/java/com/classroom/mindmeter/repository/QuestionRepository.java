package com.classroom.mindmeter.repository;

import com.classroom.mindmeter.model.Question;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface QuestionRepository extends JpaRepository<Question, Long> {

    List<Question> findByQuizId(Long quizId);

    void deleteByQuizId(Long quizId);
}