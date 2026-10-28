import { useCallback, useEffect, useRef, useState } from 'react';
import { useNavigate, useParams } from 'react-router-dom';
import api from '../api/axiosConfig';
import { clearAuthSession, getAuthValue } from '../api/authStorage';
import BrandLogo from '../components/BrandLogo.jsx';
import ThemeToggle from '../theme/ThemeToggle.jsx';
import './StudentQuiz.css';

function formatTime(seconds) {
  const minutes = Math.floor(seconds / 60);
  const remainingSeconds = seconds % 60;

  return `${String(minutes).padStart(2, '0')}:${String(
    remainingSeconds
  ).padStart(2, '0')}`;
}

function getRequestErrorMessage(error, action) {
  const status = error.response?.status;

  if (status === 401) {
    return 'Your session has expired. Please log in again.';
  }

  if (status === 403) {
    return 'Your account does not have permission to access this quiz.';
  }

  if (status === 404) {
    return 'This quiz could not be found or is no longer available.';
  }

  if (status === 400) {
    return 'The quiz link is invalid. Return to the dashboard and select a quiz again.';
  }

  if (status >= 500) {
    return `The server could not ${action} right now. Please try again later.`;
  }

  if (!error.response) {
    return `Unable to connect to the server to ${action}. Check your connection and try again.`;
  }

  if (status) {
    return `Unable to ${action} (HTTP ${status}). Please try again.`;
  }

  return `Unable to ${action}. Please try again.`;
}

