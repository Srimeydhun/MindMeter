package com.classroom.mindmeter.service;

import com.classroom.mindmeter.model.Subject;
import com.classroom.mindmeter.model.User;
import com.classroom.mindmeter.repository.SubjectRepository;
import com.classroom.mindmeter.repository.UserRepository;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
public class SubjectService {

    private final SubjectRepository subjectRepository;
    private final UserRepository userRepository;

    public SubjectService(
            SubjectRepository subjectRepository,
            UserRepository userRepository) {

        this.subjectRepository = subjectRepository;
        this.userRepository = userRepository;
    }

    /*
     * =========================================================
     * GET ALL SUBJECTS
     * =========================================================
     *
     * Used by existing student functionality.
     */
    public List<Subject> getAllSubjects() {

        return subjectRepository.findAll();
    }

    /*
     * =========================================================
     * GET SUBJECTS BY TEACHER ID
     * =========================================================
     */
    public List<Subject> getSubjectsByTeacherId(Long teacherId) {

        return subjectRepository.findByTeacherId(teacherId);
    }

    /*
     * =========================================================
     * GET SUBJECTS OF LOGGED-IN TEACHER
     * =========================================================
     */
    public List<Subject> getSubjectsByTeacherEmail(String email) {

        User teacher = userRepository.findByEmail(email)
                .orElseThrow(() ->
                        new RuntimeException("Teacher not found")
                );

        return subjectRepository.findByTeacherId(
                teacher.getId()
        );
    }

    /*
     * =========================================================
     * CREATE SUBJECT
     * =========================================================
     *
     * Teacher email comes from JWT authentication.
     *
     * Frontend does NOT send teacher ID.
     */
    public Subject createSubject(
            String name,
            String description,
            String teacherEmail) {

        if (name == null || name.trim().isEmpty()) {

            throw new RuntimeException(
                    "Subject name cannot be empty"
            );
        }

        String trimmedName = name.trim();

        /*
         * Prevent duplicate subject names.
         */
        if (subjectRepository.existsByName(trimmedName)) {

            throw new RuntimeException(
                    "Subject already exists: " + trimmedName
            );
        }

        /*
         * Find the logged-in teacher.
         */
        User teacher = userRepository.findByEmail(teacherEmail)
                .orElseThrow(() ->
                        new RuntimeException("Teacher not found")
                );

        /*
         * Make sure the logged-in user is a teacher.
         */
        if (teacher.getRole() == null ||
                !"TEACHER".equalsIgnoreCase(
                        teacher.getRole().toString()
                )) {

            throw new RuntimeException(
                    "Only teachers can create subjects"
            );
        }

        /*
         * Create subject.
         */
        Subject subject = new Subject();

        subject.setName(trimmedName);

        subject.setDescription(
                description == null
                        ? ""
                        : description.trim()
        );

        /*
         * IMPORTANT:
         *
         * Associate subject with logged-in teacher.
         */
        subject.setTeacher(teacher);

        return subjectRepository.save(subject);
    }

    /*
     * =========================================================
     * UPDATE SUBJECT
     * =========================================================
     *
     * Only the teacher who owns the subject can update it.
     */
    public Subject updateSubject(
            Long subjectId,
            String name,
            String description,
            String teacherEmail) {

        /*
         * Find subject.
         */
        Subject subject = subjectRepository.findById(subjectId)
                .orElseThrow(() ->
                        new RuntimeException(
                                "Subject not found"
                        )
                );

        /*
         * Find logged-in teacher.
         */
        User teacher = userRepository.findByEmail(teacherEmail)
                .orElseThrow(() ->
                        new RuntimeException(
                                "Teacher not found"
                        )
                );

        /*
         * Check ownership.
         */
        if (subject.getTeacher() == null ||
                !subject.getTeacher()
                        .getId()
                        .equals(teacher.getId())) {

            throw new RuntimeException(
                    "You are not allowed to modify this subject"
            );
        }

        /*
         * Validate name.
         */
        if (name == null || name.trim().isEmpty()) {

            throw new RuntimeException(
                    "Subject name cannot be empty"
            );
        }

        String trimmedName = name.trim();

        /*
         * Check duplicate name only if
         * the subject name is changed.
         */
        if (!subject.getName().equalsIgnoreCase(trimmedName)
                && subjectRepository.existsByName(trimmedName)) {

            throw new RuntimeException(
                    "Subject already exists: " + trimmedName
            );
        }

        subject.setName(trimmedName);

        subject.setDescription(
                description == null
                        ? ""
                        : description.trim()
        );

        return subjectRepository.save(subject);
    }

    /*
     * =========================================================
     * DELETE SUBJECT
     * =========================================================
     *
     * Only the teacher who owns the subject can delete it.
     */
    public void deleteSubject(
            Long subjectId,
            String teacherEmail) {

        /*
         * Find subject.
         */
        Subject subject = subjectRepository.findById(subjectId)
                .orElseThrow(() ->
                        new RuntimeException(
                                "Subject not found"
                        )
                );

        /*
         * Find logged-in teacher.
         */
        User teacher = userRepository.findByEmail(teacherEmail)
                .orElseThrow(() ->
                        new RuntimeException(
                                "Teacher not found"
                        )
                );

        /*
         * Check ownership.
         */
        if (subject.getTeacher() == null ||
                !subject.getTeacher()
                        .getId()
                        .equals(teacher.getId())) {

            throw new RuntimeException(
                    "You are not allowed to delete this subject"
            );
        }

        /*
         * Delete subject.
         */
        subjectRepository.delete(subject);
    }
}