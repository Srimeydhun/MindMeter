import { useEffect, useRef, useState } from 'react';
import { useNavigate } from 'react-router-dom';
import api from '../api/axiosConfig';
import { clearAuthSession, getAuthValue } from '../api/authStorage';
import BrandLogo from '../components/BrandLogo.jsx';
import ThemeToggle from '../theme/ThemeToggle.jsx';
import './TeacherDashboard.css';

function formatSubmittedAt(value) {
  if (typeof value !== 'string' || !value.trim()) {
    return '—';
  }

  const timestamp = /(?:Z|[+-]\d{2}:\d{2})$/i.test(value)
    ? value
    : `${value}+05:30`;
  const date = new Date(timestamp);

  if (Number.isNaN(date.getTime())) {
    return '—';
  }

  return new Intl.DateTimeFormat('en-IN', {
    day: '2-digit',
    month: 'short',
    year: 'numeric',
    hour: '2-digit',
    minute: '2-digit',
    hour12: true,
    timeZone: 'Asia/Kolkata',
  })
    .format(date)
    .replace(/\b(am|pm)\b/g, (period) => period.toUpperCase());
}

function getSubmissionRequestError(error, endpoint, action) {
  const response = error?.response;

  if (response) {
    const payload = response.data;
    const details =
      typeof payload === 'string'
        ? payload
        : payload?.detail || payload?.message || payload?.error;
    const statusText = response.statusText
      ? ` ${response.statusText}`
      : '';
    const backendDetails =
      typeof details === 'string' && details.trim()
        ? `: ${details.trim()}`
        : '';

    return `${action} failed (HTTP ${response.status}${statusText}, GET ${endpoint})${backendDetails}`;
  }

  if (error?.request) {
    return `${action} failed (GET ${endpoint}; no HTTP response): ${
      error.message || 'The server could not be reached.'
    }`;
  }

  return `${action} failed (GET ${endpoint}): ${
    error?.message || 'The response could not be processed.'
  }`;
}

