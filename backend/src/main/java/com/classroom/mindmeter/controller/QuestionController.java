package com.classroom.mindmeter.controller;

import com.classroom.mindmeter.dto.QuestionRequest;
import com.classroom.mindmeter.dto.QuestionResponse;
import com.classroom.mindmeter.dto.StudentQuestionResponse;
import com.classroom.mindmeter.service.QuestionService;

import jakarta.validation.Valid;

import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/questions")
@CrossOrigin(origins = {
        "http://localhost:5173",
        "http://127.0.0.1:5173",
        "http://mindmeter.local:5173"
})
public class QuestionController {

    private final QuestionService questionService;

    public QuestionController(
            QuestionService questionService) {

        this.questionService = questionService;
    }

    // ==========================================
    // TEACHER - CREATE QUESTION
    // ==========================================

    @PostMapping
    @PreAuthorize("hasRole('TEACHER')")
    public ResponseEntity<QuestionResponse> createQuestion(
            @Valid @RequestBody QuestionRequest request,
            Authentication authentication) {

        return ResponseEntity.ok(
                questionService.createQuestion(
                        request,
                        authentication.getName()
                )
        );
    }

    // ==========================================
    // TEACHER - GET ALL QUESTIONS
    // ==========================================

    @GetMapping
    @PreAuthorize("hasRole('TEACHER')")
    public ResponseEntity<List<QuestionResponse>>
    getAllQuestions(
            Authentication authentication) {

        return ResponseEntity.ok(
                questionService.getAllQuestions(
                        authentication.getName()
                )
        );
    }

    // ==========================================
    // TEACHER - GET QUESTION BY ID
    // ==========================================

    @GetMapping("/{id}")
    @PreAuthorize("hasRole('TEACHER')")
    public ResponseEntity<QuestionResponse>
    getQuestionById(
            @PathVariable Long id,
            Authentication authentication) {

        return ResponseEntity.ok(
                questionService.getQuestionById(
                        id,
                        authentication.getName()
                )
        );
    }

    // ==========================================
    // TEACHER - GET QUESTIONS BY QUIZ
    // ==========================================

    @GetMapping("/quiz/{quizId}")
    @PreAuthorize("hasRole('TEACHER')")
    public ResponseEntity<List<QuestionResponse>>
    getQuestionsByQuiz(
            @PathVariable Long quizId,
            Authentication authentication) {

        return ResponseEntity.ok(
                questionService.getQuestionsByQuiz(
                        quizId,
                        authentication.getName()
                )
        );
    }

    // ==========================================
    // STUDENT - GET QUESTIONS BY QUIZ
    // ==========================================

    @GetMapping("/student/quiz/{quizId}")
    @PreAuthorize("hasRole('STUDENT')")
    public ResponseEntity<List<StudentQuestionResponse>>
    getStudentQuestionsByQuiz(
            @PathVariable Long quizId) {

        return ResponseEntity.ok(
                questionService.getStudentQuestionsByQuiz(
                        quizId
                )
        );
    }

    // ==========================================
    // STUDENT - GET QUESTION BY ID
    // ==========================================

    @GetMapping("/student/{id}")
    @PreAuthorize("hasRole('STUDENT')")
    public ResponseEntity<StudentQuestionResponse>
    getStudentQuestionById(
            @PathVariable Long id) {

        return ResponseEntity.ok(
                questionService.getStudentQuestionById(
                        id
                )
        );
    }

    // ==========================================
    // TEACHER - UPDATE QUESTION
    // ==========================================

    @PutMapping("/{id}")
    @PreAuthorize("hasRole('TEACHER')")
    public ResponseEntity<QuestionResponse>
    updateQuestion(
            @PathVariable Long id,
            @Valid @RequestBody QuestionRequest request,
            Authentication authentication) {

        return ResponseEntity.ok(
                questionService.updateQuestion(
                        id,
                        request,
                        authentication.getName()
                )
        );
    }

    // ==========================================
    // TEACHER - DELETE QUESTION
    // ==========================================

    @DeleteMapping("/{id}")
    @PreAuthorize("hasRole('TEACHER')")
    public ResponseEntity<String> deleteQuestion(
            @PathVariable Long id,
            Authentication authentication) {

        questionService.deleteQuestion(
                id,
                authentication.getName()
        );

        return ResponseEntity.ok(
                "Question deleted successfully"
        );
    }
}