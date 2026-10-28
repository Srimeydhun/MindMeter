
import { useEffect, useState } from 'react';

import {
  Navigate,
  Route,
  Routes,
  Link,
  useNavigate,
} from 'react-router-dom';

import StudentDashboard from './pages/StudentDashboard';
import StudentQuiz from './pages/StudentQuiz';
import TeacherDashboard from './pages/TeacherDashboard';
import BrandLogo from './components/BrandLogo.jsx';

import {
  loginUser,
  registerStudent,
  registerTeacher,
} from './api/authService';
import {
  clearAuthSession,
  setAuthSession,
} from './api/authStorage';
import ThemeToggle from './theme/ThemeToggle.jsx';

import './App.css';

// ==========================================
// HOME PAGE
// ==========================================

function Home() {
  const [menuOpen, setMenuOpen] = useState(false);

  return (
    <div className="app-container">
      <header className="navbar">
        <BrandLogo className="logo" />

        <button
          className="navbar-menu-toggle"
          type="button"
          aria-label={menuOpen ? 'Close navigation menu' : 'Open navigation menu'}
          aria-expanded={menuOpen}
          aria-controls="home-navigation"
          onClick={() => setMenuOpen((open) => !open)}
        >
          <svg viewBox="0 0 24 24" aria-hidden="true">
            {menuOpen ? (
              <path d="m6 6 12 12M18 6 6 18" />
            ) : (
              <path d="M4 6h16M4 12h16M4 18h16" />
            )}
          </svg>
        </button>

        <div
          className={`navbar-menu${menuOpen ? ' is-open' : ''}`}
          id="home-navigation"
        >
          <nav aria-label="Main navigation">
            <a href="#features" onClick={() => setMenuOpen(false)}>
              Features
            </a>
            <a href="#about" onClick={() => setMenuOpen(false)}>
              About
            </a>
          </nav>

          <div className="nav-buttons">
            <Link
              to="/login"
              className="btn btn-outline"
              onClick={() => setMenuOpen(false)}
            >
              Sign In
            </Link>

            <Link
              to="/register"
              className="btn btn-primary"
              onClick={() => setMenuOpen(false)}
            >
              Get Started
            </Link>
            <ThemeToggle />
          </div>
        </div>
      </header>

      <section className="hero">
        <div className="hero-content">
          <BrandLogo className="home-brand" />
          <span className="hero-tag">
            SMARTER LEARNING STARTS HERE
          </span>

          <h1>
            Learn smarter.
            <br />
            Practice better.
            <br />
            <span>Grow faster.</span>
          </h1>

          <p>
            MindMeter helps students practice subject-wise assessments,
            track their performance, and understand their learning progress.
            Teachers can manage quizzes and monitor student results.
          </p>

          <div className="hero-buttons">
            <Link
              to="/register"
              className="btn btn-primary btn-large"
            >
              Create Account →
            </Link>

            <Link
              to="/login"
              className="btn btn-outline btn-large"
            >
              Sign In
            </Link>
          </div>

          <p className="hero-note">
            A smarter way to assess knowledge and improve every day.
          </p>
        </div>

        <div className="hero-visual">
          <div className="dashboard-preview">
            <div className="preview-header">
              <div>
                <span className="preview-label">MINDMETER</span>
                <h3>Your learning journey</h3>
              </div>

              <span className="status-dot">
                ● Ready to learn
              </span>
            </div>

            <div className="preview-stats">
              <div className="stat-card">
                <span>Subject practice</span>
                <strong>Learn</strong>
                <small>Explore multiple subjects</small>
              </div>

              <div className="stat-card">
                <span>Assessments</span>
                <strong>Practice</strong>
                <small>Test your knowledge</small>
              </div>

              <div className="stat-card">
                <span>Performance</span>
                <strong>Track</strong>
                <small>Review your results</small>
              </div>

              <div className="stat-card">
                <span>Improvement</span>
                <strong>Grow</strong>
                <small>Learn from each attempt</small>
              </div>
            </div>

            <div className="progress-card">
              <div className="progress-heading">
                <span>Your next step</span>
                <strong>Start learning</strong>
              </div>

              <div className="progress-track">
                <div className="progress-fill" />
              </div>

              <p>
                Sign in to access your personal learning activities.
              </p>
            </div>

            <div className="preview-bottom">
              <span>Subject-wise quizzes</span>
              <span>Performance analytics</span>
            </div>
          </div>

        </div>
      </section>

      <section className="features" id="features">
        <div className="section-heading">
          <span className="section-tag">WHAT YOU CAN DO</span>

          <h2>Everything you need to learn effectively</h2>

          <p>
            Practice, evaluate your knowledge, and use performance insights
            to focus on areas that need improvement.
          </p>
        </div>

        <div className="feature-grid">
          <div className="feature-card">
            <div className="feature-icon purple">📝</div>
            <h3>Online Assessments</h3>
            <p>
              Attempt quizzes and test your understanding of different
              subjects.
            </p>
          </div>

          <div className="feature-card">
            <div className="feature-icon blue">📚</div>
            <h3>Multiple Subjects</h3>
            <p>
              Access subject-wise learning and assessment activities.
            </p>
          </div>

          <div className="feature-card">
            <div className="feature-icon green">📊</div>
            <h3>Student Analytics</h3>
            <p>
              Review quiz scores, previous attempts, and learning progress.
            </p>
          </div>

          <div className="feature-card">
            <div className="feature-icon orange">🎯</div>
            <h3>Focused Improvement</h3>
            <p>
              Identify topics that need more practice using assessment
              results.
            </p>
          </div>
        </div>
      </section>

      <section className="about-section" id="about">
        <span className="section-tag">ABOUT MINDMETER</span>

        <h2>Make every learning session count</h2>

        <p>
          MindMeter is a web-based multi-subject assessment and student
          analytics system designed to support students and teachers through
          organized quizzes and performance tracking.
        </p>

        <div className="hero-buttons about-buttons">
          <Link to="/register" className="btn btn-primary">
            Join MindMeter
          </Link>
        </div>
      </section>

      <footer>
        <p>
          © {new Date().getFullYear()} MindMeter. All rights reserved.
        </p>
        <span>Learn · Practice · Improve</span>
      </footer>
    </div>
  );
}