function TeacherDashboard() {
  const navigate = useNavigate();

  const [students, setStudents] = useState([]);
  const [subjects, setSubjects] = useState([]);
  const [attempts, setAttempts] = useState([]);
  const [selectedSubmissionQuiz, setSelectedSubmissionQuiz] =
    useState(null);
  const [quizSubmissions, setQuizSubmissions] = useState([]);
  const [loadingSubmissions, setLoadingSubmissions] = useState(false);
  const [refreshingSubmissions, setRefreshingSubmissions] = useState(false);
  const [submissionError, setSubmissionError] = useState('');
  const submissionsRequestId = useRef(0);

  const [loading, setLoading] = useState(true);
  const [error, setError] = useState('');

  const sortedStudents = [...students].sort((studentA, studentB) => {
    const nameA = studentA?.name?.trim() || '';
    const nameB = studentB?.name?.trim() || '';

    if (!nameA) {
      return nameB ? 1 : 0;
    }

    if (!nameB) {
      return -1;
    }

    return nameA.localeCompare(nameB, undefined, {
      sensitivity: 'base',
    });
  });

  const sortedSubmissions = quizSubmissions
    .filter((attempt) => attempt?.status === 'Submitted')
    .sort((attemptA, attemptB) => {
      const nameOrder = (attemptA.studentName || '').localeCompare(
        attemptB.studentName || '',
        undefined,
        { sensitivity: 'base' }
      );

      if (nameOrder !== 0) {
        return nameOrder;
      }

      return (attemptA.registerNumber || '').localeCompare(
        attemptB.registerNumber || '',
        undefined,
        { sensitivity: 'base' }
      );
    });

  // =========================================================
  // COURSE STATES
  // =========================================================

  const [courseName, setCourseName] = useState('');
  const [courseDescription, setCourseDescription] = useState('');
  const [addingCourse, setAddingCourse] = useState(false);

  const [editingSubject, setEditingSubject] = useState(null);
  const [editName, setEditName] = useState('');
  const [editDescription, setEditDescription] = useState('');
  const [updatingCourse, setUpdatingCourse] = useState(false);

  const [deletingCourseId, setDeletingCourseId] = useState(null);

  // =========================================================
  // QUIZ STATES
  // =========================================================

  const [quizzesBySubject, setQuizzesBySubject] = useState({});
  const [expandedSubjectId, setExpandedSubjectId] = useState(null);

  const [quizTitle, setQuizTitle] = useState('');
  const [quizDescription, setQuizDescription] = useState('');
  const [quizDuration, setQuizDuration] = useState(30);
  const [quizMaxAttempts, setQuizMaxAttempts] = useState(1);

  const [addingQuiz, setAddingQuiz] = useState(false);
  const [editingQuiz, setEditingQuiz] = useState(null);
  const [updatingQuiz, setUpdatingQuiz] = useState(false);
  const [deletingQuizId, setDeletingQuizId] = useState(null);

  // =========================================================
  // QUESTION STATES
  // =========================================================

  const [questionsByQuiz, setQuestionsByQuiz] = useState({});
  const [expandedQuizId, setExpandedQuizId] = useState(null);
  const [questionText, setQuestionText] = useState('');
  const [optionA, setOptionA] = useState('');
  const [optionB, setOptionB] = useState('');
  const [optionC, setOptionC] = useState('');
  const [optionD, setOptionD] = useState('');
  const [correctAnswer, setCorrectAnswer] = useState('A');
  const [questionMarks, setQuestionMarks] = useState(1);
  const [addingQuestion, setAddingQuestion] = useState(false);
  const [editingQuestion, setEditingQuestion] = useState(null);
  const [updatingQuestion, setUpdatingQuestion] = useState(false);
  const [deletingQuestionId, setDeletingQuestionId] = useState(null);

  const teacherName =
    getAuthValue('mindmeter_user_name') || 'Teacher';

  // =========================================================
  // AUTH CHECK
  // =========================================================

  useEffect(() => {
    const token = getAuthValue('mindmeter_token');
    const role = getAuthValue('mindmeter_role');

    if (!token || role !== 'TEACHER') {
      navigate('/login', { replace: true });
      return;
    }

    loadDashboard();
  }, [navigate]);

  // =========================================================
  // LOAD DASHBOARD
  // =========================================================

  async function loadDashboard() {
    setLoading(true);
    setError('');

    const results = await Promise.allSettled([
      api.get('/teacher/students'),
      api.get('/subjects/my'),
      api.get('/attempts/all'),
    ]);

    // ---------------------------------------------------------
    // STUDENTS
    // ---------------------------------------------------------

    if (results[0].status === 'fulfilled') {
      const data = results[0].value.data;
      setStudents(Array.isArray(data) ? data : []);
    }

    // ---------------------------------------------------------
    // SUBJECTS + QUIZZES
    // ---------------------------------------------------------

    if (results[1].status === 'fulfilled') {
      const data = results[1].value.data;
      const teacherSubjects = Array.isArray(data) ? data : [];

      setSubjects(teacherSubjects);

      // Load quizzes belonging to each teacher course
      const quizResults = await Promise.allSettled(
        teacherSubjects.map((subject) =>
          api.get(`/quizzes/subject/${subject.id}`)
        )
      );

      const quizMap = {};

      teacherSubjects.forEach((subject, index) => {
        const result = quizResults[index];

        if (result?.status === 'fulfilled') {
          const quizData = result.value.data;

          quizMap[subject.id] = Array.isArray(quizData)
            ? quizData
            : [];
        } else {
          quizMap[subject.id] = [];
        }
      });

      setQuizzesBySubject(quizMap);
    }

    // ---------------------------------------------------------
    // ATTEMPTS
    // ---------------------------------------------------------

    if (results[2].status === 'fulfilled') {
      const data = results[2].value.data;
      setAttempts(Array.isArray(data) ? data : []);
    }

    // ---------------------------------------------------------
    // ERROR HANDLING
    // ---------------------------------------------------------

    const failed = results.filter(
      (result) => result.status === 'rejected'
    );

    if (failed.length > 0) {
      setError(
        'Some dashboard information could not load. Please try refreshing.'
      );
    }

    setLoading(false);
  }

  async function openSubmittedList(quiz, subject) {
    const endpoint = `/attempts/quiz/${quiz.id}/submitted`;
    const requestId = ++submissionsRequestId.current;
    setSelectedSubmissionQuiz({
      id: quiz.id,
      title: quiz.title,
      subjectName: subject.name,
    });
    setQuizSubmissions([]);
    setSubmissionError('');
    setLoadingSubmissions(true);
    setRefreshingSubmissions(false);

    try {
      const response = await api.get(endpoint);

      if (!Array.isArray(response.data)) {
        throw new Error('Expected an array of submitted attempts.');
      }

      if (requestId === submissionsRequestId.current) {
        setQuizSubmissions(response.data);
      }
    } catch (error) {
      console.error('Failed to load quiz submissions.', {
        endpoint,
        status: error?.response?.status,
        response: error?.response?.data,
        error,
      });
      if (requestId === submissionsRequestId.current) {
        setSubmissionError(
          getSubmissionRequestError(
            error,
            endpoint,
            'Loading submitted attempts'
          )
        );
      }
    } finally {
      if (requestId === submissionsRequestId.current) {
        setLoadingSubmissions(false);
      }
    }
  }

  async function refreshSubmittedList() {
    if (!selectedSubmissionQuiz || refreshingSubmissions) {
      return;
    }

    const endpoint =
      `/attempts/quiz/${selectedSubmissionQuiz.id}/submitted`;
    const requestId = ++submissionsRequestId.current;
    setSubmissionError('');
    setRefreshingSubmissions(true);

    try {
      const response = await api.get(endpoint);

      if (!Array.isArray(response.data)) {
        throw new Error('Expected an array of submitted attempts.');
      }

      if (requestId === submissionsRequestId.current) {
        setQuizSubmissions(response.data);
      }
    } catch (error) {
      console.error('Failed to refresh quiz submissions.', {
        endpoint,
        status: error?.response?.status,
        response: error?.response?.data,
        error,
      });
      if (requestId === submissionsRequestId.current) {
        setSubmissionError(
          getSubmissionRequestError(
            error,
            endpoint,
            'Refreshing submitted attempts'
          )
        );
      }
    } finally {
      if (requestId === submissionsRequestId.current) {
        setRefreshingSubmissions(false);
      }
    }
  }

  function closeSubmittedList() {
    submissionsRequestId.current += 1;
    setSelectedSubmissionQuiz(null);
    setQuizSubmissions([]);
    setSubmissionError('');
    setLoadingSubmissions(false);
    setRefreshingSubmissions(false);
  }

  // =========================================================
  // ADD COURSE
  // =========================================================

  async function handleAddCourse(event) {
    event.preventDefault();

    setError('');

    const trimmedName = courseName.trim();
    const trimmedDescription = courseDescription.trim();

    if (!trimmedName) {
      setError('Course name is required.');
      return;
    }

    try {
      setAddingCourse(true);

      const response = await api.post('/subjects', {
        name: trimmedName,
        description: trimmedDescription,
      });

      const newSubject = response.data;

      setSubjects((currentSubjects) => [
        ...currentSubjects,
        newSubject,
      ]);

      // New course starts with no quizzes
      setQuizzesBySubject((current) => ({
        ...current,
        [newSubject.id]: [],
      }));

      setCourseName('');
      setCourseDescription('');
    } catch (err) {
      const message =
        err.response?.data?.message ||
        'Unable to add the course. Please try again.';

      setError(message);
    } finally {
      setAddingCourse(false);
    }
  }

  // =========================================================
  // START COURSE EDIT
  // =========================================================

  function startEdit(subject) {
    setEditingSubject(subject);
    setEditName(subject.name || '');
    setEditDescription(subject.description || '');
    setError('');

    window.scrollTo({
      top: 0,
      behavior: 'smooth',
    });
  }

  // =========================================================
  // CANCEL COURSE EDIT
  // =========================================================

  function cancelEdit() {
    setEditingSubject(null);
    setEditName('');
    setEditDescription('');
  }

  // =========================================================
  // UPDATE COURSE
  // =========================================================

  async function handleUpdateCourse(event) {
    event.preventDefault();

    if (!editingSubject) {
      return;
    }

    const trimmedName = editName.trim();
    const trimmedDescription = editDescription.trim();

    if (!trimmedName) {
      setError('Course name is required.');
      return;
    }

    try {
      setUpdatingCourse(true);
      setError('');

      const response = await api.put(
        `/subjects/${editingSubject.id}`,
        {
          name: trimmedName,
          description: trimmedDescription,
        }
      );

      const updatedSubject = response.data;

      setSubjects((currentSubjects) =>
        currentSubjects.map((subject) =>
          subject.id === updatedSubject.id
            ? updatedSubject
            : subject
        )
      );

      cancelEdit();
    } catch (err) {
      const message =
        err.response?.data?.message ||
        'Unable to update the course. Please try again.';

      setError(message);
    } finally {
      setUpdatingCourse(false);
    }
  }

  // =========================================================
  // DELETE COURSE
  // =========================================================

  async function handleDeleteCourse(subjectId) {
    const subject = subjects.find(
      (item) => item.id === subjectId
    );

    const subjectName =
      subject?.name || 'this course';

    const confirmed = window.confirm(
      `Are you sure you want to delete "${subjectName}"?`
    );

    if (!confirmed) {
      return;
    }

    try {
      setDeletingCourseId(subjectId);
      setError('');

      await api.delete(`/subjects/${subjectId}`);

      setSubjects((currentSubjects) =>
        currentSubjects.filter(
          (subject) => subject.id !== subjectId
        )
      );

      setQuizzesBySubject((current) => {
        const updated = { ...current };
        delete updated[subjectId];
        return updated;
      });

      if (expandedSubjectId === subjectId) {
        setExpandedSubjectId(null);
      }

      if (editingSubject?.id === subjectId) {
        cancelEdit();
      }
    } catch (err) {
      const message =
        err.response?.data?.message ||
        'Unable to delete the course. Please try again.';

      setError(message);
    } finally {
      setDeletingCourseId(null);
    }
  }

  // =========================================================
  // QUIZ - RESET FORM
  // =========================================================

  function resetQuizForm() {
    setQuizTitle('');
    setQuizDescription('');
    setQuizDuration(30);
    setQuizMaxAttempts(1);
    setEditingQuiz(null);
  }

  // =========================================================
  // QUIZ - TOGGLE QUIZZES
  // =========================================================

  function toggleQuizzes(subjectId) {
    setExpandedSubjectId((currentId) =>
      currentId === subjectId ? null : subjectId
    );

    resetQuizForm();
    setError('');
  }

  // =========================================================
  // QUIZ - CREATE
  // =========================================================

  async function handleAddQuiz(subjectId) {
    setError('');

    const trimmedTitle = quizTitle.trim();
    const trimmedDescription = quizDescription.trim();

    const duration = Number(quizDuration);
    const maxAttempts = Number(quizMaxAttempts);

    if (!trimmedTitle) {
      setError('Quiz title is required.');
      return;
    }

    if (!Number.isInteger(duration) || duration < 1) {
      setError('Duration must be at least 1 minute.');
      return;
    }

    if (!Number.isInteger(maxAttempts) || maxAttempts < 1) {
      setError('Maximum attempts must be at least 1.');
      return;
    }

    try {
      setAddingQuiz(true);

      const response = await api.post('/quizzes', {
        title: trimmedTitle,
        description: trimmedDescription,
        durationMinutes: duration,
        maxAttempts: maxAttempts,
        subjectId: subjectId,
      });

      const newQuiz = response.data;

      setQuizzesBySubject((current) => ({
        ...current,
        [subjectId]: [
          ...(current[subjectId] || []),
          newQuiz,
        ],
      }));

      resetQuizForm();
    } catch (err) {
      const message =
        err.response?.data?.message ||
        'Unable to create the quiz. Please try again.';

      setError(message);
    } finally {
      setAddingQuiz(false);
    }
  }

  // =========================================================
  // QUIZ - START EDIT
  // =========================================================

  function startEditQuiz(quiz) {
    setEditingQuiz(quiz);

    setQuizTitle(quiz.title || '');
    setQuizDescription(quiz.description || '');
    setQuizDuration(quiz.durationMinutes || 30);
    setQuizMaxAttempts(quiz.maxAttempts || 1);

    setError('');
  }

  // =========================================================
  // QUIZ - CANCEL EDIT
  // =========================================================

  function cancelEditQuiz() {
    resetQuizForm();
  }

  // =========================================================
  // QUIZ - UPDATE
  // =========================================================

  async function handleUpdateQuiz(subjectId) {
    if (!editingQuiz) {
      return;
    }

    const trimmedTitle = quizTitle.trim();
    const trimmedDescription = quizDescription.trim();

    const duration = Number(quizDuration);
    const maxAttempts = Number(quizMaxAttempts);

    if (!trimmedTitle) {
      setError('Quiz title is required.');
      return;
    }

    if (!Number.isInteger(duration) || duration < 1) {
      setError('Duration must be at least 1 minute.');
      return;
    }

    if (!Number.isInteger(maxAttempts) || maxAttempts < 1) {
      setError('Maximum attempts must be at least 1.');
      return;
    }

    try {
      setUpdatingQuiz(true);
      setError('');

      const response = await api.put(
        `/quizzes/${editingQuiz.id}`,
        {
          title: trimmedTitle,
          description: trimmedDescription,
          durationMinutes: duration,
          maxAttempts: maxAttempts,
          subjectId: subjectId,
        }
      );

      const updatedQuiz = response.data;

      setQuizzesBySubject((current) => ({
        ...current,
        [subjectId]: (current[subjectId] || []).map(
          (quiz) =>
            quiz.id === updatedQuiz.id
              ? updatedQuiz
              : quiz
        ),
      }));

      resetQuizForm();
    } catch (err) {
      const message =
        err.response?.data?.message ||
        'Unable to update the quiz. Please try again.';

      setError(message);
    } finally {
      setUpdatingQuiz(false);
    }
  }

  // =========================================================
  // QUIZ - DELETE
  // =========================================================

  async function handleDeleteQuiz(
    subjectId,
    quizId
  ) {
    const quiz = (
      quizzesBySubject[subjectId] || []
    ).find((item) => item.id === quizId);

    const confirmed = window.confirm(
      `Are you sure you want to delete "${quiz?.title || 'this quiz'}"?`
    );

    if (!confirmed) {
      return;
    }

    try {
      setDeletingQuizId(quizId);
      setError('');

      await api.delete(`/quizzes/${quizId}`);

      setQuizzesBySubject((current) => ({
        ...current,
        [subjectId]: (
          current[subjectId] || []
        ).filter(
          (item) => item.id !== quizId
        ),
      }));

      if (editingQuiz?.id === quizId) {
        resetQuizForm();
      }

      if (expandedQuizId === quizId) {
        setExpandedQuizId(null);
        resetQuestionForm();
      }

      setQuestionsByQuiz((current) => {
        const updated = { ...current };
        delete updated[quizId];
        return updated;
      });
    } catch (err) {
      const message =
        err.response?.data?.message ||
        'Unable to delete the quiz. Please try again.';

      setError(message);
    } finally {
      setDeletingQuizId(null);
    }
  }

  // =========================================================
  // QUESTION MANAGEMENT
  // =========================================================

  function resetQuestionForm() {
    setQuestionText('');
    setOptionA('');
    setOptionB('');
    setOptionC('');
    setOptionD('');
    setCorrectAnswer('A');
    setQuestionMarks(1);
    setEditingQuestion(null);
  }

  async function toggleQuestions(quizId) {
    if (expandedQuizId === quizId) {
      setExpandedQuizId(null);
      resetQuestionForm();
      return;
    }

    try {
      setError('');
      setExpandedQuizId(quizId);
      resetQuestionForm();

      const response = await api.get(`/questions/quiz/${quizId}`);

      setQuestionsByQuiz((current) => ({
        ...current,
        [quizId]: Array.isArray(response.data) ? response.data : [],
      }));
    } catch (err) {
      setError(
        err.response?.data?.message ||
        'Unable to load questions. Please try again.'
      );
    }
  }

  async function handleAddQuestion(quizId) {
    setError('');

    const data = {
      questionText: questionText.trim(),
      optionA: optionA.trim(),
      optionB: optionB.trim(),
      optionC: optionC.trim(),
      optionD: optionD.trim(),
      correctAnswer,
      marks: Number(questionMarks),
      quizId,
    };

    if (!data.questionText) {
      setError('Question text is required.');
      return;
    }

    if (!data.optionA || !data.optionB || !data.optionC || !data.optionD) {
      setError('All four options are required.');
      return;
    }

    if (!['A', 'B', 'C', 'D'].includes(data.correctAnswer)) {
      setError('Please select a valid correct answer.');
      return;
    }

    if (!Number.isInteger(data.marks) || data.marks < 1) {
      setError('Marks must be at least 1.');
      return;
    }

    try {
      setAddingQuestion(true);

      const response = await api.post('/questions', data);

      setQuestionsByQuiz((current) => ({
        ...current,
        [quizId]: [
          ...(current[quizId] || []),
          response.data,
        ],
      }));

      resetQuestionForm();
    } catch (err) {
      setError(
        err.response?.data?.message ||
        'Unable to add the question. Please try again.'
      );
    } finally {
      setAddingQuestion(false);
    }
  }

  function startEditQuestion(question) {
    setEditingQuestion(question);
    setQuestionText(question.questionText || '');
    setOptionA(question.optionA || '');
    setOptionB(question.optionB || '');
    setOptionC(question.optionC || '');
    setOptionD(question.optionD || '');
    setCorrectAnswer(question.correctAnswer || 'A');
    setQuestionMarks(question.marks || 1);
    setError('');
  }

  async function handleUpdateQuestion(quizId) {
    if (!editingQuestion) {
      return;
    }

    setError('');

    const data = {
      questionText: questionText.trim(),
      optionA: optionA.trim(),
      optionB: optionB.trim(),
      optionC: optionC.trim(),
      optionD: optionD.trim(),
      correctAnswer,
      marks: Number(questionMarks),
      quizId,
    };

    if (!data.questionText) {
      setError('Question text is required.');
      return;
    }

    if (!data.optionA || !data.optionB || !data.optionC || !data.optionD) {
      setError('All four options are required.');
      return;
    }

    if (!['A', 'B', 'C', 'D'].includes(data.correctAnswer)) {
      setError('Please select a valid correct answer.');
      return;
    }

    if (!Number.isInteger(data.marks) || data.marks < 1) {
      setError('Marks must be at least 1.');
      return;
    }

    try {
      setUpdatingQuestion(true);

      const response = await api.put(
        `/questions/${editingQuestion.id}`,
        data
      );

      setQuestionsByQuiz((current) => ({
        ...current,
        [quizId]: (current[quizId] || []).map((question) =>
          question.id === response.data.id
            ? response.data
            : question
        ),
      }));

      resetQuestionForm();
    } catch (err) {
      setError(
        err.response?.data?.message ||
        'Unable to update the question. Please try again.'
      );
    } finally {
      setUpdatingQuestion(false);
    }
  }

  async function handleDeleteQuestion(quizId, questionId) {
    const confirmed = window.confirm(
      'Are you sure you want to delete this question?'
    );

    if (!confirmed) {
      return;
    }

    try {
      setDeletingQuestionId(questionId);
      setError('');

      await api.delete(`/questions/${questionId}`);

      setQuestionsByQuiz((current) => ({
        ...current,
        [quizId]: (current[quizId] || []).filter(
          (question) => question.id !== questionId
        ),
      }));

      if (editingQuestion?.id === questionId) {
        resetQuestionForm();
      }
    } catch (err) {
      setError(
        err.response?.data?.message ||
        'Unable to delete the question. Please try again.'
      );
    } finally {
      setDeletingQuestionId(null);
    }
  }

  // =========================================================
  // LOGOUT
  // =========================================================

  function logout() {
    clearAuthSession();

    navigate('/login', { replace: true });
  }

  // =========================================================
  // RENDER
  // =========================================================

  return (
    <div className="teacher-dashboard">

      {/* =====================================================
          NAVBAR
          ===================================================== */}

      <header className="teacher-navbar">

        <BrandLogo className="teacher-brand" />

        <div className="teacher-nav-right">

          <span className="teacher-role">
            TEACHER
          </span>

          <button
            className="teacher-logout"
            onClick={logout}
          >
            Logout
          </button>
          <ThemeToggle />

        </div>

      </header>

      {/* =====================================================
          MAIN
          ===================================================== */}

      <main className="teacher-main">

        {/* ===================================================
            WELCOME
            =================================================== */}

        <section className="teacher-welcome">

          <div>

            <p className="teacher-eyebrow">
              TEACHER PORTAL
            </p>

            <h1>
              Welcome, {teacherName}!
            </h1>

            <p>
              Monitor students, review assessments,
              and manage learning activities.
            </p>

          </div>

          <div className="teacher-welcome-icon">
            📚
          </div>

        </section>

        {/* ===================================================
            ERROR
            =================================================== */}

        {error && (
          <div
            className="teacher-error"
            role="alert"
          >

            <span>
              {error}
            </span>

            <button onClick={loadDashboard}>
              Retry
            </button>

          </div>
        )}

        {/* ===================================================
            LOADING
            =================================================== */}

        {loading ? (

          <p className="teacher-message">
            Loading dashboard...
          </p>

        ) : (

          <>

            {/* ===============================================
                STATISTICS
                =============================================== */}

            <section className="teacher-stats">

              <article className="teacher-stat-card">

                <span className="teacher-stat-icon">
                  👨‍🎓
                </span>

                <p>
                  Total Students
                </p>

                <h2>
                  {students.length}
                </h2>

              </article>

              <article className="teacher-stat-card">

                <span className="teacher-stat-icon">
                  📚
                </span>

                <p>
                  My Courses
                </p>

                <h2>
                  {subjects.length}
                </h2>

              </article>

              <article className="teacher-stat-card">

                <span className="teacher-stat-icon">
                  📝
                </span>

                <p>
                  Quiz Attempts
                </p>

                <h2>
                  {attempts.length}
                </h2>

              </article>

            </section>

            {/* ===============================================
                ADD COURSE
                =============================================== */}

            <section className="teacher-panel add-course-panel">

              <div className="teacher-section-heading">

                <div>

                  <h2>
                    Add New Course
                  </h2>

                  <p>
                    Create a course for your students.
                  </p>

                </div>

              </div>

              <form
                className="course-form"
                onSubmit={handleAddCourse}
              >

                <div className="course-form-group">

                  <label htmlFor="courseName">
                    Course Name
                  </label>

                  <input
                    id="courseName"
                    type="text"
                    value={courseName}
                    onChange={(event) =>
                      setCourseName(
                        event.target.value
                      )
                    }
                    placeholder="Enter course name"
                    disabled={addingCourse}
                  />

                </div>

                <div className="course-form-group">

                  <label htmlFor="courseDescription">
                    Description
                  </label>

                  <textarea
                    id="courseDescription"
                    value={courseDescription}
                    onChange={(event) =>
                      setCourseDescription(
                        event.target.value
                      )
                    }
                    placeholder="Enter course description"
                    rows="4"
                    disabled={addingCourse}
                  />

                </div>

                <button
                  type="submit"
                  className="course-submit-button"
                  disabled={addingCourse}
                >
                  {addingCourse
                    ? 'Adding Course...'
                    : 'Add Course'}
                </button>

              </form>

            </section>

            {/* ===============================================
                EDIT COURSE
                =============================================== */}

            {editingSubject && (

              <section className="teacher-panel edit-course-panel">

                <div className="teacher-section-heading">

                  <div>

                    <h2>
                      Edit Course
                    </h2>

                    <p>
                      Update the selected course.
                    </p>

                  </div>

                  <button
                    type="button"
                    className="cancel-edit-button"
                    onClick={cancelEdit}
                  >
                    Cancel
                  </button>

                </div>

                <form
                  className="course-form"
                  onSubmit={handleUpdateCourse}
                >

                  <div className="course-form-group">

                    <label htmlFor="editCourseName">
                      Course Name
                    </label>

                    <input
                      id="editCourseName"
                      type="text"
                      value={editName}
                      onChange={(event) =>
                        setEditName(
                          event.target.value
                        )
                      }
                      placeholder="Enter course name"
                      disabled={updatingCourse}
                    />

                  </div>

                  <div className="course-form-group">

                    <label htmlFor="editCourseDescription">
                      Description
                    </label>

                    <textarea
                      id="editCourseDescription"
                      value={editDescription}
                      onChange={(event) =>
                        setEditDescription(
                          event.target.value
                        )
                      }
                      placeholder="Enter course description"
                      rows="4"
                      disabled={updatingCourse}
                    />

                  </div>

                  <button
                    type="submit"
                    className="course-submit-button"
                    disabled={updatingCourse}
                  >
                    {updatingCourse
                      ? 'Updating Course...'
                      : 'Update Course'}
                  </button>

                </form>

              </section>

            )}

            {/* ===============================================
                STUDENTS
                =============================================== */}

            <section className="teacher-panel">

              <div className="teacher-section-heading">

                <div>

                  <h2>
                    Students
                  </h2>

                  <p>
                    Registered student accounts
                  </p>

                </div>

                <button onClick={loadDashboard}>
                  Refresh
                </button>

              </div>

              {students.length === 0 ? (

                <p className="teacher-empty">
                  No student records returned by the API.
                </p>

              ) : (

                <div className="teacher-table-wrapper">

                  <table className="teacher-table">

                    <thead>

                      <tr>

                        <th>
                          Name
                        </th>

                        <th>
                          Email
                        </th>

                        <th>
                          Register Number
                        </th>

                      </tr>

                    </thead>

                    <tbody>

                      {sortedStudents.map(
                        (student, index) => (

                          <tr
                            key={
                              student?.id ?? index
                            }
                          >

                            <td>
                              {student?.name?.trim() || '—'}
                            </td>

                            <td>
                              {student?.email || '—'}
                            </td>

                            <td>
                              {student?.registerNumber || '—'}
                            </td>

                          </tr>

                        )
                      )}

                    </tbody>

                  </table>

                </div>

              )}

            </section>

            {/* ===============================================
                MY COURSES
                =============================================== */}

            <section className="teacher-panel">

              <div className="teacher-section-heading">

                <div>

                  <h2>
                    {selectedSubmissionQuiz
                      ? `Submitted List - ${selectedSubmissionQuiz.title}`
                      : 'My Courses'}
                  </h2>

                  <p>
                    {selectedSubmissionQuiz
                      ? `Submissions for ${selectedSubmissionQuiz.subjectName}`
                      : 'Courses created by you'}
                  </p>

                </div>

                {selectedSubmissionQuiz && (
                  <div className="submitted-list-actions">
                    <button
                      type="button"
                      className="submitted-list-refresh"
                      onClick={refreshSubmittedList}
                      disabled={loadingSubmissions || refreshingSubmissions}
                      aria-live="polite"
                    >
                      <svg
                        className={refreshingSubmissions ? 'is-refreshing' : ''}
                        viewBox="0 0 24 24"
                        aria-hidden="true"
                      >
                        <path
                          d="M20 7v5h-5M4 17v-5h5m10-1a7 7 0 0 0-12.1-4.8L4 8m16 8-2.9 2.8A7 7 0 0 1 5 13"
                          fill="none"
                          stroke="currentColor"
                          strokeLinecap="round"
                          strokeLinejoin="round"
                          strokeWidth="2"
                        />
                      </svg>
                      {refreshingSubmissions ? 'Refreshing...' : 'Refresh'}
                    </button>
                    <button
                      type="button"
                      onClick={closeSubmittedList}
                    >
                      Back to Quizzes
                    </button>
                  </div>
                )}

              </div>

              {selectedSubmissionQuiz ? (

                loadingSubmissions ? (
                  <p className="teacher-empty">
                    Loading submitted attempts...
                  </p>
                ) : submissionError && sortedSubmissions.length === 0 ? (
                  <p className="teacher-error" role="alert">
                    {submissionError}
                  </p>
                ) : sortedSubmissions.length === 0 ? (
                  <p className="teacher-empty">
                    No submitted attempts for this quiz yet.
                  </p>
                ) : (
                  <>
                    {submissionError && (
                      <p className="teacher-error" role="alert">
                        {submissionError}
                      </p>
                    )}
                    <div className="teacher-table-wrapper">
                      <table className="teacher-table submission-table">
                        <thead>
                          <tr>
                            <th>Student Name</th>
                            <th>Register Number</th>
                            <th>Quiz Title / Topic</th>
                            <th>Subject</th>
                            <th>Submitted Date and Time</th>
                            <th>Marks Obtained</th>
                            <th>Total Marks</th>
                            <th>Status</th>
                          </tr>
                        </thead>
                        <tbody>
                          {sortedSubmissions.map((attempt, index) => (
                            <tr key={index}>
                              <td>{attempt.studentName || '—'}</td>
                              <td>{attempt.registerNumber || '—'}</td>
                              <td>{attempt.quizTitle || '—'}</td>
                              <td>{attempt.subjectName || '—'}</td>
                              <td>
                                {formatSubmittedAt(
                                  attempt.submittedAt ?? attempt.endTime
                                )}
                              </td>
                              <td>{attempt.marksObtained ?? '—'}</td>
                              <td>{attempt.totalMarks ?? '—'}</td>
                              <td>
                                {attempt.status || '—'}
                              </td>
                            </tr>
                          ))}
                        </tbody>
                      </table>
                    </div>
                  </>
                )

              ) : subjects.length === 0 ? (

                <div className="teacher-empty-course">

                  <div className="empty-course-icon">
                    📚
                  </div>

                  <h3>
                    No courses yet
                  </h3>

                  <p>
                    Use the Add New Course section
                    above to create your first course.
                  </p>

                </div>

              ) : (

                <div className="teacher-subject-grid">

                  {subjects.map((subject) => (

                    <article
                      className="teacher-subject-card"
                      key={subject.id}
                    >

                      <span className="subject-card-icon">
                        📘
                      </span>

                      <h3>
                        {subject.name}
                      </h3>

                      <p>
                        {subject.description ||
                          'No description provided.'}
                      </p>

                      {/* COURSE ACTIONS */}

                      <div className="subject-card-actions">

                        <button
                          className="manage-quiz-button"
                          onClick={() =>
                            toggleQuizzes(
                              subject.id
                            )
                          }
                        >
                          {expandedSubjectId ===
                          subject.id
                            ? 'Hide Quizzes'
                            : 'Manage Quizzes'}
                        </button>

                        <button
                          className="edit-course-button"
                          onClick={() =>
                            startEdit(subject)
                          }
                        >
                          Edit
                        </button>

                        <button
                          type="button"
                          className="delete-course-button"
                          onClick={() =>
                            handleDeleteCourse(
                              subject.id
                            )
                          }
                          disabled={
                            deletingCourseId ===
                            subject.id
                          }
                        >
                          {deletingCourseId ===
                          subject.id
                            ? 'Deleting...'
                            : 'Delete'}
                        </button>

                      </div>

                      {/* =====================================
                          QUIZ MANAGEMENT
                          ===================================== */}

                      {expandedSubjectId ===
                        subject.id && (

                        <div className="quiz-management">

                          <div className="quiz-management-header">

                            <div>

                              <h4>
                                Quizzes
                              </h4>

                              <p>
                                Quizzes for{' '}
                                {subject.name}
                              </p>

                            </div>

                          </div>

                          {/* QUIZ FORM */}

                          <div className="quiz-form">

                            <h4>
                              {editingQuiz
                                ? 'Edit Quiz'
                                : 'Create New Quiz'}
                            </h4>

                            <div className="quiz-form-group">

                              <label>
                                Quiz Title
                              </label>

                              <input
                                type="text"
                                value={quizTitle}
                                onChange={(event) =>
                                  setQuizTitle(
                                    event.target.value
                                  )
                                }
                                placeholder="Enter quiz title"
                                disabled={
                                  addingQuiz ||
                                  updatingQuiz
                                }
                              />

                            </div>

                            <div className="quiz-form-group">

                              <label>
                                Description
                              </label>

                              <textarea
                                value={quizDescription}
                                onChange={(event) =>
                                  setQuizDescription(
                                    event.target.value
                                  )
                                }
                                placeholder="Enter quiz description"
                                rows="3"
                                disabled={
                                  addingQuiz ||
                                  updatingQuiz
                                }
                              />

                            </div>

                            <div className="quiz-form-row">

                              <div className="quiz-form-group">

                                <label>
                                  Duration (minutes)
                                </label>

                                <input
                                  type="number"
                                  min="1"
                                  value={quizDuration}
                                  onChange={(event) =>
                                    setQuizDuration(
                                      event.target.value
                                    )
                                  }
                                  disabled={
                                    addingQuiz ||
                                    updatingQuiz
                                  }
                                />

                              </div>

                              <div className="quiz-form-group">

                                <label>
                                  Maximum Attempts
                                </label>

                                <input
                                  type="number"
                                  min="1"
                                  value={quizMaxAttempts}
                                  onChange={(event) =>
                                    setQuizMaxAttempts(
                                      event.target.value
                                    )
                                  }
                                  disabled={
                                    addingQuiz ||
                                    updatingQuiz
                                  }
                                />

                              </div>

                            </div>

                            <div className="quiz-form-actions">

                              <button
                                type="button"
                                className="quiz-save-button"
                                onClick={() =>
                                  editingQuiz
                                    ? handleUpdateQuiz(
                                        subject.id
                                      )
                                    : handleAddQuiz(
                                        subject.id
                                      )
                                }
                                disabled={
                                  addingQuiz ||
                                  updatingQuiz
                                }
                              >
                                {editingQuiz
                                  ? updatingQuiz
                                    ? 'Updating...'
                                    : 'Update Quiz'
                                  : addingQuiz
                                    ? 'Creating...'
                                    : 'Create Quiz'}
                              </button>

                              {editingQuiz && (

                                <button
                                  type="button"
                                  className="quiz-cancel-button"
                                  onClick={
                                    cancelEditQuiz
                                  }
                                  disabled={
                                    updatingQuiz
                                  }
                                >
                                  Cancel
                                </button>

                              )}

                            </div>

                          </div>

                          {/* QUIZ LIST */}

                          <div className="quiz-list">

                            {(quizzesBySubject[
                              subject.id
                            ] || []).length === 0 ? (

                              <div className="quiz-empty">

                                <span>
                                  📝
                                </span>

                                <p>
                                  No quizzes created yet.
                                </p>

                              </div>

                            ) : (

                              (
                                quizzesBySubject[
                                  subject.id
                                ] || []
                              ).map((quiz) => (

                                <div
                                  className={`quiz-card ${
                                    expandedQuizId === quiz.id
                                      ? 'quiz-card-expanded'
                                      : ''
                                  }`}
                                  key={quiz.id}
                                >

                                  <div className="quiz-card-info">

                                    <h4>
                                      {quiz.title}
                                    </h4>

                                    <p>
                                      {quiz.description ||
                                        'No description provided.'}
                                    </p>

                                    <div className="quiz-meta">

                                      <span>
                                        ⏱{' '}
                                        {
                                          quiz.durationMinutes
                                        }{' '}
                                        min
                                      </span>

                                      <span>
                                        🔁{' '}
                                        {
                                          quiz.maxAttempts
                                        }{' '}
                                        attempt
                                        {quiz.maxAttempts !==
                                        1
                                          ? 's'
                                          : ''}
                                      </span>

                                    </div>

                                  </div>

                                  <div className="quiz-card-actions">

                                    <button
                                      type="button"
                                      className="edit-quiz-button"
                                      onClick={() =>
                                        toggleQuestions(quiz.id)
                                      }
                                    >
                                      {expandedQuizId === quiz.id
                                        ? 'Hide Questions'
                                        : 'Manage Questions'}
                                    </button>

                                    <button
                                      type="button"
                                      className="edit-quiz-button"
                                      onClick={() =>
                                        openSubmittedList(quiz, subject)
                                      }
                                    >
                                      Submitted List
                                    </button>

                                    <button
                                      type="button"
                                      className="edit-quiz-button"
                                      onClick={() =>
                                        startEditQuiz(
                                          quiz
                                        )
                                      }
                                    >
                                      Edit
                                    </button>

                                    <button
                                      type="button"
                                      className="delete-quiz-button"
                                      onClick={() =>
                                        handleDeleteQuiz(
                                          subject.id,
                                          quiz.id
                                        )
                                      }
                                      disabled={
                                        deletingQuizId ===
                                        quiz.id
                                      }
                                    >
                                      {deletingQuizId ===
                                      quiz.id
                                        ? 'Deleting...'
                                        : 'Delete'}
                                    </button>

                                  </div>

                                  {expandedQuizId === quiz.id && (
                                    <div className="question-management quiz-management">
                                      <div className="quiz-management-header">
                                        <h4>Questions</h4>
                                        <p>Manage questions for {quiz.title}</p>
                                      </div>

                                      <div className="quiz-form">
                                        <h4>
                                          {editingQuestion
                                            ? 'Edit Question'
                                            : 'Add New Question'}
                                        </h4>

                                        <div className="quiz-form-group">
                                          <label htmlFor={`questionText-${quiz.id}`}>
                                            Question
                                          </label>
                                          <textarea
                                            id={`questionText-${quiz.id}`}
                                            value={questionText}
                                            onChange={(event) =>
                                              setQuestionText(event.target.value)
                                            }
                                            placeholder="Enter the question"
                                            rows="3"
                                            disabled={addingQuestion || updatingQuestion}
                                          />
                                        </div>

                                        <div className="quiz-form-group">
                                          <label htmlFor={`optionA-${quiz.id}`}>Option A</label>
                                          <input
                                            id={`optionA-${quiz.id}`}
                                            type="text"
                                            value={optionA}
                                            onChange={(event) => setOptionA(event.target.value)}
                                            placeholder="Enter option A"
                                            disabled={addingQuestion || updatingQuestion}
                                          />
                                        </div>

                                        <div className="quiz-form-group">
                                          <label htmlFor={`optionB-${quiz.id}`}>Option B</label>
                                          <input
                                            id={`optionB-${quiz.id}`}
                                            type="text"
                                            value={optionB}
                                            onChange={(event) => setOptionB(event.target.value)}
                                            placeholder="Enter option B"
                                            disabled={addingQuestion || updatingQuestion}
                                          />
                                        </div>

                                        <div className="quiz-form-group">
                                          <label htmlFor={`optionC-${quiz.id}`}>Option C</label>
                                          <input
                                            id={`optionC-${quiz.id}`}
                                            type="text"
                                            value={optionC}
                                            onChange={(event) => setOptionC(event.target.value)}
                                            placeholder="Enter option C"
                                            disabled={addingQuestion || updatingQuestion}
                                          />
                                        </div>

                                        <div className="quiz-form-group">
                                          <label htmlFor={`optionD-${quiz.id}`}>Option D</label>
                                          <input
                                            id={`optionD-${quiz.id}`}
                                            type="text"
                                            value={optionD}
                                            onChange={(event) => setOptionD(event.target.value)}
                                            placeholder="Enter option D"
                                            disabled={addingQuestion || updatingQuestion}
                                          />
                                        </div>

                                        <div className="quiz-form-row">
                                          <div className="quiz-form-group">
                                            <label htmlFor={`correctAnswer-${quiz.id}`}>
                                              Correct Answer
                                            </label>
                                            <select
                                              id={`correctAnswer-${quiz.id}`}
                                              value={correctAnswer}
                                              onChange={(event) =>
                                                setCorrectAnswer(event.target.value)
                                              }
                                              disabled={addingQuestion || updatingQuestion}
                                            >
                                              <option value="A">Option A</option>
                                              <option value="B">Option B</option>
                                              <option value="C">Option C</option>
                                              <option value="D">Option D</option>
                                            </select>
                                          </div>

                                          <div className="quiz-form-group">
                                            <label htmlFor={`questionMarks-${quiz.id}`}>
                                              Marks
                                            </label>
                                            <input
                                              id={`questionMarks-${quiz.id}`}
                                              type="number"
                                              min="1"
                                              step="1"
                                              value={questionMarks}
                                              onChange={(event) =>
                                                setQuestionMarks(event.target.value)
                                              }
                                              disabled={addingQuestion || updatingQuestion}
                                            />
                                          </div>
                                        </div>

                                        <div className="quiz-form-actions">
                                          <button
                                            type="button"
                                            className="quiz-save-button"
                                            onClick={() =>
                                              editingQuestion
                                                ? handleUpdateQuestion(quiz.id)
                                                : handleAddQuestion(quiz.id)
                                            }
                                            disabled={addingQuestion || updatingQuestion}
                                          >
                                            {editingQuestion
                                              ? updatingQuestion
                                                ? 'Updating...'
                                                : 'Update Question'
                                              : addingQuestion
                                                ? 'Adding...'
                                                : 'Add Question'}
                                          </button>

                                          {editingQuestion && (
                                            <button
                                              type="button"
                                              className="quiz-cancel-button"
                                              onClick={resetQuestionForm}
                                              disabled={updatingQuestion}
                                            >
                                              Cancel
                                            </button>
                                          )}
                                        </div>
                                      </div>

                                      <div className="quiz-list">
                                        {(questionsByQuiz[quiz.id] || []).length === 0 ? (
                                          <div className="quiz-empty">
                                            <span>❓</span>
                                            <p>No questions added yet.</p>
                                          </div>
                                        ) : (
                                          (questionsByQuiz[quiz.id] || []).map(
                                            (question, index) => (
                                              <div className="quiz-card" key={question.id}>
                                                <div className="quiz-card-info">
                                                  <h4>Question {index + 1}: {question.questionText}</h4>
                                                  <p><strong>A:</strong> {question.optionA}</p>
                                                  <p><strong>B:</strong> {question.optionB}</p>
                                                  <p><strong>C:</strong> {question.optionC}</p>
                                                  <p><strong>D:</strong> {question.optionD}</p>
                                                  <div className="quiz-meta">
                                                    <span>Correct: {question.correctAnswer}</span>
                                                    <span>Marks: {question.marks}</span>
                                                  </div>
                                                </div>

                                                <div className="quiz-card-actions">
                                                  <button
                                                    type="button"
                                                    className="edit-quiz-button"
                                                    onClick={() => startEditQuestion(question)}
                                                  >
                                                    Edit
                                                  </button>
                                                  <button
                                                    type="button"
                                                    className="delete-quiz-button"
                                                    onClick={() =>
                                                      handleDeleteQuestion(quiz.id, question.id)
                                                    }
                                                    disabled={deletingQuestionId === question.id}
                                                  >
                                                    {deletingQuestionId === question.id
                                                      ? 'Deleting...'
                                                      : 'Delete'}
                                                  </button>
                                                </div>
                                              </div>
                                            )
                                          )
                                        )}
                                      </div>
                                    </div>
                                  )}

                                </div>

                              ))

                            )}

                          </div>

                        </div>

                      )}

                    </article>

                  ))}

                </div>

              )}

            </section>

          </>

        )}

      </main>

      {/* =====================================================
          FOOTER
          ===================================================== */}

      <footer className="teacher-footer">
        MindMeter · Teacher Dashboard
      </footer>

    </div>
  );
}

export default TeacherDashboard;