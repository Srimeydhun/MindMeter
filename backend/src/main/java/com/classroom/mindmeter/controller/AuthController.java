package com.classroom.mindmeter.controller;

import com.classroom.mindmeter.dto.AuthResponse;
import com.classroom.mindmeter.dto.LoginRequest;
import com.classroom.mindmeter.dto.RegisterRequest;
import com.classroom.mindmeter.dto.TeacherRegisterRequest;
import com.classroom.mindmeter.service.AuthService;

import jakarta.validation.Valid;

import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/auth")
@CrossOrigin(origins = {
        "http://localhost:5173",
        "http://127.0.0.1:5173",
        "http://mindmeter.local:5173"
})
public class AuthController {

    private final AuthService authService;

    public AuthController(AuthService authService) {
        this.authService = authService;
    }

    // ==========================================
    // STUDENT REGISTRATION
    // ==========================================

    @PostMapping("/register")
    public ResponseEntity<AuthResponse> register(
            @Valid @RequestBody RegisterRequest request) {

        return ResponseEntity.ok(
                authService.register(request)
        );
    }

    // ==========================================
    // TEACHER REGISTRATION
    // ==========================================

    @PostMapping("/register/teacher")
    public ResponseEntity<AuthResponse> registerTeacher(
            @Valid @RequestBody TeacherRegisterRequest request) {

        return ResponseEntity.ok(
                authService.registerTeacher(request)
        );
    }

    // ==========================================
    // LOGIN - STUDENT / TEACHER
    // ==========================================

    @PostMapping("/login")
    public ResponseEntity<AuthResponse> login(
            @Valid @RequestBody LoginRequest request) {

        return ResponseEntity.ok(
                authService.login(request)
        );
    }
}