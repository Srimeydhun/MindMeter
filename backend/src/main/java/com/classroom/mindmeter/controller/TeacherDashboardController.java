package com.classroom.mindmeter.controller;

import com.classroom.mindmeter.dto.QuizAttendanceResponse;
import com.classroom.mindmeter.dto.TeacherDashboardResponse;
import com.classroom.mindmeter.service.TeacherDashboardService;

import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/teacher")
@CrossOrigin(origins = {
        "http://localhost:5173",
        "http://127.0.0.1:5173",
        "http://mindmeter.local:5173"
})
public class TeacherDashboardController {

    private final TeacherDashboardService teacherDashboardService;

    public TeacherDashboardController(
            TeacherDashboardService teacherDashboardService) {

        this.teacherDashboardService = teacherDashboardService;
    }

    // ==========================================
    // TEACHER DASHBOARD SUMMARY
    // ==========================================

    @GetMapping("/dashboard")
    @PreAuthorize("hasRole('TEACHER')")
    public ResponseEntity<TeacherDashboardResponse> getDashboard() {

        return ResponseEntity.ok(
                teacherDashboardService.getDashboard()
        );
    }

    // ==========================================
    // QUIZ-WISE ATTENDANCE
    // ==========================================

    @GetMapping("/attendance")
    @PreAuthorize("hasRole('TEACHER')")
    public ResponseEntity<List<QuizAttendanceResponse>> getQuizAttendance() {

        return ResponseEntity.ok(
                teacherDashboardService.getQuizAttendance()
        );
    }
}