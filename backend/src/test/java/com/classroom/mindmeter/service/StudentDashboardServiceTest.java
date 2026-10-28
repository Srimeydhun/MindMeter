package com.classroom.mindmeter.service;

import com.classroom.mindmeter.model.Attempt;
import com.classroom.mindmeter.model.PracticeSession;
import com.classroom.mindmeter.model.User;
import com.classroom.mindmeter.repository.AttemptRepository;
import com.classroom.mindmeter.repository.PracticeSessionRepository;
import com.classroom.mindmeter.repository.UserRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class StudentDashboardServiceTest {

    private static final String STUDENT_EMAIL = "student@example.com";
    private static final Long STUDENT_ID = 7L;

    @Mock
    private UserRepository userRepository;

    @Mock
    private AttemptRepository attemptRepository;

    @Mock
    private PracticeSessionRepository practiceSessionRepository;

    @InjectMocks
    private StudentDashboardService dashboardService;

    @Test
    void lifetimePracticeTotalsUseValidAttemptTimestampsAcrossHistory() {
        User student = new User();
        student.setId(STUDENT_ID);
        student.setEmail(STUDENT_EMAIL);
        student.setRole("STUDENT");

        LocalDateTime historicalEnd = LocalDateTime.of(2025, 1, 2, 12, 0);
        Attempt completedAttempt = attempt(
                student,
                historicalEnd.minusMinutes(20),
                historicalEnd,
                true,
                60L
        );
        completedAttempt.setId(1L);
        Attempt legacyAttempt = attempt(
                student,
                historicalEnd.minusMinutes(10),
                historicalEnd.minusMinutes(5),
                true,
                0L
        );
        legacyAttempt.setId(2L);
        Attempt unfinishedAttempt = attempt(
                student,
                historicalEnd.minusMinutes(30),
                historicalEnd,
                false,
                900L
        );
        unfinishedAttempt.setId(3L);
        Attempt missingEndAttempt = attempt(
                student,
                historicalEnd.minusMinutes(15),
                null,
                true,
                900L
        );
        missingEndAttempt.setId(4L);
        Attempt missingStartAttempt = attempt(
                student,
                null,
                historicalEnd,
                true,
                900L
        );
        missingStartAttempt.setId(8L);
        Attempt invalidEndAttempt = attempt(
                student,
                historicalEnd,
                historicalEnd.minusMinutes(1),
                true,
                60L
        );
        invalidEndAttempt.setId(5L);
        Attempt validAttemptOnAnotherHistoricalDate = attempt(
                student,
                LocalDateTime.of(2024, 12, 31, 23, 59, 1),
                LocalDateTime.of(2025, 1, 1, 0, 0),
                true,
                0L
        );
        validAttemptOnAnotherHistoricalDate.setId(6L);
        Attempt pausedAttempt = attempt(
                student,
                historicalEnd.minusMinutes(30),
                historicalEnd.minusMinutes(20),
                true,
                0L
        );
        pausedAttempt.setId(7L);
        pausedAttempt.setPauseSecondsUsed(120);
        Attempt invalidPauseAttempt = attempt(
                student,
                historicalEnd.minusMinutes(1),
                historicalEnd,
                true,
                0L
        );
        invalidPauseAttempt.setId(9L);
        invalidPauseAttempt.setPauseSecondsUsed(61);
        Attempt negativePauseAttempt = attempt(
                student,
                historicalEnd.minusMinutes(1),
                historicalEnd,
                true,
                0L
        );
        negativePauseAttempt.setId(10L);
        negativePauseAttempt.setPauseSecondsUsed(-1);

        PracticeSession recordedSession = new PracticeSession();
        recordedSession.setStartTime(historicalEnd.minusMinutes(40));
        recordedSession.setEndTime(historicalEnd);
        recordedSession.setDurationMinutes(40);
        recordedSession.setDurationSeconds(2_400L);

        PracticeSession legacySession = new PracticeSession();
        legacySession.setStartTime(historicalEnd.minusMinutes(10));
        legacySession.setEndTime(historicalEnd);
        legacySession.setDurationMinutes(10);

        when(userRepository.findByEmail(STUDENT_EMAIL))
                .thenReturn(Optional.of(student));
        when(attemptRepository.findByUserId(STUDENT_ID))
                .thenReturn(List.of(
                        completedAttempt,
                        legacyAttempt,
                        unfinishedAttempt,
                        missingEndAttempt,
                        missingStartAttempt,
                        invalidEndAttempt,
                        validAttemptOnAnotherHistoricalDate,
                        pausedAttempt,
                        invalidPauseAttempt,
                        negativePauseAttempt,
                        completedAttempt
                ));
        when(practiceSessionRepository.findByUserId(STUDENT_ID))
                .thenReturn(List.of(recordedSession, legacySession));

        var dashboard = dashboardService.getDashboard(STUDENT_EMAIL);

        assertEquals(9, dashboard.getTotalAttempts());
        assertEquals(2_039L, dashboard.getTotalPracticeSeconds());
        assertEquals(33L, dashboard.getTotalPracticeMinutes());
        assertEquals(2_039.0 / 3_600.0, dashboard.getTotalPracticeHours());
        verify(attemptRepository).findByUserId(STUDENT_ID);
    }

    @Test
    void lifetimePracticeDurationIsZeroWhenNoAttemptHasValidElapsedTime() {
        User student = new User();
        student.setId(STUDENT_ID);
        student.setEmail(STUDENT_EMAIL);
        student.setRole("STUDENT");

        LocalDateTime timestamp = LocalDateTime.of(2025, 1, 2, 12, 0);
        List<Attempt> attempts = List.of(
                attempt(student, timestamp, null, true, 300L),
                attempt(student, timestamp, timestamp.minusSeconds(1), true, 300L),
                attempt(student, timestamp, timestamp, true, 300L)
        );

        when(userRepository.findByEmail(STUDENT_EMAIL))
                .thenReturn(Optional.of(student));
        when(attemptRepository.findByUserId(STUDENT_ID))
                .thenReturn(attempts);
        when(practiceSessionRepository.findByUserId(STUDENT_ID))
                .thenReturn(List.of());

        var dashboard = dashboardService.getDashboard(STUDENT_EMAIL);

        assertEquals(0L, dashboard.getTotalPracticeSeconds());
        assertEquals(0L, dashboard.getTotalPracticeMinutes());
        assertEquals(0.0, dashboard.getTotalPracticeHours());
    }

    private Attempt attempt(
            User student,
            LocalDateTime startTime,
            LocalDateTime endTime,
            boolean submitted,
            long durationSeconds) {
        Attempt attempt = new Attempt();
        attempt.setUser(student);
        attempt.setStartTime(startTime);
        attempt.setEndTime(endTime);
        attempt.setSubmitted(submitted);
        attempt.setStatus(submitted ? "SUBMITTED" : "IN_PROGRESS");
        attempt.setScore(0);
        attempt.setTotalMarks(0);
        attempt.setPauseSecondsUsed(0);
        attempt.setDurationSeconds(durationSeconds);
        return attempt;
    }

    @Test
    void lifetimePracticeTotalsAreIsolatedByStudentDatabaseId() {
        User firstStudent = student("first@example.com", 7L);
        User secondStudent = student("second@example.com", 8L);
        LocalDateTime end = LocalDateTime.of(2025, 1, 2, 12, 0);

        when(userRepository.findByEmail(firstStudent.getEmail()))
                .thenReturn(Optional.of(firstStudent));
        when(userRepository.findByEmail(secondStudent.getEmail()))
                .thenReturn(Optional.of(secondStudent));
        when(attemptRepository.findByUserId(7L))
                .thenReturn(List.of(attempt(
                        firstStudent,
                        end.minusMinutes(3),
                        end,
                        true,
                        0L
                )));
        when(attemptRepository.findByUserId(8L))
                .thenReturn(List.of(
                        attempt(
                                secondStudent,
                                end.minusMinutes(10),
                                end,
                                true,
                                0L
                        ),
                        attempt(
                                secondStudent,
                                end.minusMinutes(5),
                                end,
                                true,
                                0L
                        )
                ));
        when(practiceSessionRepository.findByUserId(7L))
                .thenReturn(List.of());
        when(practiceSessionRepository.findByUserId(8L))
                .thenReturn(List.of());

        var firstDashboard =
                dashboardService.getDashboard(firstStudent.getEmail());
        var secondDashboard =
                dashboardService.getDashboard(secondStudent.getEmail());

        assertEquals(180L, firstDashboard.getTotalPracticeSeconds());
        assertEquals(900L, secondDashboard.getTotalPracticeSeconds());
        verify(attemptRepository).findByUserId(7L);
        verify(attemptRepository).findByUserId(8L);
    }

    @Test
    void wholeMinutesAreDerivedFromTotalSecondsWithoutHourRounding() {
        User student = student(STUDENT_EMAIL, STUDENT_ID);
        LocalDateTime end = LocalDateTime.of(2025, 1, 2, 12, 0);
        Attempt fiftyNineMinutes = attempt(
                student, end.minusMinutes(59), end, true, 0L
        );
        Attempt oneMinute = attempt(
                student, end.minusMinutes(1), end, true, 0L
        );
        Attempt ninetyMinutes = attempt(
                student, end.minusMinutes(90), end, true, 0L
        );

        when(userRepository.findByEmail(STUDENT_EMAIL))
                .thenReturn(Optional.of(student));
        when(attemptRepository.findByUserId(STUDENT_ID))
                .thenReturn(List.of(fiftyNineMinutes));
        when(practiceSessionRepository.findByUserId(STUDENT_ID))
                .thenReturn(List.of());
        var fiftyNineMinuteDashboard =
                dashboardService.getDashboard(STUDENT_EMAIL);
        assertEquals(59L, fiftyNineMinuteDashboard.getTotalPracticeMinutes());

        when(attemptRepository.findByUserId(STUDENT_ID))
                .thenReturn(List.of(oneMinute));
        var oneMinuteDashboard =
                dashboardService.getDashboard(STUDENT_EMAIL);
        assertEquals(1L, oneMinuteDashboard.getTotalPracticeMinutes());

        when(attemptRepository.findByUserId(STUDENT_ID))
                .thenReturn(List.of(fiftyNineMinutes, oneMinute));
        var sixtyMinuteDashboard =
                dashboardService.getDashboard(STUDENT_EMAIL);
        assertEquals(60L, sixtyMinuteDashboard.getTotalPracticeMinutes());

        when(attemptRepository.findByUserId(STUDENT_ID))
                .thenReturn(List.of(ninetyMinutes));
        var ninetyMinuteDashboard =
                dashboardService.getDashboard(STUDENT_EMAIL);
        assertEquals(90L, ninetyMinuteDashboard.getTotalPracticeMinutes());
        assertEquals(1.5, ninetyMinuteDashboard.getTotalPracticeHours());
    }

    private User student(String email, Long id) {
        User student = new User();
        student.setId(id);
        student.setEmail(email);
        student.setRole("STUDENT");
        return student;
    }
}
