package com.classroom.mindmeter.service;

import com.classroom.mindmeter.dto.AttemptResponse;
import com.classroom.mindmeter.dto.AttemptProgressResponse;
import com.classroom.mindmeter.dto.QuizSubmission;
import com.classroom.mindmeter.dto.QuizSubmissionResponse;
import com.classroom.mindmeter.model.Attempt;
import com.classroom.mindmeter.model.AttemptAnswer;
import com.classroom.mindmeter.model.Question;
import com.classroom.mindmeter.model.Quiz;
import com.classroom.mindmeter.model.User;
import com.classroom.mindmeter.repository.AttemptAnswerRepository;
import com.classroom.mindmeter.repository.AttemptRepository;
import com.classroom.mindmeter.repository.QuestionRepository;
import com.classroom.mindmeter.repository.QuizRepository;
import com.classroom.mindmeter.repository.SubjectRepository;
import com.classroom.mindmeter.repository.UserRepository;

import org.springframework.security.access.AccessDeniedException;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Duration;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

@Service
public class AttemptService {

    private static final ZoneId APPLICATION_ZONE =
            ZoneId.of("Asia/Kolkata");

    private final AttemptRepository attemptRepository;
    private final AttemptAnswerRepository attemptAnswerRepository;
    private final UserRepository userRepository;
    private final QuizRepository quizRepository;
    private final QuestionRepository questionRepository;
    private final SubjectRepository subjectRepository;

    @Value("${mindmeter.quiz.max-pause-seconds:600}")
    private int maxPauseSeconds;

    public AttemptService(
            AttemptRepository attemptRepository,
            AttemptAnswerRepository attemptAnswerRepository,
            UserRepository userRepository,
            QuizRepository quizRepository,
            QuestionRepository questionRepository,
            SubjectRepository subjectRepository) {

        this.attemptRepository = attemptRepository;
        this.attemptAnswerRepository = attemptAnswerRepository;
        this.userRepository = userRepository;
        this.quizRepository = quizRepository;
        this.questionRepository = questionRepository;
        this.subjectRepository = subjectRepository;
    }

    // ==========================================
    // STUDENT - SUBMIT QUIZ
    // ==========================================

    @Transactional
    public AttemptProgressResponse startOrResumeAttempt(
            Long quizId,
            String studentEmail,
            boolean startNewAttempt) {

        User student = getStudent(studentEmail);
        Quiz quiz = attemptRepository.lockQuiz(quizId)
                .orElseThrow(() ->
                        new RuntimeException("Quiz not found with ID: " + quizId)
                );
        LocalDateTime now = now();

        Attempt active = attemptRepository
                .findFirstByUserIdAndQuizIdAndSubmittedFalseOrderByAttemptNumberDesc(
                        student.getId(),
                        quizId
                )
                .orElse(null);

        if (active != null) {
            submitIfExpired(active, now);
            return toProgress(active, now);
        }

        List<Attempt> previousAttempts =
                attemptRepository.findByUserIdAndQuizId(student.getId(), quizId);
        List<Attempt> submittedAttempts = previousAttempts.stream()
                .filter(attempt -> Boolean.TRUE.equals(attempt.getSubmitted()))
                .toList();

        if (submittedAttempts.size() >= quiz.getMaxAttempts()
                || (!startNewAttempt && !submittedAttempts.isEmpty())) {
            return toProgress(getLatestAttempt(submittedAttempts), now);
        }

        List<Question> questions = questionRepository.findByQuizId(quizId);
        if (questions.isEmpty()) {
            throw new RuntimeException("This quiz has no questions");
        }

        int totalMarks = questions.stream()
                .mapToInt(this::safeMarks)
                .sum();
        int durationSeconds = Math.max(0, quiz.getDurationMinutes()) * 60;
        LocalDateTime deadline = now.plusSeconds(durationSeconds);

        Attempt attempt = new Attempt();
        attempt.setUser(student);
        attempt.setQuiz(quiz);
        attempt.setAttemptNumber(submittedAttempts.size() + 1);
        attempt.setScore(0);
        attempt.setTotalMarks(totalMarks);
        attempt.setStartTime(now);
        attempt.setDeadlineTime(deadline);
        attempt.setSubmitted(false);
        attempt.setStatus("IN_PROGRESS");
        attempt.setPauseSecondsUsed(0);
        attempt.setDurationSeconds(0L);

        return toProgress(attemptRepository.save(attempt), now);
    }

