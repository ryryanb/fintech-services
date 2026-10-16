import { useEffect, useState, type FormEvent } from 'react';
import { clearAccessToken, setAccessToken } from './authSession';
import UserProfile from './UserProfile';
import ProtectedRoute from './ProtectedRoute';
import FinancialAccounts from './FinancialAccounts';

type LoginFormProps = {
  onCreateAccount: () => void;
};

function BrandMark() {
  return (
    <span className="brand-mark" aria-hidden="true">
      <svg viewBox="0 0 32 32" fill="none">
        <path d="M7 23.5V8.5h4.2l4.8 7.1 4.8-7.1H25v15h-4.4v-8.1L16 22l-4.6-6.6v8.1H7Z" fill="currentColor" />
      </svg>
    </span>
  );
}

function LockIcon() {
  return (
    <svg viewBox="0 0 20 20" fill="none" aria-hidden="true">
      <rect x="4.25" y="8.5" width="11.5" height="8.25" rx="2" stroke="currentColor" strokeWidth="1.5" />
      <path d="M6.75 8.5V6a3.25 3.25 0 0 1 6.5 0v2.5" stroke="currentColor" strokeWidth="1.5" strokeLinecap="round" />
      <path d="M10 11.75v1.75" stroke="currentColor" strokeWidth="1.5" strokeLinecap="round" />
    </svg>
  );
}

