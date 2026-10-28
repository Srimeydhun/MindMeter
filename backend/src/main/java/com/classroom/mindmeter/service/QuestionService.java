package com.classroom.mindmeter.service;

import com.classroom.mindmeter.dto.QuestionRequest;
import com.classroom.mindmeter.dto.QuestionResponse;
import com.classroom.mindmeter.dto.StudentQuestionResponse;
import com.classroom.mindmeter.model.Question;
import com.classroom.mindmeter.model.Quiz;
import com.classroom.mindmeter.model.Subject;
import com.classroom.mindmeter.model.User;
import com.classroom.mindmeter.repository.QuestionRepository;
import com.classroom.mindmeter.repository.QuizRepository;
import com.classroom.mindmeter.repository.UserRepository;

import org.springframework.stereotype.Service;

import java.util.List;

@Service
public class QuestionService {

    private final QuestionRepository questionRepository;
    private final QuizRepository quizRepository;
    private final UserRepository userRepository;

    public QuestionService(
            QuestionRepository questionRepository,
            QuizRepository quizRepository,
            UserRepository userRepository) {

        this.questionRepository = questionRepository;
        this.quizRepository = quizRepository;
        this.userRepository = userRepository;
    }

    // ==========================================
    // TEACHER - CREATE QUESTION
    // ==========================================

    public QuestionResponse createQuestion(
            QuestionRequest request,
            String teacherEmail) {

        User teacher = getTeacherByEmail(teacherEmail);

        Quiz quiz = getQuiz(request.getQuizId());

        verifyQuizOwnership(quiz, teacher);

        Question question = new Question();

        question.setQuestionText(
                request.getQuestionText().trim()
        );

        question.setOptionA(
                request.getOptionA().trim()
        );

        question.setOptionB(
                request.getOptionB().trim()
        );

        question.setOptionC(
                request.getOptionC().trim()
        );

        question.setOptionD(
                request.getOptionD().trim()
        );

        question.setCorrectAnswer(
                request.getCorrectAnswer().trim()
        );

        question.setMarks(
                request.getMarks()
        );

        question.setQuiz(quiz);

        Question savedQuestion =
                questionRepository.save(question);

        return convertToTeacherResponse(savedQuestion);
    }

    // ==========================================
    // TEACHER - GET ALL QUESTIONS
    // ==========================================

    public List<QuestionResponse> getAllQuestions(
            String teacherEmail) {

        User teacher = getTeacherByEmail(teacherEmail);

        return questionRepository.findAll()
                .stream()
                .filter(question ->
                        isQuizOwnedByTeacher(
                                question.getQuiz(),
                                teacher
                        )
                )
                .map(this::convertToTeacherResponse)
                .toList();
    }

    // ==========================================
    // TEACHER - GET QUESTION BY ID
    // ==========================================

    public QuestionResponse getQuestionById(
            Long id,
            String teacherEmail) {

        User teacher = getTeacherByEmail(teacherEmail);

        Question question =
                questionRepository.findById(id)
                        .orElseThrow(() ->
                                new RuntimeException(
                                        "Question not found with ID: "
                                                + id
                                )
                        );

        verifyQuizOwnership(
                question.getQuiz(),
                teacher
        );

        return convertToTeacherResponse(question);
    }

    // ==========================================
    // TEACHER - GET QUESTIONS BY QUIZ
    // ==========================================

    public List<QuestionResponse> getQuestionsByQuiz(
            Long quizId,
            String teacherEmail) {

        User teacher = getTeacherByEmail(teacherEmail);

        Quiz quiz = getQuiz(quizId);

        verifyQuizOwnership(quiz, teacher);

        return questionRepository.findByQuizId(quizId)
                .stream()
                .map(this::convertToTeacherResponse)
                .toList();
    }

    // ==========================================
    // STUDENT - GET QUESTIONS BY QUIZ
    // ==========================================

    public List<StudentQuestionResponse>
    getStudentQuestionsByQuiz(Long quizId) {

        Quiz quiz = getQuiz(quizId);

        return questionRepository.findByQuizId(quiz.getId())
                .stream()
                .map(this::convertToStudentResponse)
                .toList();
    }

    // ==========================================
    // STUDENT - GET QUESTION BY ID
    // ==========================================

    public StudentQuestionResponse
    getStudentQuestionById(Long id) {

        Question question =
                questionRepository.findById(id)
                        .orElseThrow(() ->
                                new RuntimeException(
                                        "Question not found with ID: "
                                                + id
                                )
                        );

        return convertToStudentResponse(question);
    }

    // ==========================================
    // TEACHER - UPDATE QUESTION
    // ==========================================