    private Attempt getLatestAttempt(List<Attempt> attempts) {
        return attempts.stream()
                .max((left, right) -> Integer.compare(
                        left.getAttemptNumber(),
                        right.getAttemptNumber()
                ))
                .orElseThrow(() -> new RuntimeException(
                        "Maximum attempts reached for this quiz"
                ));
    }

    @Transactional
    public AttemptProgressResponse getAttemptProgress(
            Long attemptId,
            String studentEmail) {

        User student = getStudent(studentEmail);
        Attempt attempt = getOwnedAttemptForUpdate(attemptId, student.getId());
        LocalDateTime now = now();
        submitIfExpired(attempt, now);
        return toProgress(attempt, now);
    }

    @Transactional
    public AttemptProgressResponse saveAnswer(
            Long attemptId,
            Long questionId,
            String answer,
            String studentEmail) {

        User student = getStudent(studentEmail);
        Attempt attempt = getOwnedAttemptForUpdate(attemptId, student.getId());
        LocalDateTime now = now();
        submitIfExpired(attempt, now);
        if (Boolean.TRUE.equals(attempt.getSubmitted())) {
            return toProgress(attempt, now);
        }
        if ("PAUSED".equals(attempt.getStatus())) {
            throw new IllegalStateException("Resume the quiz before changing answers");
        }

        Question question = questionRepository.findById(questionId)
                .orElseThrow(() -> new RuntimeException("Question not found"));
        if (!question.getQuiz().getId().equals(attempt.getQuiz().getId())) {
            throw new AccessDeniedException(
                    "This question does not belong to the active quiz"
            );
        }

        String normalizedAnswer = validateAnswer(answer);
        AttemptAnswer savedAnswer = attemptAnswerRepository
                .findByAttemptIdAndQuestionId(attemptId, questionId)
                .orElseGet(() ->
                    new AttemptAnswer(attempt, questionId, normalizedAnswer)
                );
        savedAnswer.setAnswer(normalizedAnswer);
        attemptAnswerRepository.save(savedAnswer);
        return toProgress(attempt, now);
    }

    @Transactional
    public AttemptProgressResponse pauseAttempt(
            Long attemptId,
            String studentEmail) {

        User student = getStudent(studentEmail);
        Attempt attempt = getOwnedAttemptForUpdate(attemptId, student.getId());
        LocalDateTime now = now();
        submitIfExpired(attempt, now);
        if (Boolean.TRUE.equals(attempt.getSubmitted())) {
            return toProgress(attempt, now);
        }
        if ("PAUSED".equals(attempt.getStatus())) {
            return toProgress(attempt, now);
        }

        attempt.setPausedAt(now);
        attempt.setStatus("PAUSED");
        attemptRepository.save(attempt);
        return toProgress(attempt, now);
    }

