package com.classroom.mindmeter.service;

import com.classroom.mindmeter.model.Attempt;
import com.classroom.mindmeter.model.AttemptAnswer;
import com.classroom.mindmeter.model.Question;
import com.classroom.mindmeter.model.Quiz;
import com.classroom.mindmeter.model.Subject;
import com.classroom.mindmeter.model.User;
import com.classroom.mindmeter.dto.QuizSubmission;
import com.classroom.mindmeter.repository.AttemptRepository;
import com.classroom.mindmeter.repository.AttemptAnswerRepository;
import com.classroom.mindmeter.repository.QuestionRepository;
import com.classroom.mindmeter.repository.QuizRepository;
import com.classroom.mindmeter.repository.SubjectRepository;
import com.classroom.mindmeter.repository.UserRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.test.util.ReflectionTestUtils;

import java.time.LocalDateTime;
import java.time.ZoneId;
import java.util.List;
import java.util.Map;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class AttemptServiceTest {

    private static final Long TEACHER_ID = 11L;
    private static final Long QUIZ_ID = 22L;
    private static final String TEACHER_EMAIL = "teacher@example.com";

    @Mock
    private AttemptRepository attemptRepository;

    @Mock
    private AttemptAnswerRepository attemptAnswerRepository;

    @Mock
    private UserRepository userRepository;

    @Mock
    private QuizRepository quizRepository;

    @Mock
    private QuestionRepository questionRepository;

    @Mock
    private SubjectRepository subjectRepository;

    @InjectMocks
    private AttemptService attemptService;

    @Test
    void returnsSubmittedAttemptsForExactQuizAndTeacher() {
        User teacher = teacher(TEACHER_ID);
        User student = new User();
        student.setName("Avery Student");
        student.setRegisterNumber("REG-001");

        Subject subject = new Subject();
        subject.setName("Computer Science");
        subject.setTeacher(teacher);

        Quiz quiz = new Quiz();
        quiz.setId(QUIZ_ID);
        quiz.setTitle("Data Structures Basics");
        quiz.setSubject(subject);

        LocalDateTime submittedAt = LocalDateTime.of(2026, 10, 13, 9, 30);
        Attempt attempt = new Attempt(
                student,
                quiz,
                1,
                8,
                10,
                submittedAt,
                submittedAt,
                true
        );

        when(quizRepository.findWithSubjectAndTeacherById(QUIZ_ID))
                .thenReturn(Optional.of(quiz));
        when(userRepository.findByEmail(TEACHER_EMAIL))
                .thenReturn(Optional.of(teacher));
        when(attemptRepository.findSubmittedByQuizAndTeacher(
                QUIZ_ID,
                TEACHER_EMAIL
        )).thenReturn(List.of(attempt));

        var submissions = attemptService.getSubmittedAttemptsByQuiz(
                QUIZ_ID,
                TEACHER_EMAIL
        );

        assertEquals(1, submissions.size());
        assertEquals("Avery Student", submissions.get(0).getStudentName());
        assertEquals("REG-001", submissions.get(0).getRegisterNumber());
        assertEquals("Data Structures Basics", submissions.get(0).getQuizTitle());
        assertEquals("Computer Science", submissions.get(0).getSubjectName());
        assertEquals(8, submissions.get(0).getMarksObtained());
        assertEquals(10, submissions.get(0).getTotalMarks());
        assertEquals("Submitted", submissions.get(0).getStatus());
        verify(attemptRepository).findSubmittedByQuizAndTeacher(
                QUIZ_ID,
                TEACHER_EMAIL
        );
    }

    @Test
    void deniesSubmissionAccessWhenQuizBelongsToAnotherTeacher() {
        User teacher = teacher(TEACHER_ID);
        Subject subject = new Subject();
        subject.setTeacher(teacher(99L));
        Quiz quiz = new Quiz();
        quiz.setId(QUIZ_ID);
        quiz.setSubject(subject);

        when(quizRepository.findWithSubjectAndTeacherById(QUIZ_ID))
                .thenReturn(Optional.of(quiz));
        when(userRepository.findByEmail(TEACHER_EMAIL))
                .thenReturn(Optional.of(teacher));

        assertThrows(
                AccessDeniedException.class,
                () -> attemptService.getSubmittedAttemptsByQuiz(
                        QUIZ_ID,
                        TEACHER_EMAIL
                )
        );
        verify(attemptRepository, never())
                .findSubmittedByQuizAndTeacher(QUIZ_ID, TEACHER_EMAIL);
    }

    @Test
    void startsOnePersistentAttemptAndReturnsItsTimer() {
        User student = student(7L);
        Quiz quiz = quiz();
        Question question = question(quiz);

        when(userRepository.findByEmail(student.getEmail()))
                .thenReturn(Optional.of(student));
        when(attemptRepository.lockQuiz(QUIZ_ID))
                .thenReturn(Optional.of(quiz));
        when(attemptRepository
                .findFirstByUserIdAndQuizIdAndSubmittedFalseOrderByAttemptNumberDesc(
                        student.getId(),
                        QUIZ_ID
                )).thenReturn(Optional.empty());
        when(attemptRepository.findByUserIdAndQuizId(student.getId(), QUIZ_ID))
                .thenReturn(List.of());
        when(questionRepository.findByQuizId(QUIZ_ID))
                .thenReturn(List.of(question));
        when(attemptRepository.save(any(Attempt.class)))
                .thenAnswer(invocation -> {
                    Attempt saved = invocation.getArgument(0);
                    saved.setId(71L);
                    return saved;
                });

        var progress = attemptService.startOrResumeAttempt(
                QUIZ_ID,
                student.getEmail(),
                false
        );

        assertEquals(71L, progress.getAttemptId());
        assertEquals("IN_PROGRESS", progress.getStatus());
        assertEquals(1, progress.getAttemptNumber());
        assertEquals(5, progress.getRemainingSeconds() / 60);
        ArgumentCaptor<Attempt> attemptCaptor =
                ArgumentCaptor.forClass(Attempt.class);
        verify(attemptRepository).save(attemptCaptor.capture());
        assertNotNull(attemptCaptor.getValue().getStartTime());
        assertNull(attemptCaptor.getValue().getEndTime());
        assertEquals(0L, attemptCaptor.getValue().getDurationSeconds());
    }

    @Test
    void startsAttemptWhenQuestionMarksAreMissing() {
        User student = student(7L);
        Quiz quiz = quiz();
        Question question = question(quiz);
        question.setMarks(null);

        when(userRepository.findByEmail(student.getEmail()))
                .thenReturn(Optional.of(student));
        when(attemptRepository.lockQuiz(QUIZ_ID))
                .thenReturn(Optional.of(quiz));
        when(attemptRepository
                .findFirstByUserIdAndQuizIdAndSubmittedFalseOrderByAttemptNumberDesc(
                        student.getId(),
                        QUIZ_ID
                )).thenReturn(Optional.empty());
        when(attemptRepository.findByUserIdAndQuizId(student.getId(), QUIZ_ID))
                .thenReturn(List.of());
        when(questionRepository.findByQuizId(QUIZ_ID))
                .thenReturn(List.of(question));
        when(attemptRepository.save(any(Attempt.class)))
                .thenAnswer(invocation -> {
                    Attempt saved = invocation.getArgument(0);
                    saved.setId(71L);
                    return saved;
                });

        var progress = attemptService.startOrResumeAttempt(
                QUIZ_ID,
                student.getEmail(),
                false
        );

        assertEquals("IN_PROGRESS", progress.getStatus());
        assertEquals(71L, progress.getAttemptId());
    }

    @Test
    void refreshShowsLatestResultButDashboardIntentStartsNextAllowedAttempt() {
        User student = student(7L);
        Quiz quiz = quiz();
        Attempt previous = new Attempt(
                student,
                quiz,
                1,
                4,
                5,
                LocalDateTime.now().minusMinutes(3),
                LocalDateTime.now(),
                true
        );
        previous.setId(70L);
        Question question = question(quiz);

        when(userRepository.findByEmail(student.getEmail()))
                .thenReturn(Optional.of(student));
        when(attemptRepository.lockQuiz(QUIZ_ID))
                .thenReturn(Optional.of(quiz));
        when(attemptRepository
                .findFirstByUserIdAndQuizIdAndSubmittedFalseOrderByAttemptNumberDesc(
                        student.getId(),
                        QUIZ_ID
                )).thenReturn(Optional.empty());
        when(attemptRepository.findByUserIdAndQuizId(student.getId(), QUIZ_ID))
                .thenReturn(List.of(previous));
        when(attemptAnswerRepository.findByAttemptId(70L))
                .thenReturn(List.of());
        when(questionRepository.findByQuizId(QUIZ_ID))
                .thenReturn(List.of(question));
        when(attemptRepository.save(any(Attempt.class)))
                .thenAnswer(invocation -> {
                    Attempt saved = invocation.getArgument(0);
                    saved.setId(71L);
                    return saved;
                });

        var refreshed = attemptService.startOrResumeAttempt(
                QUIZ_ID,
                student.getEmail(),
                false
        );
        var nextAttempt = attemptService.startOrResumeAttempt(
                QUIZ_ID,
                student.getEmail(),
                true
        );

        assertEquals("SUBMITTED", refreshed.getStatus());
        assertEquals(70L, refreshed.getAttemptId());
        assertEquals("IN_PROGRESS", nextAttempt.getStatus());
        assertEquals(2, nextAttempt.getAttemptNumber());
    }

    @Test
    void pausingAndResumingPreservesTimeAndReusesAttempt() {
        ReflectionTestUtils.setField(attemptService, "maxPauseSeconds", 600);
        User student = student(7L);
        Attempt attempt = activeAttempt(student, quiz());
        attempt.setDeadlineTime(
                LocalDateTime.now(ZoneId.of("Asia/Kolkata")).plusMinutes(4)
        );

        when(userRepository.findByEmail(student.getEmail()))
                .thenReturn(Optional.of(student));
        when(attemptRepository.findOwnedByIdForUpdate(71L, student.getId()))
                .thenReturn(Optional.of(attempt));
        when(attemptAnswerRepository.findByAttemptId(71L))
                .thenReturn(List.of());

        var paused = attemptService.pauseAttempt(
                71L,
                student.getEmail()
        );

        assertEquals("PAUSED", paused.getStatus());
        assertTrue(paused.getRemainingSeconds() >= 239);

        attempt.setPausedAt(
                LocalDateTime.now(ZoneId.of("Asia/Kolkata")).minusSeconds(30)
        );
        var resumed = attemptService.resumeAttempt(71L, student.getEmail());

        assertEquals("IN_PROGRESS", resumed.getStatus());
        assertNull(attempt.getEndTime());
        assertTrue(attempt.getPauseSecondsUsed() >= 29);
        assertTrue(attempt.getPauseSecondsUsed() <= 30);
        verify(attemptRepository, org.mockito.Mockito.times(2))
                .save(attempt);
    }

    @Test
    void submissionScoresOnlyStoredAnswersAndIsIdempotent() {
        User student = student(7L);
        Quiz quiz = quiz();
        Question question = question(quiz);
        Attempt attempt = activeAttempt(student, quiz);
        attempt.setStartTime(
                LocalDateTime.now(ZoneId.of("Asia/Kolkata")).minusSeconds(120)
        );
        attempt.setPauseSecondsUsed(30);
        AttemptAnswer storedAnswer =
                new AttemptAnswer(attempt, question.getId(), "A");
        QuizSubmission submission = new QuizSubmission(
                QUIZ_ID,
                Map.of(question.getId(), "A")
        );
        submission.setAttemptId(71L);

        when(userRepository.findByEmail(student.getEmail()))
                .thenReturn(Optional.of(student));
        when(attemptRepository.findOwnedByIdForUpdate(71L, student.getId()))
                .thenReturn(Optional.of(attempt));
        when(questionRepository.findById(question.getId()))
                .thenReturn(Optional.of(question));
        when(questionRepository.findByQuizId(QUIZ_ID))
                .thenReturn(List.of(question));
        when(attemptAnswerRepository.findByAttemptIdAndQuestionId(
                71L,
                question.getId()
        )).thenReturn(Optional.of(storedAnswer));
        when(attemptAnswerRepository.findByAttemptId(71L))
                .thenReturn(List.of(storedAnswer));

        var submitted = attemptService.submitQuiz(
                submission,
                student.getEmail()
        );

        assertEquals("SUBMITTED", submitted.getStatus());
        assertEquals(5, submitted.getResult().getScore());
        assertEquals(5, submitted.getResult().getTotalMarks());
        assertEquals(1, submitted.getResult().getCorrectAnswers());
        assertNotNull(attempt.getEndTime());
        assertTrue(attempt.getEndTime().isAfter(attempt.getStartTime()));
        assertTrue(attempt.getDurationSeconds() >= 90);
        assertTrue(attempt.getDurationSeconds() <= 95);
        verify(attemptRepository).save(attempt);

        when(attemptRepository.findOwnedByIdForUpdate(71L, student.getId()))
                .thenReturn(Optional.of(attempt));
        var duplicate = attemptService.submitQuiz(
                submission,
                student.getEmail()
        );
        assertEquals("SUBMITTED", duplicate.getStatus());
        verify(attemptRepository, org.mockito.Mockito.times(1)).save(attempt);
    }

    @Test
    void expiresAttemptAtItsEffectiveDeadline() {
        User student = student(7L);
        Quiz quiz = quiz();
        Question question = question(quiz);
        LocalDateTime deadline =
                LocalDateTime.now(ZoneId.of("Asia/Kolkata")).minusSeconds(1);
        Attempt attempt = activeAttempt(student, quiz);
        attempt.setStartTime(deadline.minusMinutes(5));
        attempt.setDeadlineTime(deadline);
        attempt.setEndTime(null);

        when(userRepository.findByEmail(student.getEmail()))
                .thenReturn(Optional.of(student));
        when(attemptRepository.findOwnedByIdForUpdate(71L, student.getId()))
                .thenReturn(Optional.of(attempt));
        when(questionRepository.findByQuizId(QUIZ_ID))
                .thenReturn(List.of(question));
        when(attemptAnswerRepository.findByAttemptId(71L))
                .thenReturn(List.of());

        var progress = attemptService.getAttemptProgress(
                71L,
                student.getEmail()
        );

        assertEquals("SUBMITTED", progress.getStatus());
        assertEquals(deadline, attempt.getEndTime());
        assertEquals(300L, attempt.getDurationSeconds());
    }

    @Test
    void rejectsAnAttemptNotOwnedByTheAuthenticatedStudent() {
        User student = student(7L);
        when(userRepository.findByEmail(student.getEmail()))
                .thenReturn(Optional.of(student));
        when(attemptRepository.findOwnedByIdForUpdate(71L, student.getId()))
                .thenReturn(Optional.empty());

        assertThrows(
                AccessDeniedException.class,
                () -> attemptService.getAttemptProgress(
                        71L,
                        student.getEmail()
                )
        );
        verify(attemptAnswerRepository, never()).findByAttemptId(71L);
    }

    private User student(Long id) {
        User student = new User();
        student.setId(id);
        student.setEmail("student@example.com");
        student.setRole("STUDENT");
        return student;
    }

    private Quiz quiz() {
        Quiz quiz = new Quiz();
        quiz.setId(QUIZ_ID);
        quiz.setTitle("Test Quiz");
        quiz.setDurationMinutes(5);
        quiz.setMaxAttempts(2);
        return quiz;
    }

    private Question question(Quiz quiz) {
        Question question = new Question();
        question.setId(33L);
        question.setQuiz(quiz);
        question.setCorrectAnswer("A");
        question.setMarks(5);
        question.setQuestionText("Question");
        return question;
    }

    private Attempt activeAttempt(User student, Quiz quiz) {
        LocalDateTime now = LocalDateTime.now(ZoneId.of("Asia/Kolkata"));
        Attempt attempt = new Attempt(
                student,
                quiz,
                1,
                0,
                5,
                now.minusMinutes(1),
                null,
                false
        );
        attempt.setId(71L);
        attempt.setStatus("IN_PROGRESS");
        attempt.setDeadlineTime(now.plusMinutes(5));
        attempt.setPauseSecondsUsed(0);
        return attempt;
    }

    private User teacher(Long id) {
        User teacher = new User();
        teacher.setId(id);
        teacher.setEmail(TEACHER_EMAIL);
        return teacher;
    }
}
