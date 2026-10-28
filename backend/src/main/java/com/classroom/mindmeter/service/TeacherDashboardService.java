package com.classroom.mindmeter.service;

import com.classroom.mindmeter.dto.QuizAttendanceResponse;
import com.classroom.mindmeter.dto.TeacherDashboardResponse;
import com.classroom.mindmeter.model.Attempt;
import com.classroom.mindmeter.model.Quiz;
import com.classroom.mindmeter.repository.AttemptRepository;
import com.classroom.mindmeter.repository.QuestionRepository;
import com.classroom.mindmeter.repository.QuizRepository;
import com.classroom.mindmeter.repository.SubjectRepository;
import com.classroom.mindmeter.repository.UserRepository;

import org.springframework.stereotype.Service;

import java.util.HashSet;
import java.util.List;
import java.util.Set;

@Service
public class TeacherDashboardService {

    private final UserRepository userRepository;
    private final SubjectRepository subjectRepository;
    private final QuizRepository quizRepository;
    private final QuestionRepository questionRepository;
    private final AttemptRepository attemptRepository;

    public TeacherDashboardService(
            UserRepository userRepository,
            SubjectRepository subjectRepository,
            QuizRepository quizRepository,
            QuestionRepository questionRepository,
            AttemptRepository attemptRepository) {

        this.userRepository = userRepository;
        this.subjectRepository = subjectRepository;
        this.quizRepository = quizRepository;
        this.questionRepository = questionRepository;
        this.attemptRepository = attemptRepository;
    }

    // ==========================================
    // TEACHER DASHBOARD SUMMARY
    // ==========================================

    public TeacherDashboardResponse getDashboard() {

        long totalStudents = userRepository.findAll()
                .stream()
                .filter(user ->
                        "STUDENT".equalsIgnoreCase(user.getRole()))
                .count();

        long totalSubjects = subjectRepository.count();

        long totalQuizzes = quizRepository.count();

        long totalQuestions = questionRepository.count();

        long totalAttempts = attemptRepository.count();

        return new TeacherDashboardResponse(
                totalStudents,
                totalSubjects,
                totalQuizzes,
                totalQuestions,
                totalAttempts
        );
    }

    // ==========================================
    // QUIZ-WISE ATTENDANCE
    // ==========================================

    public List<QuizAttendanceResponse> getQuizAttendance() {

        List<Quiz> quizzes = quizRepository.findAll();

        return quizzes.stream()
                .map(this::createAttendanceResponse)
                .toList();
    }

    private QuizAttendanceResponse createAttendanceResponse(
            Quiz quiz) {

        List<Attempt> attempts =
                attemptRepository.findByQuizId(quiz.getId());

        // A student may make multiple attempts.
        // We count each student only once for attendance.
        Set<Long> uniqueStudentIds = new HashSet<>();

        for (Attempt attempt : attempts) {

            if (attempt.getUser() != null
                    && attempt.getUser().getId() != null) {

                uniqueStudentIds.add(
                        attempt.getUser().getId()
                );
            }
        }

        return new QuizAttendanceResponse(
                quiz.getId(),
                quiz.getTitle(),
                quiz.getSubject().getName(),
                uniqueStudentIds.size(),
                attempts.size()
        );
    }
}