// ==========================================
// LOGIN PAGE
// ==========================================

function Login() {
  const navigate = useNavigate();

  const [username, setUsername] = useState('');
  const [password, setPassword] = useState('');
  const [role, setRole] = useState('STUDENT');
  const [error, setError] = useState('');
  const [loading, setLoading] = useState(false);

  const handleLogin = async (event) => {
    event.preventDefault();

    setError('');
    setLoading(true);
    clearAuthSession();

    try {
      const data = await loginUser(
        username.trim(),
        password,
        role
      );

      if (!data?.token) {
        throw new Error(
          'The server did not return an authentication token.'
        );
      }

      const loggedInRole = String(
        data.role || role
      ).toUpperCase();

      if (
        loggedInRole !== 'STUDENT' &&
        loggedInRole !== 'TEACHER'
      ) {
        throw new Error(
          'Unknown account role returned by the server.'
        );
      }

      setAuthSession(data, loggedInRole);

      // Redirect according to the user's role.
      if (loggedInRole === 'STUDENT') {
        navigate('/student-dashboard', { replace: true });
      } else {
        navigate('/teacher-dashboard', { replace: true });
      }
    } catch (err) {
      clearAuthSession();

      setError(
        err.response?.data?.message ||
          err.response?.data?.error ||
          err.message ||
          'Login failed. Please check your credentials.'
      );
    } finally {
      setLoading(false);
    }
  };

  return (
    <div className="placeholder-page auth-page login-page">
      <ThemeToggle className="theme-toggle--floating auth-theme-toggle" />
      <div className="auth-heading">
        <BrandLogo className="auth-brand" />
        <span className="auth-tag">WELCOME BACK</span>

        <h1>Welcome back to MindMeter</h1>

        <p>Sign in to continue your learning journey.</p>
      </div>

      <form className="auth-form" onSubmit={handleLogin}>
        <div className="form-group">
          <label htmlFor="login-role">Account type</label>

          <select
            id="login-role"
            value={role}
            onChange={(event) => {
              setRole(event.target.value);
              setUsername('');
              setPassword('');
              setError('');
            }}
          >
            <option value="STUDENT">Student</option>
            <option value="TEACHER">Teacher</option>
          </select>
        </div>

        <div className="form-group">
          <label htmlFor="login-username">
            {role === 'STUDENT'
              ? 'College register number'
              : 'Email address'}
          </label>

          <input
            id="login-username"
            type={role === 'STUDENT' ? 'text' : 'email'}
            value={username}
            onChange={(event) => setUsername(event.target.value)}
            placeholder={
              role === 'STUDENT'
                ? 'Enter your college register number'
                : 'Enter your email address'
            }
            autoComplete="username"
            required
          />
        </div>

        <div className="form-group">
          <label htmlFor="login-password">
            {role === 'STUDENT' ? 'Date of birth' : 'Password'}
          </label>

          <input
            id="login-password"
            type={role === 'STUDENT' ? 'date' : 'password'}
            value={password}
            onChange={(event) => setPassword(event.target.value)}
            placeholder={
              role === 'STUDENT'
                ? 'Enter DOB as YYYY-MM-DD'
                : 'Enter your password'
            }
            autoComplete="current-password"
            required
          />

          {role === 'STUDENT' && (
            <small>
              Use the date of birth entered during registration.
            </small>
          )}
        </div>

        {error && (
          <p className="form-error" role="alert">
            {error}
          </p>
        )}

        <button
          className="btn btn-primary auth-submit"
          type="submit"
          disabled={loading}
        >
          {loading ? 'Signing in...' : 'Sign In'}
        </button>
      </form>

      <p className="auth-switch">
        Don't have an account? <Link to="/register">Register here</Link>
      </p>

      <Link className="auth-home-link" to="/">
        Back to Home
      </Link>
    </div>
  );
}

