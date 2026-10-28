package com.classroom.mindmeter.repository;

import com.classroom.mindmeter.model.Subject;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface SubjectRepository extends JpaRepository<Subject, Long> {

    Optional<Subject> findByName(String name);

    boolean existsByName(String name);

    List<Subject> findByTeacherId(Long teacherId);
}