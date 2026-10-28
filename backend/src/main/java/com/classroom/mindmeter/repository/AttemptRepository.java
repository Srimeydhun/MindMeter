package com.classroom.mindmeter.repository;

import com.classroom.mindmeter.model.Attempt;
import com.classroom.mindmeter.model.Quiz;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import jakarta.persistence.LockModeType;

import java.util.List;
import java.util.Optional;

public interface AttemptRepository extends JpaRepository<Attempt, Long> {

    List<Attempt> findByUserId(Long userId);

    List<Attempt> findByQuizId(Long quizId);

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    List<Attempt> findByUserIdAndQuizId(Long userId, Long quizId);

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    Optional<Attempt> findFirstByUserIdAndQuizIdAndSubmittedFalseOrderByAttemptNumberDesc(
            Long userId,
            Long quizId
    );

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("SELECT q FROM Quiz q WHERE q.id = :quizId")
    Optional<Quiz> lockQuiz(
            @Param("quizId") Long quizId
    );

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("SELECT a FROM Attempt a WHERE a.id = :attemptId AND a.user.id = :userId")
    Optional<Attempt> findOwnedByIdForUpdate(
            @Param("attemptId") Long attemptId,
            @Param("userId") Long userId
    );

    long countByUserId(Long userId);

    @Query("""
            SELECT a
            FROM Attempt a
            JOIN FETCH a.user
            JOIN FETCH a.quiz q
            JOIN FETCH q.subject s
            WHERE s.teacher.email = :teacherEmail
              AND a.submitted = true
            """)
    List<Attempt> findAllByTeacherEmail(
            @Param("teacherEmail") String teacherEmail);

    @Query("""
            SELECT a
            FROM Attempt a
            JOIN FETCH a.user
            JOIN FETCH a.quiz q
            JOIN FETCH q.subject s
            WHERE s.id = :subjectId
              AND s.teacher.email = :teacherEmail
              AND a.submitted = true
            ORDER BY a.user.name ASC, a.user.registerNumber ASC
            """)
    List<Attempt> findSubmittedBySubjectAndTeacher(
            @Param("subjectId") Long subjectId,
            @Param("teacherEmail") String teacherEmail);

    @Query("""
            SELECT a
            FROM Attempt a
            JOIN FETCH a.user
            JOIN FETCH a.quiz q
            JOIN FETCH q.subject s
            WHERE q.id = :quizId
              AND s.teacher.email = :teacherEmail
              AND a.submitted = true
            ORDER BY LOWER(a.user.name) ASC,
                     LOWER(a.user.registerNumber) ASC
            """)
    List<Attempt> findSubmittedByQuizAndTeacher(
            @Param("quizId") Long quizId,
            @Param("teacherEmail") String teacherEmail);
}