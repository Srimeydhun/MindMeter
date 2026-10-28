package com.classroom.mindmeter.service;

import com.classroom.mindmeter.dto.StudentResponse;
import com.classroom.mindmeter.model.User;
import com.classroom.mindmeter.repository.UserRepository;

import org.springframework.stereotype.Service;

import java.util.List;

@Service
public class StudentManagementService {

    private final UserRepository userRepository;

    public StudentManagementService(
            UserRepository userRepository) {

        this.userRepository = userRepository;
    }

    // ==========================================
    // GET ALL STUDENTS
    // ==========================================

    public List<StudentResponse> getAllStudents() {

        return userRepository.findAll()
                .stream()
                .filter(user ->
                        "STUDENT".equalsIgnoreCase(
                                user.getRole()
                        )
                )
                .map(this::convertToResponse)
                .toList();
    }

    // ==========================================
    // GET STUDENT BY ID
    // ==========================================

    public StudentResponse getStudentById(Long id) {

        User student = userRepository.findById(id)
                .orElseThrow(() ->
                        new RuntimeException(
                                "Student not found with ID: " + id
                        )
                );

        if (!"STUDENT".equalsIgnoreCase(
                student.getRole())) {

            throw new RuntimeException(
                    "The specified user is not a student"
            );
        }

        return convertToResponse(student);
    }

    // ==========================================
    // DELETE STUDENT
    // ==========================================

    public void deleteStudent(Long id) {

        User student = userRepository.findById(id)
                .orElseThrow(() ->
                        new RuntimeException(
                                "Student not found with ID: " + id
                        )
                );

        if (!"STUDENT".equalsIgnoreCase(
                student.getRole())) {

            throw new RuntimeException(
                    "The specified user is not a student"
            );
        }

        userRepository.delete(student);
    }

    // ==========================================
    // CONVERT USER TO SAFE RESPONSE
    // ==========================================

    private StudentResponse convertToResponse(
            User user) {

        return new StudentResponse(
                user.getId(),
                user.getName(),
                user.getEmail(),
                user.getRole(),
                user.getRegisterNumber()
        );
    }
}