    @Transactional
    public AttemptProgressResponse resumeAttempt(
            Long attemptId,
            String studentEmail) {

        User student = getStudent(studentEmail);
        Attempt attempt = getOwnedAttemptForUpdate(attemptId, student.getId());
        LocalDateTime now = now();
        submitIfExpired(attempt, now);
        if (Boolean.TRUE.equals(attempt.getSubmitted())
                || !"PAUSED".equals(attempt.getStatus())) {
            return toProgress(attempt, now);
        }

        long pausedSeconds = Math.max(0,
                Duration.between(attempt.getPausedAt(), now).getSeconds());
        int remainingPause = Math.max(
                0,
                maxPauseSeconds - attempt.getPauseSecondsUsed()
        );
        int creditedPause = (int) Math.min(pausedSeconds, remainingPause);
        LocalDateTime adjustedDeadline =
                attempt.getDeadlineTime().plusSeconds(creditedPause);

        attempt.setDeadlineTime(adjustedDeadline);
        attempt.setPauseSecondsUsed(
                attempt.getPauseSecondsUsed() + creditedPause
        );
        attempt.setPausedAt(null);
        attempt.setStatus("IN_PROGRESS");

        if (remainingSeconds(attempt, now) == 0) {
            submitAttempt(attempt, expirationTime(attempt, now), now);
        } else {
            attemptRepository.save(attempt);
        }
        return toProgress(attempt, now);
    }

    @Transactional
    public AttemptProgressResponse submitQuiz(
            QuizSubmission submission,
            String studentEmail) {

        if (submission.getAttemptId() == null) {
            throw new IllegalArgumentException("Attempt ID is required");
        }
        User student = getStudent(studentEmail);
        Attempt attempt = getOwnedAttemptForUpdate(
                submission.getAttemptId(),
                student.getId()
        );
        LocalDateTime now = now();
        if (Boolean.TRUE.equals(attempt.getSubmitted())) {
            return toProgress(attempt, now);
        }
        if (!attempt.getQuiz().getId().equals(submission.getQuizId())) {
            throw new AccessDeniedException(
                    "This attempt does not belong to the supplied quiz"
            );
        }
        boolean expired = remainingSeconds(attempt, now) == 0;
        if ("PAUSED".equals(attempt.getStatus()) && !expired) {
            throw new IllegalStateException("Resume the quiz before submitting");
        }

        saveAnswers(attempt, submission.getAnswers());
        if (expired) {
            submitAttempt(attempt, expirationTime(attempt, now), now);
        } else {
            submitAttempt(attempt, now);
        }
        return toProgress(attempt, now);
    }

    private User getStudent(String studentEmail) {
        User student = userRepository.findByEmail(studentEmail)
                .orElseThrow(() -> new RuntimeException("Student not found"));
        if (!"STUDENT".equalsIgnoreCase(student.getRole())) {
            throw new AccessDeniedException("Only students can attempt quizzes");
        }
        return student;
    }

    private Attempt getOwnedAttemptForUpdate(Long attemptId, Long studentId) {
        return attemptRepository.findOwnedByIdForUpdate(attemptId, studentId)
                .orElseThrow(() ->
                        new AccessDeniedException(
                                "Attempt not found or does not belong to this student"
                        )
                );
    }

    private void saveAnswers(Attempt attempt, Map<Long, String> answers) {
        if (answers == null) {
            throw new IllegalArgumentException("Answers are required");
        }
        for (Map.Entry<Long, String> entry : answers.entrySet()) {
            Question question = questionRepository.findById(entry.getKey())
                    .orElseThrow(() ->
                            new IllegalArgumentException(
                                    "Question does not belong to this quiz"
                            )
                    );
            if (!question.getQuiz().getId().equals(attempt.getQuiz().getId())) {
                throw new AccessDeniedException(
                        "Question does not belong to this attempt's quiz"
                );
            }

            String answer = validateAnswer(entry.getValue());
            AttemptAnswer savedAnswer = attemptAnswerRepository
                    .findByAttemptIdAndQuestionId(attempt.getId(), question.getId())
                    .orElseGet(() ->
                            new AttemptAnswer(attempt, question.getId(), answer)
                    );
            savedAnswer.setAnswer(answer);
            attemptAnswerRepository.save(savedAnswer);
        }
    }

    private String validateAnswer(String answer) {
        if (answer == null || !Set.of("A", "B", "C", "D")
                .contains(answer.trim().toUpperCase())) {
            throw new IllegalArgumentException(
                    "Answer must be one of A, B, C, or D"
            );
        }
        return answer.trim().toUpperCase();
    }

