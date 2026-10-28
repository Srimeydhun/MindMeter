package com.classroom.mindmeter.controller;

import com.classroom.mindmeter.dto.PracticeSessionRequest;
import com.classroom.mindmeter.dto.PracticeSessionResponse;
import com.classroom.mindmeter.service.PracticeSessionService;

import jakarta.validation.Valid;

import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/practice")
@CrossOrigin(origins = {
        "http://localhost:5173",
        "http://127.0.0.1:5173",
        "http://mindmeter.local:5173"
})
public class PracticeSessionController {

    private final PracticeSessionService practiceSessionService;

    public PracticeSessionController(
            PracticeSessionService practiceSessionService) {

        this.practiceSessionService = practiceSessionService;
    }

    // ==========================================
    // STUDENT - RECORD PRACTICE SESSION
    // ==========================================

    @PostMapping("/session")
    @PreAuthorize("hasRole('STUDENT')")
    public ResponseEntity<PracticeSessionResponse> createSession(
            @Valid @RequestBody PracticeSessionRequest request,
            Authentication authentication) {

        String studentEmail = authentication.getName();

        return ResponseEntity.ok(
                practiceSessionService.createSession(
                        request,
                        studentEmail
                )
        );
    }

    // ==========================================
    // STUDENT - VIEW MY PRACTICE SESSIONS
    // ==========================================

    @GetMapping("/sessions")
    @PreAuthorize("hasRole('STUDENT')")
    public ResponseEntity<List<PracticeSessionResponse>> getMySessions(
            Authentication authentication) {

        String studentEmail = authentication.getName();

        return ResponseEntity.ok(
                practiceSessionService.getMySessions(
                        studentEmail
                )
        );
    }

    // ==========================================
    // STUDENT - VIEW TOTAL PRACTICE MINUTES
    // ==========================================

    @GetMapping("/total-minutes")
    @PreAuthorize("hasRole('STUDENT')")
    public ResponseEntity<Long> getTotalPracticeMinutes(
            Authentication authentication) {

        String studentEmail = authentication.getName();

        return ResponseEntity.ok(
                practiceSessionService.getTotalPracticeMinutes(
                        studentEmail
                )
        );
    }
}