// ==========================================
// REGISTER PAGE
// ==========================================

function Register() {
  const [role, setRole] = useState('STUDENT');
  const [name, setName] = useState('');
  const [registerNumber, setRegisterNumber] = useState('');
  const [email, setEmail] = useState('');
  const [dateOfBirth, setDateOfBirth] = useState('');
  const [password, setPassword] = useState('');
  const [error, setError] = useState('');
  const [success, setSuccess] = useState('');
  const [loading, setLoading] = useState(false);

  const handleRegister = async (event) => {
    event.preventDefault();

    setError('');
    setSuccess('');
    setLoading(true);

    try {
      if (role === 'STUDENT') {
        await registerStudent({
          name: name.trim(),
          registerNumber: registerNumber.trim(),
          email: email.trim(),
          dateOfBirth,
        });
      } else {
        await registerTeacher({
          name: name.trim(),
          email: email.trim(),
          password,
        });
      }

      setSuccess('Registration successful! You can now sign in.');

      setName('');
      setRegisterNumber('');
      setEmail('');
      setDateOfBirth('');
      setPassword('');
    } catch (err) {
      setError(
        err.response?.data?.message ||
          err.response?.data?.error ||
          err.message ||
          'Registration failed. Please check your details.'
      );
    } finally {
      setLoading(false);
    }
  };

  return (
    <div className="placeholder-page auth-page">
      <ThemeToggle className="theme-toggle--floating auth-theme-toggle" />
      <div className="auth-heading">
        <BrandLogo className="auth-brand" />
        <span className="auth-tag">GET STARTED</span>

        <h1>Create your MindMeter account</h1>

        <p>Register to access assessments and learning analytics.</p>
      </div>

      <form className="auth-form" onSubmit={handleRegister}>
        <div className="form-group">
          <label htmlFor="register-role">Account type</label>

          <select
            id="register-role"
            value={role}
            onChange={(event) => {
              setRole(event.target.value);
              setError('');
              setSuccess('');
              setPassword('');
            }}
          >
            <option value="STUDENT">Student</option>
            <option value="TEACHER">Teacher</option>
          </select>
        </div>

        <div className="form-group">
          <label htmlFor="register-name">Full name</label>

          <input
            id="register-name"
            type="text"
            value={name}
            onChange={(event) => setName(event.target.value)}
            placeholder="Enter your full name"
            autoComplete="name"
            required
          />
        </div>

        {role === 'STUDENT' && (
          <div className="form-group">
            <label htmlFor="register-number">
              College register number
            </label>

            <input
              id="register-number"
              type="text"
              value={registerNumber}
              onChange={(event) =>
                setRegisterNumber(event.target.value)
              }
              placeholder="Enter your college register number"
              required
            />
          </div>
        )}

        <div className="form-group">
          <label htmlFor="register-email">Email address</label>

          <input
            id="register-email"
            type="email"
            value={email}
            onChange={(event) => setEmail(event.target.value)}
            placeholder="Enter your email address"
            autoComplete="email"
            required
          />
        </div>

        {role === 'STUDENT' ? (
          <div className="form-group">
            <label htmlFor="register-dob">Date of birth</label>

            <input
              id="register-dob"
              type="date"
              value={dateOfBirth}
              onChange={(event) => setDateOfBirth(event.target.value)}
              required
            />

            <small>
              Remember this date; you will use it to sign in.
            </small>
          </div>
        ) : (
          <div className="form-group">
            <label htmlFor="register-password">Password</label>

            <input
              id="register-password"
              type="password"
              value={password}
              onChange={(event) => setPassword(event.target.value)}
              placeholder="Create a password"
              autoComplete="new-password"
              minLength={6}
              required
            />
          </div>
        )}

        {error && (
          <p className="form-error" role="alert">
            {error}
          </p>
        )}

        {success && (
          <p className="form-success" role="status">
            {success}
          </p>
        )}

        <button
          className="btn btn-primary auth-submit"
          type="submit"
          disabled={loading}
        >
          {loading ? 'Creating account...' : 'Create Account'}
        </button>
      </form>

      <p className="auth-switch">
        Already have an account? <Link to="/login">Sign in</Link>
      </p>

      <Link className="auth-home-link" to="/">
        Back to Home
      </Link>
    </div>
  );
}