    private void submitIfExpired(Attempt attempt, LocalDateTime now) {
        if (!Boolean.TRUE.equals(attempt.getSubmitted())
                && remainingSeconds(attempt, now) == 0) {
            submitAttempt(attempt, expirationTime(attempt, now), now);
        }
    }

    private void submitAttempt(Attempt attempt, LocalDateTime now) {
        submitAttempt(attempt, now, now);
    }

    private void submitAttempt(
            Attempt attempt,
            LocalDateTime endTime,
            LocalDateTime now) {

        if ("PAUSED".equals(attempt.getStatus())
                && attempt.getPausedAt() != null) {
            int remainingPause = Math.max(
                    0,
                    maxPauseSeconds - attempt.getPauseSecondsUsed()
            );
            long pausedSeconds = Math.max(
                    0,
                    Duration.between(attempt.getPausedAt(), now).getSeconds()
            );
            int creditedPause = (int) Math.min(pausedSeconds, remainingPause);
            attempt.setPauseSecondsUsed(
                    attempt.getPauseSecondsUsed() + creditedPause
            );
        }

        List<Question> questions =
                questionRepository.findByQuizId(attempt.getQuiz().getId());
        Map<Long, String> answers = getSavedAnswers(attempt.getId());
        int score = 0;
        int totalMarks = 0;
        for (Question question : questions) {
            int questionMarks = safeMarks(question);
            totalMarks += questionMarks;
            String answer = answers.get(question.getId());
            if (answer != null && question.getCorrectAnswer() != null
                    && answer.equalsIgnoreCase(
                            question.getCorrectAnswer().trim()
                    )) {
                score += questionMarks;
            }
        }

        attempt.setScore(score);
        attempt.setTotalMarks(totalMarks);
        attempt.setSubmitted(true);
        attempt.setStatus("SUBMITTED");
        attempt.setPausedAt(null);
        attempt.setEndTime(endTime);
        attempt.setDurationSeconds(calculateDurationSeconds(attempt, endTime));
        attemptRepository.save(attempt);
    }

    private LocalDateTime expirationTime(Attempt attempt, LocalDateTime now) {
        LocalDateTime deadline = attempt.getDeadlineTime();
        if ("PAUSED".equals(attempt.getStatus())
                && attempt.getPausedAt() != null) {
            long pausedSeconds = Math.max(
                    0,
                    Duration.between(attempt.getPausedAt(), now).getSeconds()
            );
            int remainingPause = Math.max(
                    0,
                    maxPauseSeconds - attempt.getPauseSecondsUsed()
            );
            deadline = deadline.plusSeconds(
                    Math.min(pausedSeconds, remainingPause)
            );
        }
        return deadline;
    }

    private long calculateDurationSeconds(
            Attempt attempt,
            LocalDateTime endTime) {

        if (attempt.getStartTime() == null || endTime == null
                || endTime.isBefore(attempt.getStartTime())) {
            return 0;
        }
        long elapsedSeconds = Duration.between(
                attempt.getStartTime(),
                endTime
        ).getSeconds();
        return Math.max(0, elapsedSeconds - attempt.getPauseSecondsUsed());
    }

    private int safeMarks(Question question) {
        return question == null || question.getMarks() == null
                ? 0
                : question.getMarks();
    }

    private AttemptProgressResponse toProgress(
            Attempt attempt,
            LocalDateTime now) {

        boolean submitted = Boolean.TRUE.equals(attempt.getSubmitted());
        String status = submitted ? "SUBMITTED" : attempt.getStatus();
        if (status == null) {
            status = "IN_PROGRESS";
        }

        return new AttemptProgressResponse(
                attempt.getId(),
                attempt.getQuiz().getId(),
                attempt.getAttemptNumber(),
                status,
                getSavedAnswers(attempt.getId()),
                remainingSeconds(attempt, now),
                remainingPauseSeconds(attempt, now),
                submitted ? toStudentResult(attempt) : null
        );
    }

