package com.classroom.mindmeter.service;

import com.classroom.mindmeter.dto.StudentDashboardResponse;
import com.classroom.mindmeter.dto.SubjectPerformanceResponse;
import com.classroom.mindmeter.model.Attempt;
import com.classroom.mindmeter.model.PracticeSession;
import com.classroom.mindmeter.model.User;
import com.classroom.mindmeter.repository.AttemptRepository;
import com.classroom.mindmeter.repository.PracticeSessionRepository;
import com.classroom.mindmeter.repository.UserRepository;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import java.time.Duration;
import java.time.LocalDate;
import java.time.ZoneId;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

@Service
public class StudentDashboardService {

    private static final Logger LOGGER =
            LoggerFactory.getLogger(StudentDashboardService.class);
    private static final ZoneId APPLICATION_ZONE =
            ZoneId.of("Asia/Kolkata");

    private final UserRepository userRepository;
    private final AttemptRepository attemptRepository;
    private final PracticeSessionRepository practiceSessionRepository;

    public StudentDashboardService(
            UserRepository userRepository,
            AttemptRepository attemptRepository,
            PracticeSessionRepository practiceSessionRepository) {

        this.userRepository = userRepository;
        this.attemptRepository = attemptRepository;
        this.practiceSessionRepository = practiceSessionRepository;
    }

    // ==========================================
    // STUDENT DASHBOARD
    // ==========================================

    public StudentDashboardResponse getDashboard(
            String studentEmail) {

        // Find logged-in student
        User student = userRepository.findByEmail(studentEmail)
                .orElseThrow(() ->
                        new RuntimeException(
                                "Student not found"
                        )
                );

        if (!"STUDENT".equalsIgnoreCase(student.getRole())) {
            throw new RuntimeException(
                    "Only students can access this dashboard"
            );
        }

        // ==========================================
        // GET ATTEMPTS
        // ==========================================

        Set<Long> loadedAttemptIds = new HashSet<>();
        List<Attempt> allAttempts = attemptRepository
                .findByUserId(student.getId())
                .stream()
                .filter(attempt -> attempt.getId() == null
                        || loadedAttemptIds.add(attempt.getId()))
                .toList();
        List<Attempt> attempts = allAttempts.stream()
                .filter(attempt ->
                        Boolean.TRUE.equals(attempt.getSubmitted())
                )
                .toList();

        // ==========================================
        // GET PRACTICE SESSIONS
        // ==========================================

        List<PracticeSession> practiceSessions =
                practiceSessionRepository.findByUserId(
                        student.getId()
                );

        // ==========================================
        // TOTAL ATTEMPTS
        // ==========================================

        long totalAttempts = attempts.size();

        // ==========================================
        // CUMULATIVE SCORE
        // ==========================================

        int cumulativeScore = attempts.stream()
                .mapToInt(attempt -> attempt.getScore())
                .sum();

        // ==========================================
        // CUMULATIVE TOTAL MARKS
        // ==========================================

        int cumulativeTotalMarks = attempts.stream()
                .mapToInt(attempt -> attempt.getTotalMarks())
                .sum();

        // ==========================================
        // AVERAGE PERCENTAGE
        // ==========================================

        double averagePercentage = 0.0;

        if (cumulativeTotalMarks > 0) {

            averagePercentage =
                    (cumulativeScore * 100.0)
                            / cumulativeTotalMarks;
        }

        averagePercentage =
                Math.round(averagePercentage * 100.0) / 100.0;

        // ==========================================
        // TOTAL PRACTICE MINUTES
        // ==========================================

        // The card uses completed quiz-attempt timestamps only. Manual practice
        // sessions remain available for streaks but can overlap these attempts.
        long totalPracticeSeconds = allAttempts.stream()
                .filter(attempt ->
                        Boolean.TRUE.equals(attempt.getSubmitted()))
                .mapToLong(this::getAttemptDurationSeconds)
                .sum();

        long totalPracticeMinutes = totalPracticeSeconds / 60L;
        if (LOGGER.isDebugEnabled()) {
            long validDurationAttempts = allAttempts.stream()
                    .filter(this::hasValidAttemptTimestamps)
                    .count();
            LOGGER.debug(
                    "Dashboard lifetime practice duration calculated: "
                            + "attempts={}, validTimestampAttempts={}, "
                            + "seconds={}, minutes={}",
                    allAttempts.size(),
                    validDurationAttempts,
                    totalPracticeSeconds,
                    totalPracticeMinutes
            );
        }

        // ==========================================
        // TOTAL PRACTICE HOURS
        // ==========================================

        double totalPracticeHours = totalPracticeSeconds / 3600.0;

        // ==========================================
        // LEARNING STREAK
        // ==========================================

        int learningStreakDays =
                calculateLearningStreak(
                        attempts,
                        practiceSessions
                );

        // ==========================================
        // SUBJECT-WISE PERFORMANCE
        // ==========================================

        List<SubjectPerformanceResponse>
                subjectPerformance =
                calculateSubjectPerformance(attempts);

        // ==========================================
        // RETURN DASHBOARD
        // ==========================================

        return new StudentDashboardResponse(
                totalAttempts,
                cumulativeScore,
                cumulativeTotalMarks,
                totalPracticeSeconds,
                totalPracticeMinutes,
                totalPracticeHours,
                averagePercentage,
                learningStreakDays,
                subjectPerformance
        );
    }