// ==========================================
// MAIN APP AND ROUTES
// ==========================================

function App() {
  return (
    <>
      <Routes>
        <Route path="/" element={<Home />} />

        <Route path="/login" element={<Login />} />

        <Route path="/register" element={<Register />} />

        <Route
          path="/student-dashboard"
          element={<StudentDashboard />}
        />

        <Route
          path="/student/quiz/:quizId"
          element={<StudentQuiz />}
        />

        <Route
          path="/teacher-dashboard"
          element={<TeacherDashboard />}
        />

        <Route
          path="*"
          element={<Navigate to="/" replace />}
        />
      </Routes>
      <PWAInstallButton />
    </>
  );
}

function PWAInstallButton() {
  const [installPrompt, setInstallPrompt] = useState(null);

  useEffect(() => {
    const isStandalone =
      window.matchMedia('(display-mode: standalone)').matches ||
      window.navigator.standalone === true;

    if (isStandalone) {
      return undefined;
    }

    function handleInstallAvailable(event) {
      event.preventDefault();
      setInstallPrompt(event);
    }

    function handleInstalled() {
      setInstallPrompt(null);
    }

    window.addEventListener('beforeinstallprompt', handleInstallAvailable);
    window.addEventListener('appinstalled', handleInstalled);

    return () => {
      window.removeEventListener('beforeinstallprompt', handleInstallAvailable);
      window.removeEventListener('appinstalled', handleInstalled);
    };
  }, []);

  async function installApp() {
    if (!installPrompt) {
      return;
    }

    await installPrompt.prompt();
    setInstallPrompt(null);
  }

  if (!installPrompt) {
    return null;
  }

  return (
    <button
      className="pwa-install-button"
      type="button"
      onClick={installApp}
    >
      Install MindMeter
    </button>
  );
}

export default App;
