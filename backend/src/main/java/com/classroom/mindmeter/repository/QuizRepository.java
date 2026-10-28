package com.classroom.mindmeter.repository;

import com.classroom.mindmeter.model.Quiz;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.Optional;

public interface QuizRepository extends JpaRepository<Quiz, Long> {

    List<Quiz> findBySubjectId(Long subjectId);

    boolean existsByTitleAndSubjectId(String title, Long subjectId);

    @Query("""
            SELECT q
            FROM Quiz q
            LEFT JOIN FETCH q.subject s
            LEFT JOIN FETCH s.teacher
            WHERE q.id = :quizId
            """)
    Optional<Quiz> findWithSubjectAndTeacherById(
            @Param("quizId") Long quizId);
}