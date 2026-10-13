import { useState, type FormEvent } from 'react';

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

function App() {
  const [passwordVisible, setPasswordVisible] = useState(false);
  const [notice, setNotice] = useState('');

  function handleSubmit(event: FormEvent<HTMLFormElement>) {
    event.preventDefault();
    setNotice('Your registration form is ready. Account creation will be connected in a follow-up step.');
  }

  return (
    <main className="page-shell">
      <header className="topbar">
        <a className="brand" href="/" aria-label="Fintech home">
          <BrandMark />
          <span>fintech<span className="brand-period">.</span></span>
        </a>
        <div className="topbar-prompt">
          Already have an account? <a href="#sign-in" onClick={(event) => { event.preventDefault(); setNotice('Sign-in will be available in a follow-up step.'); }}>Sign in</a>
        </div>
      </header>

      <div className="content-grid">
        <section className="intro-panel" aria-labelledby="intro-title">
          <div className="intro-content">
            <span className="eyebrow"><span className="eyebrow-dot" /> YOUR MONEY, IN BETTER HANDS</span>
            <h1 id="intro-title">A clearer view of your <span>financial life.</span></h1>
            <p className="intro-copy">One secure place to keep your finances organized and move forward with confidence.</p>

            <div className="benefit-list">
              <div className="benefit">
                <span className="benefit-icon">
                  <svg viewBox="0 0 24 24" fill="none" aria-hidden="true"><path d="M12 3 19 6v5c0 4.4-2.9 7.8-7 10-4.1-2.2-7-5.6-7-10V6l7-3Z" stroke="currentColor" strokeWidth="1.7" strokeLinejoin="round"/><path d="m9 12 2 2 4-4" stroke="currentColor" strokeWidth="1.7" strokeLinecap="round" strokeLinejoin="round"/></svg>
                </span>
                <div><strong>Security comes first</strong><span>Your information deserves careful protection.</span></div>
              </div>
              <div className="benefit">
                <span className="benefit-icon">
                  <svg viewBox="0 0 24 24" fill="none" aria-hidden="true"><path d="M4 19V5m0 14h16M7 15l4-4 3 2 5-6" stroke="currentColor" strokeWidth="1.7" strokeLinecap="round" strokeLinejoin="round"/><path d="M15 7h4v4" stroke="currentColor" strokeWidth="1.7" strokeLinecap="round" strokeLinejoin="round"/></svg>
                </span>
                <div><strong>Built for clarity</strong><span>Spend less time sorting and more time planning.</span></div>
              </div>
            </div>
          </div>
          <div className="panel-decoration" aria-hidden="true">
            <div className="orbit orbit-one" /><div className="orbit orbit-two" />
            <div className="decoration-card">
              <span className="decoration-label">YOUR NEXT CHAPTER</span>
              <span className="decoration-title">Starts here.</span>
              <span className="decoration-line"><span /></span>
            </div>
          </div>
          <p className="panel-footer">A more thoughtful way to manage money.</p>
        </section>

        <section className="form-panel" aria-labelledby="form-title">
          <div className="form-card">
            <div className="form-heading">
              <div className="form-step">LET'S GET STARTED</div>
              <h2 id="form-title">Create your account</h2>
              <p>Enter your details below to get started.</p>
            </div>

            <form onSubmit={handleSubmit}>
              <div className="name-row">
                <div className="field">
                  <label htmlFor="firstName">First name</label>
                  <input id="firstName" name="firstName" type="text" autoComplete="given-name" placeholder="e.g. Alex" maxLength={100} required />
                </div>
                <div className="field">
                  <label htmlFor="lastName">Last name</label>
                  <input id="lastName" name="lastName" type="text" autoComplete="family-name" placeholder="e.g. Morgan" maxLength={100} required />
                </div>
              </div>

              <div className="field">
                <label htmlFor="email">Email address</label>
                <div className="input-wrap">
                  <svg className="input-icon" viewBox="0 0 20 20" fill="none" aria-hidden="true"><rect x="2.5" y="4" width="15" height="12" rx="2" stroke="currentColor" strokeWidth="1.5"/><path d="m3.5 5 6.5 5 6.5-5" stroke="currentColor" strokeWidth="1.5" strokeLinecap="round" strokeLinejoin="round"/></svg>
                  <input id="email" name="email" type="email" autoComplete="email" placeholder="you@example.com" maxLength={254} required />
                </div>
              </div>

              <div className="field">
                <div className="label-row">
                  <label htmlFor="password">Password</label>
                  <span className="field-hint">8–100 characters</span>
                </div>
                <div className="input-wrap">
                  <svg className="input-icon" viewBox="0 0 20 20" fill="none" aria-hidden="true"><rect x="3.5" y="8.5" width="13" height="9" rx="2" stroke="currentColor" strokeWidth="1.5"/><path d="M6 8.5V6a4 4 0 0 1 8 0v2.5" stroke="currentColor" strokeWidth="1.5" strokeLinecap="round"/></svg>
                  <input id="password" name="password" type={passwordVisible ? 'text' : 'password'} autoComplete="new-password" placeholder="Create a password" minLength={8} maxLength={100} required />
                  <button className="password-toggle" type="button" onClick={() => setPasswordVisible((visible) => !visible)} aria-label={passwordVisible ? 'Hide password' : 'Show password'}>
                    {passwordVisible ? (
                      <svg viewBox="0 0 20 20" fill="none" aria-hidden="true"><path d="M2.5 10s2.7-5 7.5-5 7.5 5 7.5 5-2.7 5-7.5 5-7.5-5-7.5-5Z" stroke="currentColor" strokeWidth="1.5"/><circle cx="10" cy="10" r="2.2" stroke="currentColor" strokeWidth="1.5"/><path d="m3 3 14 14" stroke="currentColor" strokeWidth="1.5" strokeLinecap="round"/></svg>
                    ) : (
                      <svg viewBox="0 0 20 20" fill="none" aria-hidden="true"><path d="M2.5 10s2.7-5 7.5-5 7.5 5 7.5 5-2.7 5-7.5 5-7.5-5-7.5-5Z" stroke="currentColor" strokeWidth="1.5"/><circle cx="10" cy="10" r="2.2" stroke="currentColor" strokeWidth="1.5"/></svg>
                    )}
                  </button>
                </div>
              </div>

              <button className="submit-button" type="submit">Create account <span aria-hidden="true">→</span></button>
              {notice && <p className="form-notice" role="status">{notice}</p>}
            </form>

            <div className="security-note"><LockIcon /><span>Your details are kept private and secure.</span></div>
            <p className="terms-note">By continuing, you agree to our <a href="#terms" onClick={(event) => event.preventDefault()}>Terms of Service</a> and <a href="#privacy" onClick={(event) => event.preventDefault()}>Privacy Policy</a>.</p>
          </div>
          <footer className="form-footer">© 2026 Fintech Services <span aria-hidden="true">·</span> Made for a more confident financial future</footer>
        </section>
      </div>
    </main>
  );
}

export default App;