    private long getAttemptDurationSeconds(Attempt attempt) {
        if (!hasValidAttemptTimestamps(attempt)) {
            return 0;
        }
        long elapsedSeconds = Duration.between(
                attempt.getStartTime(),
                attempt.getEndTime()
        ).getSeconds();
        Integer pausedSeconds = attempt.getPauseSecondsUsed();
        if (pausedSeconds == null
                || pausedSeconds < 0
                || pausedSeconds > elapsedSeconds) {
            return 0;
        }
        return elapsedSeconds - pausedSeconds;
    }

    private boolean hasValidAttemptTimestamps(Attempt attempt) {
        return attempt.getStartTime() != null
                && attempt.getEndTime() != null
                && !attempt.getEndTime().isBefore(attempt.getStartTime());
    }

    // ==========================================
    // CALCULATE LEARNING STREAK
    // ==========================================

    private int calculateLearningStreak(
            List<Attempt> attempts,
            List<PracticeSession> practiceSessions) {

        /*
         * A learning day is any day on which the
         * student either:
         *
         * 1. submitted a quiz, or
         * 2. completed a practice session.
         */

        Set<LocalDate> learningDates = new HashSet<>();

        // Add quiz attempt dates
        for (Attempt attempt : attempts) {

            if (attempt.getEndTime() != null) {

                learningDates.add(
                        attempt.getEndTime().toLocalDate()
                );
            }
        }

        // Add practice session dates
        for (PracticeSession session : practiceSessions) {

            if (session.getEndTime() != null) {

                learningDates.add(
                        session.getEndTime().toLocalDate()
                );
            }
        }

        // No learning activity
        if (learningDates.isEmpty()) {
            return 0;
        }

        /*
         * Start from today and move backward one day
         * at a time until a learning day is missing.
         */

        LocalDate currentDate = LocalDate.now(APPLICATION_ZONE);

        int streak = 0;

        while (learningDates.contains(currentDate)) {

            streak++;

            currentDate = currentDate.minusDays(1);
        }

        return streak;
    }

    // ==========================================
    // CALCULATE SUBJECT PERFORMANCE
    // ==========================================

    private List<SubjectPerformanceResponse>
    calculateSubjectPerformance(
            List<Attempt> attempts) {

        Map<Long, SubjectPerformanceData> subjectMap =
                new LinkedHashMap<>();

        for (Attempt attempt : attempts) {

            if (attempt.getQuiz() == null
                    || attempt.getQuiz().getSubject() == null) {
                continue;
            }

            Long subjectId =
                    attempt.getQuiz().getSubject().getId();

            String subjectName =
                    attempt.getQuiz()
                            .getSubject()
                            .getName();

            SubjectPerformanceData data =
                    subjectMap.computeIfAbsent(
                            subjectId,
                            id -> new SubjectPerformanceData(
                                    subjectId,
                                    subjectName
                            )
                    );

            data.attempts++;

            data.totalScore += attempt.getScore();

            data.totalMarks += attempt.getTotalMarks();
        }

        List<SubjectPerformanceResponse> responseList =
                new ArrayList<>();

        for (SubjectPerformanceData data :
                subjectMap.values()) {

            double percentage = 0.0;

            if (data.totalMarks > 0) {

                percentage =
                        (data.totalScore * 100.0)
                                / data.totalMarks;
            }

            percentage =
                    Math.round(percentage * 100.0) / 100.0;

            responseList.add(
                    new SubjectPerformanceResponse(
                            data.subjectId,
                            data.subjectName,
                            data.attempts,
                            data.totalScore,
                            data.totalMarks,
                            percentage
                    )
            );
        }

        return responseList;
    }

    // ==========================================
    // INTERNAL SUBJECT DATA
    // ==========================================

    private static class SubjectPerformanceData {

        private final Long subjectId;
        private final String subjectName;

        private long attempts;
        private int totalScore;
        private int totalMarks;

        public SubjectPerformanceData(
                Long subjectId,
                String subjectName) {

            this.subjectId = subjectId;
            this.subjectName = subjectName;
        }
    }
}