    public QuestionResponse updateQuestion(
            Long id,
            QuestionRequest request,
            String teacherEmail) {

        User teacher = getTeacherByEmail(teacherEmail);

        Question question =
                questionRepository.findById(id)
                        .orElseThrow(() ->
                                new RuntimeException(
                                        "Question not found with ID: "
                                                + id
                                )
                        );

        verifyQuizOwnership(
                question.getQuiz(),
                teacher
        );

        Quiz quiz = getQuiz(request.getQuizId());

        verifyQuizOwnership(quiz, teacher);

        question.setQuestionText(
                request.getQuestionText().trim()
        );

        question.setOptionA(
                request.getOptionA().trim()
        );

        question.setOptionB(
                request.getOptionB().trim()
        );

        question.setOptionC(
                request.getOptionC().trim()
        );

        question.setOptionD(
                request.getOptionD().trim()
        );

        question.setCorrectAnswer(
                request.getCorrectAnswer().trim()
        );

        question.setMarks(
                request.getMarks()
        );

        question.setQuiz(quiz);

        Question updatedQuestion =
                questionRepository.save(question);

        return convertToTeacherResponse(
                updatedQuestion
        );
    }

    // ==========================================
    // TEACHER - DELETE QUESTION
    // ==========================================

    public void deleteQuestion(
            Long id,
            String teacherEmail) {

        User teacher = getTeacherByEmail(teacherEmail);

        Question question =
                questionRepository.findById(id)
                        .orElseThrow(() ->
                                new RuntimeException(
                                        "Question not found with ID: "
                                                + id
                                )
                        );

        verifyQuizOwnership(
                question.getQuiz(),
                teacher
        );

        questionRepository.delete(question);
    }

    // ==========================================
    // FIND QUIZ
    // ==========================================

    private Quiz getQuiz(Long quizId) {

        return quizRepository.findById(quizId)
                .orElseThrow(() ->
                        new RuntimeException(
                                "Quiz not found with ID: "
                                        + quizId
                        )
                );
    }

    // ==========================================
    // FIND TEACHER
    // ==========================================

    private User getTeacherByEmail(
            String teacherEmail) {

        User teacher =
                userRepository.findByEmail(
                        teacherEmail
                ).orElseThrow(() ->
                        new RuntimeException(
                                "Teacher account not found"
                        )
                );

        if (!"TEACHER".equalsIgnoreCase(
                teacher.getRole())) {

            throw new RuntimeException(
                    "Only teachers can manage questions"
            );
        }

        return teacher;
    }

    // ==========================================
    // VERIFY QUIZ OWNERSHIP
    // ==========================================

    private void verifyQuizOwnership(
            Quiz quiz,
            User teacher) {

        if (quiz.getSubject() == null) {
            throw new RuntimeException(
                    "Quiz is not assigned to a course"
            );
        }

        Subject subject = quiz.getSubject();

        if (subject.getTeacher() == null) {
            throw new RuntimeException(
                    "This course is not assigned to a teacher"
            );
        }

        if (!subject.getTeacher()
                .getId()
                .equals(teacher.getId())) {

            throw new RuntimeException(
                    "You do not have permission to manage questions for this quiz"
            );
        }
    }

    // ==========================================
    // CHECK QUIZ OWNERSHIP
    // ==========================================

    private boolean isQuizOwnedByTeacher(
            Quiz quiz,
            User teacher) {

        if (quiz == null
                || quiz.getSubject() == null
                || quiz.getSubject().getTeacher() == null) {

            return false;
        }

        return quiz.getSubject()
                .getTeacher()
                .getId()
                .equals(teacher.getId());
    }

    // ==========================================
    // TEACHER RESPONSE
    // ==========================================

    private QuestionResponse
    convertToTeacherResponse(
            Question question) {

        return new QuestionResponse(
                question.getId(),
                question.getQuestionText(),
                question.getOptionA(),
                question.getOptionB(),
                question.getOptionC(),
                question.getOptionD(),
                question.getCorrectAnswer(),
                question.getMarks(),
                question.getQuiz().getId(),
                question.getQuiz().getTitle()
        );
    }

    // ==========================================
    // STUDENT RESPONSE
    // ==========================================

    private StudentQuestionResponse
    convertToStudentResponse(
            Question question) {

        return new StudentQuestionResponse(
                question.getId(),
                question.getQuestionText(),
                question.getOptionA(),
                question.getOptionB(),
                question.getOptionC(),
                question.getOptionD(),
                question.getMarks(),
                question.getQuiz().getId(),
                question.getQuiz().getTitle()
        );
    }
}