export default function LoginForm({ onCreateAccount }: LoginFormProps) {
  const [email, setEmail] = useState('');
  const [password, setPassword] = useState('');
  const [passwordVisible, setPasswordVisible] = useState(false);
  const [notice, setNotice] = useState('');
  const [noticeIsError, setNoticeIsError] = useState(false);
  const [submitting, setSubmitting] = useState(false);
  const [isAuthenticated, setIsAuthenticated] = useState(false);
  const [currentPath, setCurrentPath] = useState(window.location.pathname);

  useEffect(() => {
    function syncPath() {
      const path = window.location.pathname;
      if (!isAuthenticated && ['/profile', '/accounts'].includes(path)) {
        window.history.replaceState(null, '', '/login');
        setCurrentPath('/login');
        return;
      }
      setCurrentPath(path);
    }

    syncPath();
    window.addEventListener('popstate', syncPath);
    return () => window.removeEventListener('popstate', syncPath);
  }, [isAuthenticated]);

  async function handleSubmit(event: FormEvent<HTMLFormElement>) {
    event.preventDefault();
    setNotice('');
    setNoticeIsError(false);
    setSubmitting(true);

    try {
      const response = await fetch('/api/auth/login', {
        method: 'POST',
        headers: { 'Content-Type': 'application/json' },
        body: JSON.stringify({ email: email.trim(), password }),
      });

      const payload: unknown = await response.json().catch(() => null);

      if (!response.ok) {
        if (response.status === 401) {
          throw new Error('Invalid email or password. Check your details and try again.');
        }
        if (response.status === 400) {
          throw new Error('Enter a valid email address and password.');
        }
        throw new Error('We could not sign you in right now. Please try again.');
      }

      if (
        typeof payload !== 'object' ||
        payload === null ||
        !('accessToken' in payload) ||
        typeof payload.accessToken !== 'string' ||
        !payload.accessToken ||
        !('tokenType' in payload) ||
        typeof payload.tokenType !== 'string' ||
        !('expiresIn' in payload) ||
        typeof payload.expiresIn !== 'number'
      ) {
        throw new Error('The sign-in service returned an unexpected response. Please try again.');
      }

      setAccessToken(payload.accessToken);
      setIsAuthenticated(true);
      window.history.pushState(null, '', '/profile');
      setCurrentPath('/profile');
      setPassword('');
      setNotice('');
    } catch (error) {
      setNoticeIsError(true);
      setNotice(
        error instanceof TypeError
          ? 'Unable to reach the sign-in service. Check that the backend is running and try again.'
          : error instanceof Error
            ? error.message
            : 'We could not sign you in right now. Please try again.',
      );
    } finally {
      setSubmitting(false);
    }
  }

  if (isAuthenticated && currentPath === '/accounts') {
    return (
      <ProtectedRoute isAuthenticated={isAuthenticated}>
        <FinancialAccounts
          onBackToProfile={() => {
            window.history.pushState(null, '', '/profile');
            setCurrentPath('/profile');
          }}
          onSignOut={() => {
            clearAccessToken();
            setIsAuthenticated(false);
            window.history.replaceState(null, '', '/login');
            setCurrentPath('/login');
          }}
        />
      </ProtectedRoute>
    );
  }

  if (isAuthenticated && currentPath === '/profile') {
    return (
      <ProtectedRoute isAuthenticated={isAuthenticated}>
        <UserProfile
          onSignOut={() => {
            clearAccessToken();
            setIsAuthenticated(false);
            window.history.replaceState(null, '', '/login');
            setCurrentPath('/login');
          }}
          onViewAccounts={() => {
            window.history.pushState(null, '', '/accounts');
            setCurrentPath('/accounts');
          }}
        />
      </ProtectedRoute>
    );
  }

  return (
    <main className="page-shell login-page">
      <header className="topbar">
        <a className="brand" href="/" aria-label="Fintech home" onClick={(event) => { event.preventDefault(); window.history.pushState(null, '', '/'); onCreateAccount(); }}>
          <BrandMark />
          <span>fintech<span className="brand-period">.</span></span>
        </a>
        <div className="topbar-prompt">
          New to Fintech? <button className="text-link" type="button" onClick={onCreateAccount}>Create account</button>
        </div>
      </header>

      <div className="login-layout">
        <section className="login-intro" aria-labelledby="login-intro-title">
          <span className="eyebrow"><span className="eyebrow-dot" /> WELCOME BACK</span>
          <h1 id="login-intro-title">Good to see <span>you again.</span></h1>
          <p className="intro-copy">Sign in to pick up where you left off and keep your financial life moving forward.</p>
          <div className="login-trust-note">
            <span className="benefit-icon"><LockIcon /></span>
            <div>
              <strong>Your account, protected</strong>
              <span>Your sign-in details stay private and secure.</span>
            </div>
          </div>
        </section>

        <section className="form-panel login-form-panel" aria-labelledby="login-title">
          <div className="form-card login-card">
            <div className="form-heading">
              <div className="form-step">YOUR FINANCIAL SPACE</div>
              <h2 id="login-title">Sign in</h2>
              <p>Enter your account details to continue.</p>
            </div>

            <form onSubmit={handleSubmit}>
              <div className="field">
                <label htmlFor="login-email">Email address</label>
                <div className="input-wrap">
                  <svg className="input-icon" viewBox="0 0 20 20" fill="none" aria-hidden="true">
                    <rect x="2.5" y="4" width="15" height="12" rx="2" stroke="currentColor" strokeWidth="1.5" />
                    <path d="m3.5 5 6.5 5 6.5-5" stroke="currentColor" strokeWidth="1.5" strokeLinecap="round" strokeLinejoin="round" />
                  </svg>
                  <input id="login-email" name="email" type="email" autoComplete="username" placeholder="you@example.com" required value={email} onChange={(event) => { setEmail(event.currentTarget.value); setNotice(''); setNoticeIsError(false); }} />
                </div>
              </div>

              <div className="field">
                <div className="label-row">
                  <label htmlFor="login-password">Password</label>
                  <a className="subtle-link" href="#forgot-password" onClick={(event) => { event.preventDefault(); setNotice('Password recovery will be available in a follow-up story.'); }}>Forgot password?</a>
                </div>
                <div className="input-wrap">
                  <svg className="input-icon" viewBox="0 0 20 20" fill="none" aria-hidden="true">
                    <rect x="3.5" y="8.5" width="13" height="9" rx="2" stroke="currentColor" strokeWidth="1.5" />
                    <path d="M6 8.5V6a4 4 0 0 1 8 0v2.5" stroke="currentColor" strokeWidth="1.5" strokeLinecap="round" />
                  </svg>
                  <input id="login-password" name="password" type={passwordVisible ? 'text' : 'password'} autoComplete="current-password" placeholder="Enter your password" required value={password} onChange={(event) => { setPassword(event.currentTarget.value); setNotice(''); setNoticeIsError(false); }} />
                  <button className="password-toggle" type="button" onClick={() => setPasswordVisible((visible) => !visible)} aria-label={passwordVisible ? 'Hide password' : 'Show password'}>
                    {passwordVisible ? (
                      <svg viewBox="0 0 20 20" fill="none" aria-hidden="true"><path d="M2.5 10s2.7-5 7.5-5 7.5 5 7.5 5-2.7 5-7.5 5-7.5-5-7.5-5Z" stroke="currentColor" strokeWidth="1.5"/><circle cx="10" cy="10" r="2.2" stroke="currentColor" strokeWidth="1.5"/><path d="m3 3 14 14" stroke="currentColor" strokeWidth="1.5" strokeLinecap="round"/></svg>
                    ) : (
                      <svg viewBox="0 0 20 20" fill="none" aria-hidden="true"><path d="M2.5 10s2.7-5 7.5-5 7.5 5 7.5 5-2.7 5-7.5 5-7.5-5-7.5-5Z" stroke="currentColor" strokeWidth="1.5"/><circle cx="10" cy="10" r="2.2" stroke="currentColor" strokeWidth="1.5"/></svg>
                    )}
                  </button>
                </div>
              </div>

              <button className="submit-button" type="submit" disabled={submitting}>{submitting ? 'Signing in…' : 'Sign in'} <span aria-hidden="true">→</span></button>
              {notice && <p className={noticeIsError ? 'form-notice form-notice-error' : 'form-notice'} role="status" aria-live="polite">{notice}</p>}
            </form>

            <div className="security-note"><LockIcon /><span>Your details are kept private and secure.</span></div>
          </div>
          <footer className="form-footer">© 2026 Fintech Services <span aria-hidden="true">·</span> Made for a more confident financial future</footer>
        </section>
      </div>
    </main>
  );
}
