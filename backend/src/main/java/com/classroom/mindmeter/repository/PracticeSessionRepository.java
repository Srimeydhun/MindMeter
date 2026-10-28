package com.classroom.mindmeter.repository;

import com.classroom.mindmeter.model.PracticeSession;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface PracticeSessionRepository
        extends JpaRepository<PracticeSession, Long> {

    List<PracticeSession> findByUserId(Long userId);

    List<PracticeSession> findByUserIdAndSubjectId(
            Long userId,
            Long subjectId
    );

    long countByUserId(Long userId);
}