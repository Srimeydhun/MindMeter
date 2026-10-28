package com.classroom.mindmeter.controller;

import com.classroom.mindmeter.dto.AttemptResponse;
import com.classroom.mindmeter.dto.AnswerSaveRequest;
import com.classroom.mindmeter.dto.AttemptProgressResponse;
import com.classroom.mindmeter.dto.QuizSubmission;
import com.classroom.mindmeter.service.AttemptService;

import org.springframework.http.HttpStatus;
import org.springframework.http.ProblemDetail;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/attempts")
@CrossOrigin(origins = {
        "http://localhost:5173",
        "http://127.0.0.1:5173",
        "http://mindmeter.local:5173"
})
public class AttemptController {

    private final AttemptService attemptService;

    public AttemptController(
            AttemptService attemptService) {

        this.attemptService = attemptService;
    }

    // ==========================================
    // STUDENT - SUBMIT QUIZ
    // ==========================================

    @PostMapping("/quiz/{quizId}/start")
    public ResponseEntity<AttemptProgressResponse> startOrResumeAttempt(
            @PathVariable Long quizId,
            @RequestParam(defaultValue = "false") boolean newAttempt,
            Authentication authentication) {

        return ResponseEntity.ok(
                attemptService.startOrResumeAttempt(
                        quizId,
                        authentication.getName(),
                        newAttempt
                )
        );
    }

    @GetMapping("/{attemptId}")
    public ResponseEntity<AttemptProgressResponse> getAttemptProgress(
            @PathVariable Long attemptId,
            Authentication authentication) {

        return ResponseEntity.ok(
                attemptService.getAttemptProgress(
                        attemptId,
                        authentication.getName()
                )
        );
    }

    @PutMapping("/{attemptId}/answer/{questionId}")
    public ResponseEntity<AttemptProgressResponse> saveAnswer(
            @PathVariable Long attemptId,
            @PathVariable Long questionId,
            @RequestBody AnswerSaveRequest request,
            Authentication authentication) {

        return ResponseEntity.ok(
                attemptService.saveAnswer(
                        attemptId,
                        questionId,
                        request.getAnswer(),
                        authentication.getName()
                )
        );
    }

    @PostMapping("/{attemptId}/pause")
    public ResponseEntity<AttemptProgressResponse> pauseAttempt(
            @PathVariable Long attemptId,
            Authentication authentication) {

        return ResponseEntity.ok(
                attemptService.pauseAttempt(
                        attemptId,
                        authentication.getName()
                )
        );
    }

    @PostMapping("/{attemptId}/resume")
    public ResponseEntity<AttemptProgressResponse> resumeAttempt(
            @PathVariable Long attemptId,
            Authentication authentication) {

        return ResponseEntity.ok(
                attemptService.resumeAttempt(
                        attemptId,
                        authentication.getName()
                )
        );
    }

    @PostMapping("/submit")
    public ResponseEntity<AttemptProgressResponse> submitQuiz(
            @RequestBody QuizSubmission submission,
            Authentication authentication) {

        String studentEmail =
                authentication.getName();

        return ResponseEntity.ok(
                attemptService.submitQuiz(
                        submission,
                        studentEmail
                )
        );
    }

    // ==========================================
    // STUDENT - VIEW OWN ATTEMPTS
    // ==========================================

    @GetMapping("/my")
    public ResponseEntity<List<AttemptResponse>> getMyAttempts(
            Authentication authentication) {

        String studentEmail =
                authentication.getName();

        return ResponseEntity.ok(
                attemptService.getMyAttempts(
                        studentEmail
                )
        );
    }

    // ==========================================
    // TEACHER - VIEW ALL ATTEMPTS
    // ==========================================

    @GetMapping("/all")
    public ResponseEntity<List<AttemptResponse>> getAllAttempts(
            Authentication authentication) {

        return ResponseEntity.ok(
                attemptService.getAllAttempts(authentication.getName())
        );
    }

    @GetMapping("/subject/{subjectId}/submitted")
    public ResponseEntity<List<AttemptResponse>> getSubmittedAttemptsBySubject(
            @PathVariable Long subjectId,
            Authentication authentication) {

        return ResponseEntity.ok(
                attemptService.getSubmittedAttemptsBySubject(
                        subjectId,
                        authentication.getName()
                )
        );
    }

    // ==========================================
    // TEACHER - VIEW QUIZ ATTEMPTS
    // ==========================================

    @GetMapping("/quiz/{quizId}")
    public ResponseEntity<List<AttemptResponse>> getAttemptsByQuiz(
            @PathVariable Long quizId,
            Authentication authentication) {

        return ResponseEntity.ok(
                attemptService.getAttemptsByQuiz(
                        quizId,
                        authentication.getName()
                )
        );
    }

    @GetMapping("/quiz/{quizId}/submitted")
    public ResponseEntity<?> getSubmittedAttemptsByQuiz(
            @PathVariable Long quizId,
            Authentication authentication) {

        try {
            return ResponseEntity.ok(
                    attemptService.getSubmittedAttemptsByQuiz(
                            quizId,
                            authentication.getName()
                    )
            );
        } catch (AccessDeniedException exception) {
            return ResponseEntity.status(HttpStatus.FORBIDDEN)
                    .body(ProblemDetail.forStatusAndDetail(
                            HttpStatus.FORBIDDEN,
                            exception.getMessage()
                    ));
        }
    }
}