    private Map<Long, String> getSavedAnswers(Long attemptId) {
        Map<Long, String> answers = new HashMap<>();
        for (AttemptAnswer answer : attemptAnswerRepository
                .findByAttemptId(attemptId)) {
            answers.put(
                    answer.getQuestionId(),
                    answer.getAnswer()
            );
        }
        return answers;
    }

    private AttemptResponse toStudentResult(Attempt attempt) {
        AttemptResponse response = convertToResponse(attempt);
        if (attempt.getDeadlineTime() == null) {
            response.setCorrectAnswers(null);
            response.setIncorrectAnswers(null);
            return response;
        }
        List<Question> questions =
                questionRepository.findByQuizId(attempt.getQuiz().getId());
        Map<Long, String> answers = getSavedAnswers(attempt.getId());
        int correctAnswers = 0;
        for (Question question : questions) {
            String answer = answers.get(question.getId());
            if (answer != null && question.getCorrectAnswer() != null
                    && answer.equalsIgnoreCase(
                            question.getCorrectAnswer().trim()
                    )) {
                correctAnswers++;
            }
        }
        response.setCorrectAnswers(correctAnswers);
        response.setIncorrectAnswers(questions.size() - correctAnswers);
        return response;
    }

    private long remainingSeconds(Attempt attempt, LocalDateTime now) {
        if (Boolean.TRUE.equals(attempt.getSubmitted())) {
            return 0;
        }
        LocalDateTime deadline = attempt.getDeadlineTime();
        if (deadline == null) {
            return 0;
        }
        if ("PAUSED".equals(attempt.getStatus())
                && attempt.getPausedAt() != null) {
            long elapsedPause = Math.max(
                    0,
                    Duration.between(attempt.getPausedAt(), now).getSeconds()
            );
            int pauseBudget = Math.max(
                    0,
                    maxPauseSeconds - attempt.getPauseSecondsUsed()
            );
            deadline = deadline.plusSeconds(
                    Math.min(elapsedPause, pauseBudget)
            );
        }
        long milliseconds = Duration.between(now, deadline).toMillis();
        return Math.max(0, (milliseconds + 999) / 1000);
    }

    private long remainingPauseSeconds(Attempt attempt, LocalDateTime now) {
        int available = Math.max(
                0,
                maxPauseSeconds - attempt.getPauseSecondsUsed()
        );
        if (!"PAUSED".equals(attempt.getStatus())
                || attempt.getPausedAt() == null) {
            return available;
        }
        long elapsed = Math.max(
                0,
                Duration.between(attempt.getPausedAt(), now).getSeconds()
        );
        return Math.max(0, available - elapsed);
    }

    private LocalDateTime now() {
        return LocalDateTime.now(APPLICATION_ZONE);
    }

    // ==========================================
    // STUDENT - VIEW OWN ATTEMPTS
    // ==========================================

    public List<AttemptResponse> getMyAttempts(
            String studentEmail) {

        User student =
                userRepository.findByEmail(studentEmail)
                        .orElseThrow(() ->
                                new RuntimeException(
                                        "Student not found"
                                )
                        );

        return attemptRepository
                .findByUserId(student.getId())
                .stream()
                .map(this::convertToResponse)
                .toList();
    }

    // ==========================================
    // TEACHER - VIEW ALL ATTEMPTS
    // ==========================================

    public List<AttemptResponse> getAllAttempts(
            String teacherEmail) {

        return attemptRepository.findAllByTeacherEmail(teacherEmail)
                .stream()
                .map(this::convertToResponse)
                .toList();
    }

    public List<AttemptResponse> getSubmittedAttemptsBySubject(
            Long subjectId,
            String teacherEmail) {

        User teacher = userRepository.findByEmail(teacherEmail)
                .orElseThrow(() ->
                        new RuntimeException("Teacher not found")
                );

        boolean ownsSubject = subjectRepository.findById(subjectId)
                .map(subject -> subject.getTeacher() != null
                        && subject.getTeacher().getId().equals(teacher.getId()))
                .orElse(false);

        if (!ownsSubject) {
            throw new AccessDeniedException(
                    "You are not allowed to view submissions for this subject"
            );
        }

        return attemptRepository
                .findSubmittedBySubjectAndTeacher(subjectId, teacherEmail)
                .stream()
                .map(this::convertToResponse)
                .toList();
    }

