import { useCallback, useEffect, useState } from 'react';
import { useNavigate } from 'react-router-dom';
import api from '../api/axiosConfig';
import { clearAuthSession, getAuthValue } from '../api/authStorage';
import BrandLogo from '../components/BrandLogo.jsx';
import ThemeToggle from '../theme/ThemeToggle.jsx';
import { formatPracticeDuration } from '../utils/practiceDuration.js';
import './StudentDashboard.css';

function StudentDashboard() {
  const navigate = useNavigate();

  const [dashboard, setDashboard] = useState(null);
  const [loading, setLoading] = useState(true);
  const [error, setError] = useState('');
  const [availableQuizzes, setAvailableQuizzes] = useState([]);
  const [quizzesLoading, setQuizzesLoading] = useState(true);
  const [quizzesError, setQuizzesError] = useState('');

  const studentName =
    getAuthValue('mindmeter_user_name') || 'Student';

  const loadAvailableQuizzes = useCallback(async () => {
    setQuizzesLoading(true);
    setQuizzesError('');

    try {
      const [quizzesResponse, attemptsResponse] = await Promise.all([
        api.get('/quizzes'),
        api.get('/attempts/my'),
      ]);

      const quizzes = Array.isArray(quizzesResponse.data)
        ? quizzesResponse.data
        : [];
      const attempts = Array.isArray(attemptsResponse.data)
        ? attemptsResponse.data
        : [];

      const questionResponses = await Promise.all(
        quizzes.map((quiz) =>
          api.get(`/questions/student/quiz/${quiz.id}`)
        )
      );

      const quizzesWithAvailability = quizzes.map((quiz, index) => ({
        ...quiz,
        questionCount: Array.isArray(questionResponses[index].data)
          ? questionResponses[index].data.length
          : 0,
        attemptsUsed: attempts.filter(
          (attempt) =>
            String(attempt.quizId) === String(quiz.id) &&
            attempt.submitted === true
        ).length,
      }));

      setAvailableQuizzes(
        quizzesWithAvailability.filter(
          (quiz) =>
            quiz.questionCount > 0 &&
            quiz.attemptsUsed < Number(quiz.maxAttempts ?? 0)
        )
      );
    } catch (err) {
      console.error('Available quizzes loading failed:', err);

      if (err.response?.status === 401) {
        setQuizzesError(
          'Your session has expired. Please log in again to view available quizzes.'
        );
      } else if (err.response?.status === 403) {
        setQuizzesError(
          'Your account does not have permission to view these quizzes.'
        );
      } else if (err.response?.status === 404) {
        setQuizzesError(
          'Some quiz information could not be found. Please try again.'
        );
      } else {
        setQuizzesError(
          'Unable to load quizzes. Please try again.'
        );
      }
    } finally {
      setQuizzesLoading(false);
    }
  }, []);

  useEffect(() => {
    const token = getAuthValue('mindmeter_token');
    const role = getAuthValue('mindmeter_role');

    if (!token || role !== 'STUDENT') {
      navigate('/login');
      return;
    }

    let active = true;

    const fetchDashboard = async (showLoading = false) => {
      try {
        if (showLoading) {
          setLoading(true);
        }
        setError('');

        const response = await api.get('/student/dashboard', {
          headers: {
            'Cache-Control': 'no-cache',
            Pragma: 'no-cache',
          },
        });
        if (!active) {
          return;
        }

        setDashboard(response.data);
        if (import.meta.env.DEV) {
          console.debug('Dashboard practice duration received', {
            totalPracticeSeconds: response.data?.totalPracticeSeconds,
            totalPracticeMinutes: response.data?.totalPracticeMinutes,
            totalPracticeHours: response.data?.totalPracticeHours,
          });
        }
      } catch (err) {
        console.error('Dashboard loading failed:', err);

        if (!active) {
          return;
        }
        if (err.response?.status === 401 ||
            err.response?.status === 403) {
          setError(
            'Your session may have expired or your account does not have student access. Please log in again.'
          );
        } else {
          setError(
            err.response?.data?.message ||
            'Unable to load dashboard. Please check the backend connection.'
          );
        }
      } finally {
        if (active && showLoading) {
          setLoading(false);
        }
      }
    };

    const refreshOnReturn = () => {
      if (document.visibilityState === 'visible') {
        fetchDashboard();
      }
    };
    const refreshFromPageCache = (event) => {
      if (event.persisted) {
        fetchDashboard();
      }
    };
    window.addEventListener('mindmeter:attempt-submitted', refreshOnReturn);
    window.addEventListener('pageshow', refreshFromPageCache);
    document.addEventListener('visibilitychange', refreshOnReturn);

    fetchDashboard(true);
    const quizLoadTimer = window.setTimeout(
      loadAvailableQuizzes,
      0
    );

    return () => {
      active = false;
      window.clearTimeout(quizLoadTimer);
      window.removeEventListener(
        'mindmeter:attempt-submitted',
        refreshOnReturn
      );
      window.removeEventListener('pageshow', refreshFromPageCache);
      document.removeEventListener('visibilitychange', refreshOnReturn);
    };
  }, [loadAvailableQuizzes, navigate]);

  const handleLogout = () => {
    clearAuthSession();

    navigate('/login');
  };

  const handleRetry = () => {
    window.location.reload();
  };

  if (loading) {
    return (
      <div className="dashboard-message">
        <h2>Loading your dashboard...</h2>
        <p>Please wait while we fetch your learning progress.</p>
        <ThemeToggle className="theme-toggle--floating" />
      </div>
    );
  }

  if (error) {
    return (
      <div className="dashboard-message">
        <h2>Dashboard Error</h2>
        <p>{error}</p>
        <ThemeToggle className="theme-toggle--floating" />
        <button onClick={handleRetry}>Try Again</button>
        <button onClick={handleLogout}>Logout</button>
      </div>
    );
  }

  const totalAttempts = dashboard?.totalAttempts ?? 0;
  const cumulativeScore = dashboard?.cumulativeScore ?? 0;
  const cumulativeTotalMarks = dashboard?.cumulativeTotalMarks ?? 0;
  const practiceSeconds = Number(dashboard?.totalPracticeSeconds);
  const practiceDuration = formatPracticeDuration(
    Number.isFinite(practiceSeconds)
      ? practiceSeconds
      : (Number(dashboard?.totalPracticeMinutes) || 0) * 60
  );
  const learningStreak = dashboard?.learningStreakDays ?? 0;
  const averagePercentage = Number(
    dashboard?.averagePercentage ?? 0
  );

  const subjects = dashboard?.subjectPerformance ?? [];

  const percentage = Math.min(
    100,
    Math.max(0, averagePercentage)
  );

  return (
    <div className="student-dashboard">

      {/* Navigation */}
      <nav className="dashboard-navbar">
        <BrandLogo className="dashboard-brand" />

        <div className="dashboard-nav-right">
          <span className="student-role">Student Dashboard</span>
          <button
            className="logout-button"
            onClick={handleLogout}
          >
            Logout
          </button>
          <ThemeToggle />
        </div>
      </nav>

      <main className="dashboard-main">

        {/* Welcome Section */}
        <section className="welcome-section">
          <div>
            <p className="eyebrow">
              YOUR LEARNING OVERVIEW
            </p>

            <h1>Welcome, {studentName}!</h1>

            <p className="welcome-description">
              Track your quiz performance, practice time, and
              learning progress in one place. Keep practicing
              to improve your results.
            </p>
          </div>

          <div className="overall-score">
            <span>Average Performance</span>

            <strong>{percentage.toFixed(1)}%</strong>

            <div className="progress-track">
              <div
                className="progress-fill"
                style={{ width: `${percentage}%` }}
              />
            </div>
          </div>
        </section>

        {/* Statistics Cards */}
        <section className="stats-grid">

          <div className="stat-card">
            <div className="stat-card-top">
              <span>Total Quiz Attempts</span>
              <span className="stat-icon">📝</span>
            </div>

            <h2>{totalAttempts}</h2>
          </div>

          <div className="stat-card">
            <div className="stat-card-top">
              <span>Total Score</span>
              <span className="stat-icon">⭐</span>
            </div>

            <h2>{cumulativeScore}</h2>

            <div className="stat-card-top">
              <span>Out of {cumulativeTotalMarks} marks</span>
            </div>
          </div>

          <div className="stat-card">
            <div className="stat-card-top">
              <span>Practice Hours</span>
              <span className="stat-icon">⏱️</span>
            </div>

            <h2>{practiceDuration}</h2>
          </div>

          <div className="stat-card">
            <div className="stat-card-top">
              <span>Learning Streak</span>
              <span className="stat-icon">🔥</span>
            </div>

            <h2>{learningStreak} days</h2>
          </div>

        </section>

        {/* Available Quizzes */}
        <section className="available-quizzes-section">
          <div className="section-heading">
            <h2>Available Quizzes</h2>
            <p>
              Choose a quiz to test your knowledge and track your progress.
            </p>
          </div>

          {quizzesLoading ? (
            <div className="empty-state">
              <p>Loading available quizzes...</p>
            </div>
          ) : quizzesError ? (
            <div className="empty-state" role="alert">
              <p>{quizzesError}</p>
              <button
                className="logout-button quiz-retry-button"
                type="button"
                onClick={loadAvailableQuizzes}
              >
                Retry
              </button>
            </div>
          ) : availableQuizzes.length === 0 ? (
            <div className="empty-state">
              <span>📝</span>
              <h3>No quizzes are available yet.</h3>
            </div>
          ) : (
            <div className="available-quizzes-grid">
              {availableQuizzes.map((quiz) => (
                  <article className="subject-card available-quiz-card" key={quiz.id}>
                    <div className="subject-card-heading">
                      <div className="subject-avatar">
                        {(quiz.subjectName || 'S')
                          .charAt(0)
                          .toUpperCase()}
                      </div>
                      <div>
                        <p className="available-quiz-subject">
                          {quiz.subjectName || 'Subject'}
                        </p>
                        <h3>{quiz.title}</h3>
                      </div>
                    </div>

                    <p className="available-quiz-description">
                      {quiz.description || 'No description provided.'}
                    </p>

                    <div className="available-quiz-details">
                      <span>
                        Duration: {quiz.durationMinutes} minutes
                      </span>
                      <span>
                        Maximum Attempts: {quiz.maxAttempts}
                      </span>
                      <span>
                        Questions: {quiz.questionCount}
                      </span>
                    </div>

                    <button
                      className="logout-button quiz-start-button"
                      type="button"
                      onClick={() => {
                        sessionStorage.setItem(
                          'mindmeter_start_quiz',
                          String(quiz.id)
                        );
                        navigate(`/student/quiz/${quiz.id}`);
                      }}
                    >
                      Start Quiz
                    </button>
                  </article>
              ))}
            </div>
          )}
        </section>

        {/* Subject Performance */}
        <section className="subjects-section">

          <div className="section-heading">
            <h2>Subject-wise Performance</h2>
            <p>
              Review your scores and progress across subjects.
            </p>
          </div>

          {subjects.length === 0 ? (
            <div className="empty-state">
              <span>📚</span>

              <h3>No quiz results yet</h3>

              <p>
                Complete a quiz to see your subject-wise
                performance here. Your results will appear
                after your first completed attempt.
              </p>
            </div>
          ) : (
            <div className="subjects-grid">
              {subjects.map((subject, index) => {
                const subjectPercentage = Math.min(
                  100,
                  Math.max(0, Number(subject.percentage ?? 0))
                );

                return (
                  <article
                    className="subject-card"
                    key={subject.subjectId ?? index}
                  >
                    <div className="subject-card-heading">
                      <div className="subject-avatar">
                        {(subject.subjectName || 'S')
                          .charAt(0)
                          .toUpperCase()}
                      </div>

                      <div>
                        <h3>
                          {subject.subjectName || 'Unnamed Subject'}
                        </h3>

                        <p>
                          {subject.attempts ?? 0} quiz attempts
                        </p>
                      </div>
                    </div>

                    <div className="subject-score">
                      <strong>
                        {subjectPercentage.toFixed(1)}%
                      </strong>

                      <span>
                        {subject.totalScore ?? 0} /{' '}
                        {subject.totalMarks ?? 0} marks
                      </span>
                    </div>

                    <div className="progress-track">
                      <div
                        className="progress-fill subject-progress"
                        style={{
                          width: `${subjectPercentage}%`,
                        }}
                      />
                    </div>
                  </article>
                );
              })}
            </div>
          )}
        </section>

        {/* Practice Summary */}
        <section className="practice-summary">
          <div className="practice-summary-icon">🎯</div>

          <div>
            <h3>Keep up your learning streak!</h3>

            <p>
              You have practiced for{' '}
              <strong>
                {practiceDuration}
              </strong>{' '}
              and maintained a streak of{' '}
              <strong>{learningStreak} days</strong>.
              Continue practicing to improve your performance.
            </p>
          </div>
        </section>

        <footer className="dashboard-footer">
          <p>MindMeter — Learn, Practice, Improve.</p>
        </footer>

      </main>
    </div>
  );
}

export default StudentDashboard;