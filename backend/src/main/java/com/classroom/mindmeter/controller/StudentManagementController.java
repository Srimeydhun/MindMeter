package com.classroom.mindmeter.controller;

import com.classroom.mindmeter.dto.StudentResponse;
import com.classroom.mindmeter.service.StudentManagementService;

import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/teacher/students")
@CrossOrigin(origins = {
        "http://localhost:5173",
        "http://127.0.0.1:5173",
        "http://mindmeter.local:5173"
})
@PreAuthorize("hasRole('TEACHER')")
public class StudentManagementController {

    private final StudentManagementService studentManagementService;

    public StudentManagementController(
            StudentManagementService studentManagementService) {

        this.studentManagementService = studentManagementService;
    }

    // ==========================================
    // GET ALL STUDENTS
    // ==========================================

    @GetMapping
    public ResponseEntity<List<StudentResponse>> getAllStudents() {

        return ResponseEntity.ok(
                studentManagementService.getAllStudents()
        );
    }

    // ==========================================
    // GET STUDENT BY ID
    // ==========================================

    @GetMapping("/{id}")
    public ResponseEntity<StudentResponse> getStudentById(
            @PathVariable Long id) {

        return ResponseEntity.ok(
                studentManagementService.getStudentById(id)
        );
    }

    // ==========================================
    // DELETE STUDENT
    // ==========================================

    @DeleteMapping("/{id}")
    public ResponseEntity<String> deleteStudent(
            @PathVariable Long id) {

        studentManagementService.deleteStudent(id);

        return ResponseEntity.ok(
                "Student deleted successfully"
        );
    }
}