function StudentQuiz() {
  const { quizId } = useParams();
  const navigate = useNavigate();

  const [quiz, setQuiz] = useState(null);
  const [questions, setQuestions] = useState([]);
  const [answers, setAnswers] = useState({});
  const [secondsRemaining, setSecondsRemaining] = useState(null);
  const [attemptId, setAttemptId] = useState(null);
  const [attemptStatus, setAttemptStatus] = useState('IN_PROGRESS');
  const [pauseSecondsRemaining, setPauseSecondsRemaining] = useState(0);
  const [saveStatus, setSaveStatus] = useState('saved');
  const [result, setResult] = useState(null);
  const [loading, setLoading] = useState(true);
  const [loadError, setLoadError] = useState('');
  const [submissionError, setSubmissionError] = useState('');
  const [actionError, setActionError] = useState('');
  const [submitting, setSubmitting] = useState(false);
  const submissionStarted = useRef(false);
  const expirySubmissionStarted = useRef(false);
  const submitQuizRef = useRef(null);
  const pendingSaves = useRef(new Map());
  const unsavedAnswers = useRef(new Map());

  useEffect(() => {
    const token = getAuthValue('mindmeter_token');
    const role = getAuthValue('mindmeter_role');

    if (!token || role !== 'STUDENT') {
      navigate('/login', { replace: true });
      return undefined;
    }

    let active = true;

    async function loadQuiz() {
      setLoading(true);
      setLoadError('');

      if (!/^\d+$/.test(quizId) || Number(quizId) < 1) {
        setLoadError(
          'The quiz link is invalid. Return to the dashboard and select a quiz again.'
        );
        setLoading(false);
        return;
      }

      try {
        const startNewAttempt =
          sessionStorage.getItem('mindmeter_start_quiz') === String(quizId);
        const startUrl = `/attempts/quiz/${quizId}/start${
          startNewAttempt ? '?newAttempt=true' : ''
        }`;
        const [quizResponse, questionsResponse] = await Promise.all([
          api.get(`/quizzes/${quizId}`),
          api.get(`/questions/student/quiz/${quizId}`),
        ]);

        if (!active) {
          return;
        }

        const loadedQuiz = quizResponse.data;
        const loadedQuestions = Array.isArray(questionsResponse.data)
          ? questionsResponse.data
          : [];

        if (loadedQuestions.length === 0) {
          setQuiz(loadedQuiz);
          setLoadError('This quiz does not have any questions yet.');
          return;
        }

        const attemptResponse = await api.post(startUrl);
        if (!active) {
          return;
        }
        const progress = attemptResponse.data;
        if (startNewAttempt) {
          sessionStorage.removeItem('mindmeter_start_quiz');
        }

        setQuiz(loadedQuiz);
        setQuestions(loadedQuestions);
        setAttemptId(progress.attemptId);
        setAnswers(progress.answers || {});
        setAttemptStatus(progress.status);
        setPauseSecondsRemaining(progress.pauseSecondsRemaining ?? 0);
        setSecondsRemaining(progress.remainingSeconds ?? 0);

        if (progress.status === 'SUBMITTED' && progress.result) {
          setResult(progress.result);
        }
      } catch (error) {
        console.error('Quiz loading failed:', error);

        if (active) {
          setLoadError(getRequestErrorMessage(error, 'load this quiz'));
        }
      } finally {
        if (active) {
          setLoading(false);
        }
      }
    }

    loadQuiz();

    return () => {
      active = false;
    };
  }, [navigate, quizId]);

  const submitQuiz = useCallback(async () => {
    if (
      submissionStarted.current ||
      !quiz ||
      !attemptId ||
      questions.length === 0 ||
      result
    ) {
      return;
    }

    submissionStarted.current = true;
    setSubmitting(true);
    setSubmissionError('');

    try {
      await Promise.allSettled([...pendingSaves.current.values()]);
      const response = await api.post('/attempts/submit', {
        quizId: quiz.id,
        attemptId,
        answers,
      });

      const progress = response.data;
      setAttemptStatus(progress.status);
      setSecondsRemaining(progress.remainingSeconds ?? 0);
      setPauseSecondsRemaining(progress.pauseSecondsRemaining ?? 0);
      window.dispatchEvent(new Event('mindmeter:attempt-submitted'));
      if (progress.result) {
        setResult(progress.result);
      }
    } catch (error) {
      console.error('Quiz submission failed:', error);
      setSubmissionError(
        getRequestErrorMessage(error, 'submit your quiz')
      );
    } finally {
      setSubmitting(false);
    }
  }, [answers, attemptId, questions.length, quiz, result]);

  useEffect(() => {
    submitQuizRef.current = submitQuiz;
  }, [submitQuiz]);

  useEffect(() => {
    if (!quiz || !attemptId || result || attemptStatus !== 'IN_PROGRESS') {
      return undefined;
    }

    const timerId = window.setInterval(() => {
      setSecondsRemaining((current) => {
        const remaining = current === null ? null : Math.max(0, current - 1);
        if (remaining === 0 && !expirySubmissionStarted.current) {
          expirySubmissionStarted.current = true;
          submitQuizRef.current?.();
        }
        return remaining;
      });
    }, 1000);

    return () => {
      window.clearInterval(timerId);
    };
  }, [attemptId, attemptStatus, quiz, result]);

  useEffect(() => {
    if (!attemptId || result) {
      return undefined;
    }

    const syncTimerId = window.setInterval(async () => {
      try {
        const response = await api.get(`/attempts/${attemptId}`);
        const progress = response.data;
        setSecondsRemaining(progress.remainingSeconds ?? 0);
        setPauseSecondsRemaining(progress.pauseSecondsRemaining ?? 0);
        setAttemptStatus(progress.status);
        if (progress.status === 'SUBMITTED' && progress.result) {
          setResult(progress.result);
        }
      } catch (error) {
        console.error('Quiz progress sync failed:', error);
        setSaveStatus('failed');
      }
    }, 5000);

    return () => window.clearInterval(syncTimerId);
  }, [attemptId, result]);

  const saveAnswer = useCallback((questionId, answer) => {
    if (!attemptId) {
      return;
    }

    setSaveStatus('saving');
    unsavedAnswers.current.set(questionId, answer);
    const previousSave = pendingSaves.current.get(questionId) || Promise.resolve();
    const savePromise = previousSave
      .catch(() => undefined)
      .then(async () => {
        let lastError;
        for (let retry = 0; retry < 3; retry += 1) {
          try {
            const response = await api.put(
              `/attempts/${attemptId}/answer/${questionId}`,
              { answer }
            );
            const progress = response.data;
            setSecondsRemaining(progress.remainingSeconds ?? 0);
            setPauseSecondsRemaining(progress.pauseSecondsRemaining ?? 0);
            setAttemptStatus(progress.status);
            if (progress.status === 'SUBMITTED' && progress.result) {
              setResult(progress.result);
            }
            return;
          } catch (error) {
            lastError = error;
            if (retry < 2) {
              await new Promise((resolve) =>
                window.setTimeout(resolve, 500 * (retry + 1))
              );
            }
          }
        }
        throw lastError;
      })
      .then(() => {
        if (unsavedAnswers.current.get(questionId) === answer) {
          unsavedAnswers.current.delete(questionId);
        }
        if (pendingSaves.current.get(questionId) === savePromise) {
          pendingSaves.current.delete(questionId);
        }
        if (
          pendingSaves.current.size === 0 &&
          unsavedAnswers.current.size === 0
        ) {
          setSaveStatus('saved');
        }
      })
      .catch((error) => {
        console.error('Answer autosave failed:', error);
        if (pendingSaves.current.get(questionId) === savePromise) {
          pendingSaves.current.delete(questionId);
        }
        setSaveStatus('failed');
        throw error;
      });
    pendingSaves.current.set(questionId, savePromise);
    return savePromise;
  }, [attemptId]);

  async function handlePause() {
    if (!attemptId || attemptStatus !== 'IN_PROGRESS') {
      return;
    }
    setSubmitting(true);
    try {
      setActionError('');
      await Promise.allSettled([...pendingSaves.current.values()]);
      const retrySaves = [...unsavedAnswers.current.entries()].map(
        ([questionId, answer]) => saveAnswer(questionId, answer)
      );
      await Promise.allSettled(retrySaves);
      if (unsavedAnswers.current.size > 0) {
        throw new Error('Some answers could not be saved before pausing.');
      }
      const response = await api.post(`/attempts/${attemptId}/pause`);
      const progress = response.data;
      setAttemptStatus(progress.status);
      setSecondsRemaining(progress.remainingSeconds ?? 0);
      setPauseSecondsRemaining(progress.pauseSecondsRemaining ?? 0);
      if (progress.result) {
        setResult(progress.result);
      }
      setSaveStatus('saved');
    } catch (error) {
      console.error('Quiz pause failed:', error);
      setSaveStatus('failed');
      setActionError(
        getRequestErrorMessage(error, 'pause your quiz')
      );
    } finally {
      setSubmitting(false);
    }
  }

  async function handleResume() {
    if (!attemptId || attemptStatus !== 'PAUSED') {
      return;
    }
    setSubmitting(true);
    try {
      setActionError('');
      const response = await api.post(`/attempts/${attemptId}/resume`);
      const progress = response.data;
      setAttemptStatus(progress.status);
      setSecondsRemaining(progress.remainingSeconds ?? 0);
      setPauseSecondsRemaining(progress.pauseSecondsRemaining ?? 0);
      if (progress.status === 'SUBMITTED' && progress.result) {
        setResult(progress.result);
      }
    } catch (error) {
      console.error('Quiz resume failed:', error);
      setActionError(
        getRequestErrorMessage(error, 'resume your quiz')
      );
    } finally {
      setSubmitting(false);
    }
  }

  function handleRetrySubmission() {
    submissionStarted.current = false;
    submitQuiz();
  }

  const handleLogout = () => {
    clearAuthSession();

    navigate('/login', { replace: true });
  };

  if (loading) {
    return (
      <div className="student-quiz-message">
        <h2>Loading quiz...</h2>
        <ThemeToggle className="theme-toggle--floating" />
      </div>
    );
  }

  if (loadError || !quiz) {
    return (
      <div className="student-quiz-message" role="alert">
        <h2>Quiz Unavailable</h2>
        <p>{loadError || 'This quiz could not be loaded.'}</p>
        <ThemeToggle className="theme-toggle--floating" />
        <button
          className="student-quiz-button"
          type="button"
          onClick={() => navigate('/student-dashboard')}
        >
          Back to Dashboard
        </button>
      </div>
    );
  }

  if (result) {
    const score = Number(result.score ?? 0);
    const totalMarks = Number(result.totalMarks ?? 0);
    const percentage = totalMarks > 0
      ? (score / totalMarks) * 100
      : 0;

    return (
      <div className="student-quiz-page">
        <header className="student-quiz-navbar">
          <BrandLogo className="student-quiz-brand" />
          <button
            className="student-quiz-button student-quiz-logout"
            type="button"
            onClick={handleLogout}
          >
            Logout
          </button>
          <ThemeToggle />
        </header>

        <main className="student-quiz-main">
          <section className="student-quiz-card student-quiz-result">
            <p className="student-quiz-eyebrow">Quiz Result</p>
            <h1>{quiz.title}</h1>

            <dl className="student-quiz-result-grid">
              <div>
                <dt>Score</dt>
                <dd>{score} / {totalMarks}</dd>
              </div>
              <div>
                <dt>Percentage</dt>
                <dd>{percentage.toFixed(1)}%</dd>
              </div>
              <div>
                <dt>Correct Answers</dt>
                <dd>{result.correctAnswers ?? '—'}</dd>
              </div>
              <div>
                <dt>Incorrect Answers</dt>
                <dd>{result.incorrectAnswers ?? '—'}</dd>
              </div>
              <div>
                <dt>Attempt Number</dt>
                <dd>{result.attemptNumber ?? '—'}</dd>
              </div>
              <div>
                <dt>Submission Status</dt>
                <dd>{result.submitted ? 'Submitted' : 'In Progress'}</dd>
              </div>
            </dl>

            <button
              className="student-quiz-button"
              type="button"
              onClick={() => navigate('/student-dashboard', { replace: true })}
            >
              Return to Dashboard
            </button>
          </section>
        </main>
      </div>
    );
  }

  return (
    <div className="student-quiz-page">
      <header className="student-quiz-navbar">
        <BrandLogo className="student-quiz-brand" />
        <div className="student-quiz-nav-actions">
          <button
            className="student-quiz-button student-quiz-secondary-button"
            type="button"
            onClick={() => navigate('/student-dashboard')}
          >
            Dashboard
          </button>
          <button
            className="student-quiz-button student-quiz-logout"
            type="button"
            onClick={handleLogout}
          >
            Logout
          </button>
          <ThemeToggle />
        </div>
      </header>

      <main className="student-quiz-main">
        <section className="student-quiz-heading">
          <div>
            <p className="student-quiz-eyebrow">
              {quiz.subjectName || 'Available Quiz'}
            </p>
            <h1>{quiz.title}</h1>
            {quiz.description && <p>{quiz.description}</p>}
          </div>
          <div className="student-quiz-timer">
            <div aria-live="polite">
              <span>Time Remaining</span>
              <strong>{formatTime(secondsRemaining ?? 0)}</strong>
            </div>
            <span className="student-quiz-pause-budget">
              Pause time left: {formatTime(pauseSecondsRemaining)}
            </span>
            {attemptStatus === 'PAUSED' ? (
              <button
                className="student-quiz-button student-quiz-timer-action"
                type="button"
                onClick={handleResume}
                disabled={submitting || secondsRemaining === 0}
              >
                Resume Quiz
              </button>
            ) : (
              <button
                className="student-quiz-button student-quiz-timer-action"
                type="button"
                onClick={handlePause}
                disabled={submitting || pauseSecondsRemaining === 0}
              >
                Pause Quiz
              </button>
            )}
          </div>
        </section>

        {attemptStatus === 'PAUSED' && (
          <div className="student-quiz-paused" role="status">
            Quiz Paused — your answers are saved.
          </div>
        )}

        <p className={`student-quiz-save-status is-${saveStatus}`} role="status">
          {saveStatus === 'saving' && 'Saving…'}
          {saveStatus === 'saved' && 'Saved'}
          {saveStatus === 'failed' && 'Save failed. Your latest answer may not be saved.'}
        </p>

        {submissionError && (
          <div className="student-quiz-error" role="alert">
            <span>{submissionError}</span>
            <button
              className="student-quiz-button"
              type="button"
              onClick={handleRetrySubmission}
              disabled={submitting}
            >
              Retry Submission
            </button>
          </div>
        )}

        {actionError && (
          <div className="student-quiz-error" role="alert">
            <span>{actionError}</span>
          </div>
        )}

        <form
          className="student-quiz-question-list"
          onSubmit={(event) => {
            event.preventDefault();
            submitQuiz();
          }}
        >
          {questions.map((question, index) => (
            <fieldset className="student-quiz-card" key={question.id}>
              <legend>Question {index + 1}</legend>
              <h2>{question.questionText}</h2>

              {[
                ['A', question.optionA],
                ['B', question.optionB],
                ['C', question.optionC],
                ['D', question.optionD],
              ].map(([letter, option]) => (
                <label
                  className="student-quiz-option"
                  htmlFor={`question-${question.id}-${letter}`}
                  key={letter}
                >
                  <input
                    id={`question-${question.id}-${letter}`}
                    type="radio"
                    name={`question-${question.id}`}
                    value={letter}
                    checked={answers[question.id] === letter}
                    onChange={() => {
                      setAnswers((current) => ({
                        ...current,
                        [question.id]: letter,
                      }));
                      saveAnswer(question.id, letter);
                    }}
                    disabled={
                      submitting ||
                      attemptStatus === 'PAUSED' ||
                      secondsRemaining === 0
                    }
                  />
                  <span><strong>{letter}.</strong> {option}</span>
                </label>
              ))}
            </fieldset>
          ))}

          <button
            className="student-quiz-button student-quiz-submit"
            type="submit"
            disabled={
              submitting ||
              attemptStatus === 'PAUSED' ||
              secondsRemaining === 0
            }
          >
            {submitting ? 'Submitting...' : 'Submit Quiz'}
          </button>
        </form>
      </main>
    </div>
  );
}

export default StudentQuiz;
