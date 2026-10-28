package com.classroom.mindmeter.service;

import com.classroom.mindmeter.dto.QuizRequest;
import com.classroom.mindmeter.dto.QuizResponse;
import com.classroom.mindmeter.model.Quiz;
import com.classroom.mindmeter.model.Subject;
import com.classroom.mindmeter.model.User;
import com.classroom.mindmeter.repository.QuizRepository;
import com.classroom.mindmeter.repository.SubjectRepository;
import com.classroom.mindmeter.repository.UserRepository;

import org.springframework.http.HttpStatus;
import org.springframework.web.server.ResponseStatusException;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
public class QuizService {

    private final QuizRepository quizRepository;
    private final SubjectRepository subjectRepository;
    private final UserRepository userRepository;

    public QuizService(
            QuizRepository quizRepository,
            SubjectRepository subjectRepository,
            UserRepository userRepository) {

        this.quizRepository = quizRepository;
        this.subjectRepository = subjectRepository;
        this.userRepository = userRepository;
    }

    // ==========================================
    // CREATE QUIZ
    // ==========================================

    public QuizResponse createQuiz(
            QuizRequest request,
            String teacherEmail) {

        User teacher = getTeacherByEmail(teacherEmail);

        Subject subject = subjectRepository.findById(
                request.getSubjectId()
        ).orElseThrow(() ->
                new RuntimeException(
                        "Subject not found with ID: "
                                + request.getSubjectId()
                )
        );

        verifySubjectOwnership(subject, teacher);

        if (quizRepository.existsByTitleAndSubjectId(
                request.getTitle(),
                request.getSubjectId())) {

            throw new RuntimeException(
                    "Quiz already exists for this subject"
            );
        }

        Quiz quiz = new Quiz();

        quiz.setTitle(request.getTitle().trim());
        quiz.setDescription(
                request.getDescription() == null
                        ? ""
                        : request.getDescription().trim()
        );
        quiz.setDurationMinutes(
                request.getDurationMinutes()
        );
        quiz.setMaxAttempts(
                request.getMaxAttempts()
        );
        quiz.setSubject(subject);

        Quiz savedQuiz = quizRepository.save(quiz);

        return convertToResponse(savedQuiz);
    }

    // ==========================================
    // GET ALL QUIZZES
    // ==========================================

    public List<QuizResponse> getAllQuizzes() {

        return quizRepository.findAll()
                .stream()
                .map(this::convertToResponse)
                .toList();
    }

    // ==========================================
    // GET QUIZ BY ID
    // ==========================================

    public QuizResponse getQuizById(Long id) {

        Quiz quiz = quizRepository.findById(id)
                .orElseThrow(() ->
                        new ResponseStatusException(
                                HttpStatus.NOT_FOUND,
                                "Quiz not found with ID: " + id
                        )
                );

        return convertToResponse(quiz);
    }

    // ==========================================
    // GET QUIZZES BY SUBJECT
    // ==========================================

    public List<QuizResponse> getQuizzesBySubject(
            Long subjectId) {

        if (!subjectRepository.existsById(subjectId)) {
            throw new RuntimeException(
                    "Subject not found with ID: " + subjectId
            );
        }

        return quizRepository.findBySubjectId(subjectId)
                .stream()
                .map(this::convertToResponse)
                .toList();
    }

    // ==========================================
    // UPDATE QUIZ
    // ==========================================

    public QuizResponse updateQuiz(
            Long id,
            QuizRequest request,
            String teacherEmail) {

        User teacher = getTeacherByEmail(teacherEmail);

        Quiz quiz = quizRepository.findById(id)
                .orElseThrow(() ->
                        new RuntimeException(
                                "Quiz not found with ID: " + id
                        )
                );

        // Verify that the existing quiz belongs
        // to a course owned by this teacher.
        verifySubjectOwnership(
                quiz.getSubject(),
                teacher
        );

        Subject subject = subjectRepository.findById(
                request.getSubjectId()
        ).orElseThrow(() ->
                new RuntimeException(
                        "Subject not found with ID: "
                                + request.getSubjectId()
                )
        );

        // Also verify that the new subject belongs
        // to the same teacher.
        verifySubjectOwnership(subject, teacher);

        // Prevent duplicate quiz titles within
        // the selected subject.
        if (!quiz.getTitle().equals(request.getTitle())
                && quizRepository.existsByTitleAndSubjectId(
                        request.getTitle(),
                        request.getSubjectId())) {

            throw new RuntimeException(
                    "Quiz already exists for this subject"
            );
        }

        quiz.setTitle(request.getTitle().trim());

        quiz.setDescription(
                request.getDescription() == null
                        ? ""
                        : request.getDescription().trim()
        );

        quiz.setDurationMinutes(
                request.getDurationMinutes()
        );

        quiz.setMaxAttempts(
                request.getMaxAttempts()
        );

        quiz.setSubject(subject);

        Quiz updatedQuiz =
                quizRepository.save(quiz);

        return convertToResponse(updatedQuiz);
    }

    // ==========================================
    // DELETE QUIZ
    // ==========================================

    public void deleteQuiz(
            Long id,
            String teacherEmail) {

        User teacher = getTeacherByEmail(teacherEmail);

        Quiz quiz = quizRepository.findById(id)
                .orElseThrow(() ->
                        new RuntimeException(
                                "Quiz not found with ID: " + id
                        )
                );

        verifySubjectOwnership(
                quiz.getSubject(),
                teacher
        );

        quizRepository.delete(quiz);
    }

    // ==========================================
    // FIND TEACHER
    // ==========================================

    private User getTeacherByEmail(
            String teacherEmail) {

        User teacher = userRepository.findByEmail(
                teacherEmail
        ).orElseThrow(() ->
                new RuntimeException(
                        "Teacher account not found"
                )
        );

        if (!"TEACHER".equalsIgnoreCase(
                teacher.getRole())) {

            throw new RuntimeException(
                    "Only teachers can manage quizzes"
            );
        }

        return teacher;
    }

    // ==========================================
    // VERIFY COURSE OWNERSHIP
    // ==========================================

    private void verifySubjectOwnership(
            Subject subject,
            User teacher) {

        if (subject.getTeacher() == null) {
            throw new RuntimeException(
                    "This course is not assigned to a teacher"
            );
        }

        if (!subject.getTeacher().getId()
                .equals(teacher.getId())) {

            throw new RuntimeException(
                    "You do not have permission to manage quizzes for this course"
            );
        }
    }

    // ==========================================
    // CONVERT ENTITY TO RESPONSE
    // ==========================================

    private QuizResponse convertToResponse(
            Quiz quiz) {

        return new QuizResponse(
                quiz.getId(),
                quiz.getTitle(),
                quiz.getDescription(),
                quiz.getDurationMinutes(),
                quiz.getMaxAttempts(),
                quiz.getSubject().getId(),
                quiz.getSubject().getName()
        );
    }
}