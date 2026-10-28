package com.classroom.mindmeter.controller;

import com.classroom.mindmeter.dto.StudentDashboardResponse;
import com.classroom.mindmeter.service.StudentDashboardService;

import org.springframework.http.CacheControl;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/student")
@CrossOrigin(origins = {
        "http://localhost:5173",
        "http://127.0.0.1:5173",
        "http://mindmeter.local:5173"
})
public class StudentDashboardController {

    private final StudentDashboardService studentDashboardService;

    public StudentDashboardController(
            StudentDashboardService studentDashboardService) {

        this.studentDashboardService = studentDashboardService;
    }

    // ==========================================
    // STUDENT DASHBOARD
    // ==========================================

    @GetMapping("/dashboard")
    @PreAuthorize("hasRole('STUDENT')")
    public ResponseEntity<StudentDashboardResponse> getDashboard(
            Authentication authentication) {

        String studentEmail = authentication.getName();

        return ResponseEntity.ok()
                .cacheControl(CacheControl.noStore())
                .body(studentDashboardService.getDashboard(studentEmail));
    }
}