    // ==========================================
    // TEACHER - VIEW QUIZ ATTEMPTS
    // ==========================================

    public List<AttemptResponse> getAttemptsByQuiz(
            Long quizId,
            String teacherEmail) {

        Quiz quiz = quizRepository.findWithSubjectAndTeacherById(quizId)
                .orElseThrow(() ->
                        new RuntimeException(
                                "Quiz not found with ID: " + quizId
                        )
                );

        User teacher = userRepository.findByEmail(teacherEmail)
                .orElseThrow(() ->
                        new RuntimeException("Teacher not found")
                );

        if (quiz.getSubject() == null
                || quiz.getSubject().getTeacher() == null
                || !quiz.getSubject().getTeacher().getId()
                        .equals(teacher.getId())) {
            throw new AccessDeniedException(
                    "You are not allowed to view attempts for this quiz"
            );
        }

        return attemptRepository
                .findByQuizId(quizId)
                .stream()
                .map(this::convertToResponse)
                .toList();
    }

    public List<QuizSubmissionResponse> getSubmittedAttemptsByQuiz(
            Long quizId,
            String teacherEmail) {

        Quiz quiz = quizRepository.findWithSubjectAndTeacherById(quizId)
                .orElseThrow(() ->
                        new RuntimeException(
                                "Quiz not found with ID: " + quizId
                        )
                );

        User teacher = userRepository.findByEmail(teacherEmail)
                .orElseThrow(() ->
                        new RuntimeException("Teacher not found")
                );

        if (quiz.getSubject() == null
                || quiz.getSubject().getTeacher() == null
                || !quiz.getSubject().getTeacher().getId()
                        .equals(teacher.getId())) {
            throw new AccessDeniedException(
                    "You are not allowed to view submissions for this quiz"
            );
        }

        return attemptRepository
                .findSubmittedByQuizAndTeacher(quizId, teacherEmail)
                .stream()
                .map(attempt -> {
                    LocalDateTime endTime = attempt.getEndTime();
                    return new QuizSubmissionResponse(
                            attempt.getUser().getName(),
                            attempt.getUser().getRegisterNumber(),
                            attempt.getQuiz().getTitle(),
                            attempt.getQuiz().getSubject().getName(),
                            endTime == null
                                    ? null
                                    : endTime.atZone(APPLICATION_ZONE)
                                            .toOffsetDateTime(),
                            attempt.getScore(),
                            attempt.getTotalMarks(),
                            "Submitted"
                    );
                })
                .toList();
    }

    // ==========================================
    // CONVERT ATTEMPT TO RESPONSE
    // ==========================================

    private AttemptResponse convertToResponse(
            Attempt attempt) {

        User student = attempt.getUser();
        Quiz quiz = attempt.getQuiz();

        AttemptResponse response = new AttemptResponse(
                attempt.getId(),
                quiz.getId(),
                quiz.getTitle(),

                student.getId(),
                student.getName(),
                student.getEmail(),

                // IMPORTANT:
                // Get the actual college register number
                // from the User entity.
                student.getRegisterNumber(),

                attempt.getAttemptNumber(),
                attempt.getScore(),
                attempt.getTotalMarks(),
                attempt.getStartTime(),
                attempt.getEndTime(),
                attempt.getSubmitted()
        );

        response.setSubjectName(
                quiz.getSubject() == null
                        ? null
                        : quiz.getSubject().getName()
        );

        LocalDateTime endTime = attempt.getEndTime();
        response.setSubmittedAt(
                endTime == null
                        ? null
                        : endTime.atZone(APPLICATION_ZONE)
                                .toOffsetDateTime()
        );

        return response;
    }
}