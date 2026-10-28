package com.classroom.mindmeter.service;

import com.classroom.mindmeter.dto.PracticeSessionRequest;
import com.classroom.mindmeter.dto.PracticeSessionResponse;
import com.classroom.mindmeter.model.PracticeSession;
import com.classroom.mindmeter.model.Subject;
import com.classroom.mindmeter.model.User;
import com.classroom.mindmeter.repository.PracticeSessionRepository;
import com.classroom.mindmeter.repository.SubjectRepository;
import com.classroom.mindmeter.repository.UserRepository;

import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.time.ZoneId;
import java.util.List;

@Service
public class PracticeSessionService {

    private static final ZoneId APPLICATION_ZONE =
            ZoneId.of("Asia/Kolkata");

    private final PracticeSessionRepository practiceSessionRepository;
    private final UserRepository userRepository;
    private final SubjectRepository subjectRepository;

    public PracticeSessionService(
            PracticeSessionRepository practiceSessionRepository,
            UserRepository userRepository,
            SubjectRepository subjectRepository) {

        this.practiceSessionRepository = practiceSessionRepository;
        this.userRepository = userRepository;
        this.subjectRepository = subjectRepository;
    }

    // ==========================================
    // CREATE PRACTICE SESSION
    // ==========================================

    public PracticeSessionResponse createSession(
            PracticeSessionRequest request,
            String studentEmail) {

        if (!request.isDurationValid()
                || (request.getDurationMinutes() != null
                && request.getDurationMinutes() < 1)
                || (request.getDurationSeconds() != null
                && (request.getDurationSeconds() < 1
                || request.getDurationSeconds() > 128849018820L))) {
            throw new IllegalArgumentException(
                    "Provide exactly one positive practice duration"
            );
        }

        long durationSeconds = request.getDurationSeconds() != null
                ? request.getDurationSeconds()
                : request.getDurationMinutes() * 60L;
        int durationMinutes = Math.toIntExact(
                (durationSeconds + 59L) / 60L
        );

        User student = userRepository.findByEmail(studentEmail)
                .orElseThrow(() ->
                        new RuntimeException(
                                "Student not found"
                        )
                );

        if (!"STUDENT".equalsIgnoreCase(student.getRole())) {
            throw new RuntimeException(
                    "Only students can record practice sessions"
            );
        }

        Subject subject = null;

        if (request.getSubjectId() != null) {

            subject = subjectRepository.findById(
                    request.getSubjectId()
            ).orElseThrow(() ->
                    new RuntimeException(
                            "Subject not found with ID: "
                                    + request.getSubjectId()
                    )
            );
        }

        LocalDateTime endTime = LocalDateTime.now(APPLICATION_ZONE);

        LocalDateTime startTime =
                endTime.minusSeconds(durationSeconds);

        PracticeSession session = new PracticeSession();

        session.setUser(student);
        session.setSubject(subject);
        session.setStartTime(startTime);
        session.setEndTime(endTime);
        session.setDurationMinutes(durationMinutes);
        session.setDurationSeconds(durationSeconds);

        PracticeSession savedSession =
                practiceSessionRepository.save(session);

        return convertToResponse(savedSession);
    }

    // ==========================================
    // GET MY PRACTICE SESSIONS
    // ==========================================

    public List<PracticeSessionResponse> getMySessions(
            String studentEmail) {

        User student = userRepository.findByEmail(studentEmail)
                .orElseThrow(() ->
                        new RuntimeException(
                                "Student not found"
                        )
                );

        return practiceSessionRepository
                .findByUserId(student.getId())
                .stream()
                .map(this::convertToResponse)
                .toList();
    }

    // ==========================================
    // GET TOTAL PRACTICE MINUTES
    // ==========================================

    public long getTotalPracticeMinutes(
            String studentEmail) {

        User student = userRepository.findByEmail(studentEmail)
                .orElseThrow(() ->
                        new RuntimeException(
                                "Student not found"
                        )
                );

        return practiceSessionRepository
                .findByUserId(student.getId())
                .stream()
                .mapToLong(PracticeSessionService::getDurationSeconds)
                .sum() / 60L;
    }

    // ==========================================
    // CONVERT ENTITY TO SAFE RESPONSE
    // ==========================================

    private PracticeSessionResponse convertToResponse(
            PracticeSession session) {

        Long subjectId = null;
        String subjectName = null;

        if (session.getSubject() != null) {
            subjectId = session.getSubject().getId();
            subjectName = session.getSubject().getName();
        }

        return new PracticeSessionResponse(
                session.getId(),
                session.getUser().getId(),
                session.getUser().getName(),
                session.getUser().getEmail(),
                subjectId,
                subjectName,
                session.getStartTime(),
                session.getEndTime(),
                session.getDurationMinutes(),
                getDurationSeconds(session)
        );
    }

    private static long getDurationSeconds(PracticeSession session) {
        if (session.getDurationSeconds() != null) {
            return session.getDurationSeconds();
        }

        return Math.multiplyExact(
                session.getDurationMinutes().longValue(),
                60L
        );
    }
}