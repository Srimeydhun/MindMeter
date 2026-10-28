package com.classroom.mindmeter.service;

import com.classroom.mindmeter.repository.QuizRepository;
import com.classroom.mindmeter.repository.SubjectRepository;
import com.classroom.mindmeter.repository.UserRepository;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpStatus;
import org.springframework.web.server.ResponseStatusException;

import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class QuizServiceTest {

    private final QuizRepository quizRepository =
            mock(QuizRepository.class);
    private final QuizService quizService = new QuizService(
            quizRepository,
            mock(SubjectRepository.class),
            mock(UserRepository.class)
    );

    @Test
    void missingQuizReturnsNotFound() {
        when(quizRepository.findById(123L)).thenReturn(Optional.empty());

        ResponseStatusException exception = assertThrows(
                ResponseStatusException.class,
                () -> quizService.getQuizById(123L)
        );

        assertEquals(HttpStatus.NOT_FOUND, exception.getStatusCode());
        verify(quizRepository).findById(123L);
    }
}
