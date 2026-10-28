package com.classroom.mindmeter.controller;

import com.classroom.mindmeter.security.CustomUserDetailsService;
import com.classroom.mindmeter.security.JwtService;
import com.classroom.mindmeter.service.AttemptService;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.http.MediaType;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import java.util.List;

import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.options;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;
import static org.hamcrest.Matchers.allOf;
import static org.hamcrest.Matchers.containsString;

import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.core.userdetails.User;
import org.springframework.security.core.userdetails.UserDetails;

@SpringBootTest
@AutoConfigureMockMvc
class SubmittedAttemptsSecurityTest {

    private static final String ENDPOINT =
            "/api/attempts/quiz/1/submitted";
    private static final String ALL_ATTEMPTS_ENDPOINT =
            "/api/attempts/all";
    private static final String TEACHER_EMAIL = "teacher@example.com";
    private static final String STUDENT_EMAIL = "student@example.com";

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private JwtService jwtService;

    @MockitoBean
    private CustomUserDetailsService userDetailsService;

    @MockitoBean
    private AttemptService attemptService;

    @Test
    void permitsAuthenticatedTeacherAndUsesJwtIdentity() throws Exception {
        when(userDetailsService.loadUserByUsername(TEACHER_EMAIL))
                .thenReturn(user(TEACHER_EMAIL, "ROLE_TEACHER"));
        when(attemptService.getSubmittedAttemptsByQuiz(
                1L,
                TEACHER_EMAIL
        )).thenReturn(List.of());

        mockMvc.perform(get(ENDPOINT)
                        .header("Authorization", bearerToken(
                                TEACHER_EMAIL,
                                "ROLE_TEACHER"
                        ))
                        .accept(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk());

        verify(attemptService).getSubmittedAttemptsByQuiz(1L, TEACHER_EMAIL);
    }

    @Test
    void permitsTeacherToLoadOnlyTheirDashboardAttempts() throws Exception {
        when(userDetailsService.loadUserByUsername(TEACHER_EMAIL))
                .thenReturn(user(TEACHER_EMAIL, "ROLE_TEACHER"));
        when(attemptService.getAllAttempts(TEACHER_EMAIL))
                .thenReturn(List.of());

        mockMvc.perform(get(ALL_ATTEMPTS_ENDPOINT)
                        .header("Authorization", bearerToken(
                                TEACHER_EMAIL,
                                "ROLE_TEACHER"
                        ))
                        .accept(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk());

        verify(attemptService).getAllAttempts(TEACHER_EMAIL);
    }

    @Test
    void deniesStudentAccessToAllTeacherDashboardAttempts() throws Exception {
        when(userDetailsService.loadUserByUsername(STUDENT_EMAIL))
                .thenReturn(user(STUDENT_EMAIL, "ROLE_STUDENT"));

        mockMvc.perform(get(ALL_ATTEMPTS_ENDPOINT)
                        .header("Authorization", bearerToken(
                                STUDENT_EMAIL,
                                "ROLE_STUDENT"
                        )))
                .andExpect(status().isForbidden());

        verify(attemptService, never()).getAllAttempts(STUDENT_EMAIL);
    }

    @Test
    void returnsUnauthorizedForAllAttemptsWhenJwtIsMissing() throws Exception {
        mockMvc.perform(get(ALL_ATTEMPTS_ENDPOINT))
                .andExpect(status().isUnauthorized());

        verify(attemptService, never()).getAllAttempts(TEACHER_EMAIL);
    }

    @Test
    void deniesAuthenticatedStudent() throws Exception {
        when(userDetailsService.loadUserByUsername(STUDENT_EMAIL))
                .thenReturn(user(STUDENT_EMAIL, "ROLE_STUDENT"));

        mockMvc.perform(get(ENDPOINT)
                        .header("Authorization", bearerToken(
                                STUDENT_EMAIL,
                                "ROLE_STUDENT"
                        )))
                .andExpect(status().isForbidden());

        verify(attemptService, never())
                .getSubmittedAttemptsByQuiz(1L, STUDENT_EMAIL);
    }

    @Test
    void returnsForbiddenWhenTeacherDoesNotOwnQuiz() throws Exception {
        when(userDetailsService.loadUserByUsername(TEACHER_EMAIL))
                .thenReturn(user(TEACHER_EMAIL, "ROLE_TEACHER"));
        doThrow(new AccessDeniedException(
                "You are not allowed to view submissions for this quiz"
        )).when(attemptService).getSubmittedAttemptsByQuiz(
                1L,
                TEACHER_EMAIL
        );

        mockMvc.perform(get(ENDPOINT)
                        .header("Authorization", bearerToken(
                                TEACHER_EMAIL,
                                "ROLE_TEACHER"
                        )))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.detail").value(
                        "You are not allowed to view submissions for this quiz"
                ));
    }

    @Test
    void returnsUnauthorizedWhenTokenIsMissing() throws Exception {
        mockMvc.perform(get(ENDPOINT))
                .andExpect(status().isUnauthorized());
    }

    @Test
    void returnsUnauthorizedWhenTokenIsInvalid() throws Exception {
        mockMvc.perform(get(ENDPOINT)
                        .header("Authorization", "Bearer invalid-token"))
                .andExpect(status().isUnauthorized());
    }

    @Test
    void permitsDashboardPreflightWithNoCacheRequestHeaders() throws Exception {
        mockMvc.perform(options("/api/student/dashboard")
                        .header("Origin", "http://localhost:5173")
                        .header("Access-Control-Request-Method", "GET")
                        .header(
                                "Access-Control-Request-Headers",
                                "authorization,content-type,cache-control,pragma"
                        ))
                .andExpect(status().isOk())
                .andExpect(header().string(
                        "Access-Control-Allow-Origin",
                        "http://localhost:5173"
                ))
                .andExpect(header().string(
                        "Access-Control-Allow-Credentials",
                        "true"
                ))
                .andExpect(header().string(
                        "Access-Control-Allow-Headers",
                        allOf(
                                containsString("authorization"),
                                containsString("content-type"),
                                containsString("cache-control"),
                                containsString("pragma")
                        )
                ));
    }

    private String bearerToken(String email, String authority) {
        return "Bearer " + jwtService.generateToken(user(email, authority));
    }

    private UserDetails user(String email, String authority) {
        return User.withUsername(email)
                .password("not-used")
                .authorities(authority)
                .build();
    }
}
