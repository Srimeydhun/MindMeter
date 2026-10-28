package com.classroom.mindmeter.controller;

import com.classroom.mindmeter.model.Subject;
import com.classroom.mindmeter.service.SubjectService;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/subjects")
@CrossOrigin(origins = {
        "http://localhost:5173",
        "http://127.0.0.1:5173",
        "http://mindmeter.local:5173"
})
public class SubjectController {

    private final SubjectService subjectService;

    public SubjectController(
            SubjectService subjectService) {

        this.subjectService = subjectService;
    }

    /*
     * =========================================================
     * GET ALL SUBJECTS
     * =========================================================
     *
     * URL:
     * GET /api/subjects
     *
     * Used by student functionality.
     */
    @GetMapping
    public ResponseEntity<List<Subject>> getAllSubjects() {

        return ResponseEntity.ok(
                subjectService.getAllSubjects()
        );
    }

    /*
     * =========================================================
     * GET MY SUBJECTS
     * =========================================================
     *
     * URL:
     * GET /api/subjects/my
     *
     * Returns only subjects created by the
     * currently logged-in teacher.
     */
    @GetMapping("/my")
    public ResponseEntity<?> getMySubjects(
            Authentication authentication) {

        try {

            /*
             * authentication.getName()
             * returns the email from JWT authentication.
             */
            String teacherEmail =
                    authentication.getName();

            List<Subject> subjects =
                    subjectService
                            .getSubjectsByTeacherEmail(
                                    teacherEmail
                            );

            return ResponseEntity.ok(subjects);

        } catch (Exception e) {

            return ResponseEntity
                    .status(HttpStatus.BAD_REQUEST)
                    .body(
                            Map.of(
                                    "message",
                                    e.getMessage()
                            )
                    );
        }
    }

    /*
     * =========================================================
     * CREATE SUBJECT
     * =========================================================
     *
     * URL:
     * POST /api/subjects
     *
     * Request body:
     *
     * {
     *     "name": "Data Structures",
     *     "description": "Arrays, linked lists and trees"
     * }
     *
     * Teacher ID is NOT sent by frontend.
     *
     * Backend gets teacher from JWT.
     */
    @PostMapping
    public ResponseEntity<?> createSubject(
            @RequestBody SubjectRequest request,
            Authentication authentication) {

        try {

            String teacherEmail =
                    authentication.getName();

            Subject subject =
                    subjectService.createSubject(
                            request.getName(),
                            request.getDescription(),
                            teacherEmail
                    );

            return ResponseEntity
                    .status(HttpStatus.CREATED)
                    .body(subject);

        } catch (Exception e) {

            return ResponseEntity
                    .status(HttpStatus.BAD_REQUEST)
                    .body(
                            Map.of(
                                    "message",
                                    e.getMessage()
                            )
                    );
        }
    }

    /*
     * =========================================================
     * UPDATE SUBJECT
     * =========================================================
     *
     * URL:
     * PUT /api/subjects/{id}
     */
    @PutMapping("/{id}")
    public ResponseEntity<?> updateSubject(
            @PathVariable Long id,
            @RequestBody SubjectRequest request,
            Authentication authentication) {

        try {

            String teacherEmail =
                    authentication.getName();

            Subject subject =
                    subjectService.updateSubject(
                            id,
                            request.getName(),
                            request.getDescription(),
                            teacherEmail
                    );

            return ResponseEntity.ok(subject);

        } catch (Exception e) {

            return ResponseEntity
                    .status(HttpStatus.BAD_REQUEST)
                    .body(
                            Map.of(
                                    "message",
                                    e.getMessage()
                            )
                    );
        }
    }

    /*
     * =========================================================
     * DELETE SUBJECT
     * =========================================================
     *
     * URL:
     * DELETE /api/subjects/{id}
     */
    @DeleteMapping("/{id}")
    public ResponseEntity<?> deleteSubject(
            @PathVariable Long id,
            Authentication authentication) {

        try {

            String teacherEmail =
                    authentication.getName();

            subjectService.deleteSubject(
                    id,
                    teacherEmail
            );

            return ResponseEntity.ok(
                    Map.of(
                            "message",
                            "Subject deleted successfully"
                    )
            );

        } catch (Exception e) {

            return ResponseEntity
                    .status(HttpStatus.BAD_REQUEST)
                    .body(
                            Map.of(
                                    "message",
                                    e.getMessage()
                            )
                    );
        }
    }

    /*
     * =========================================================
     * REQUEST CLASS
     * =========================================================
     */
    public static class SubjectRequest {

        private String name;

        private String description;

        public SubjectRequest() {
        }

        public String getName() {
            return name;
        }

        public void setName(String name) {
            this.name = name;
        }

        public String getDescription() {
            return description;
        }

        public void setDescription(
                String description) {

            this.description = description;
        }
    }
}