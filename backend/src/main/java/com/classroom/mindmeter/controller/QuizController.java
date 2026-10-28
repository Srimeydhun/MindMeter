package com.classroom.mindmeter.controller;

import com.classroom.mindmeter.dto.QuizRequest;
import com.classroom.mindmeter.dto.QuizResponse;
import com.classroom.mindmeter.service.QuizService;

import jakarta.validation.Valid;

import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/quizzes")
@CrossOrigin(origins = {
        "http://localhost:5173",
        "http://127.0.0.1:5173",
        "http://mindmeter.local:5173"
})
public class QuizController {

    private final QuizService quizService;

    public QuizController(QuizService quizService) {
        this.quizService = quizService;
    }

    // =========================================================
    // CREATE QUIZ
    // =========================================================

    @PostMapping
    @PreAuthorize("hasRole('TEACHER')")
    public ResponseEntity<QuizResponse> createQuiz(
            @Valid @RequestBody QuizRequest request,
            Authentication authentication) {

        return ResponseEntity.ok(
                quizService.createQuiz(
                        request,
                        authentication.getName()
                )
        );
    }

    // =========================================================
    // GET ALL QUIZZES
    // =========================================================

    @GetMapping
    public ResponseEntity<List<QuizResponse>> getAllQuizzes() {

        return ResponseEntity.ok(
                quizService.getAllQuizzes()
        );
    }

    // =========================================================
    // GET QUIZ BY ID
    // =========================================================

    @GetMapping("/{id}")
    public ResponseEntity<QuizResponse> getQuizById(
            @PathVariable Long id) {

        return ResponseEntity.ok(
                quizService.getQuizById(id)
        );
    }

    // =========================================================
    // GET QUIZZES BY SUBJECT
    // =========================================================

    @GetMapping("/subject/{subjectId}")
    public ResponseEntity<List<QuizResponse>> getQuizzesBySubject(
            @PathVariable Long subjectId) {

        return ResponseEntity.ok(
                quizService.getQuizzesBySubject(subjectId)
        );
    }

    // =========================================================
    // UPDATE QUIZ
    // =========================================================

    @PutMapping("/{id}")
    @PreAuthorize("hasRole('TEACHER')")
    public ResponseEntity<QuizResponse> updateQuiz(
            @PathVariable Long id,
            @Valid @RequestBody QuizRequest request,
            Authentication authentication) {

        return ResponseEntity.ok(
                quizService.updateQuiz(
                        id,
                        request,
                        authentication.getName()
                )
        );
    }

    // =========================================================
    // DELETE QUIZ
    // =========================================================

    @DeleteMapping("/{id}")
    @PreAuthorize("hasRole('TEACHER')")
    public ResponseEntity<String> deleteQuiz(
            @PathVariable Long id,
            Authentication authentication) {

        quizService.deleteQuiz(
                id,
                authentication.getName()
        );

        return ResponseEntity.ok(
                "Quiz deleted successfully"
        );
    }
}