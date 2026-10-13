import { useEffect, useState } from 'react';

type UserProfileData = {
  id: string;
  userId: string;
  firstName: string;
  lastName: string;
  email: string;
  status: string;
  customerNumber: string | null;
};

type UserProfileProps = {
  accessToken: string;
  onSignOut: () => void;
};

function getUserIdFromToken(token: string): string | null {
  try {
    const encodedPayload = token.split('.')[1];
    if (!encodedPayload) return null;
    const base64 = encodedPayload.replace(/-/g, '+').replace(/_/g, '/');
    const decoded = atob(base64.padEnd(base64.length + ((4 - (base64.length % 4)) % 4), '='));
    const claims: unknown = JSON.parse(decoded);
    if (
      typeof claims === 'object' &&
      claims !== null &&
      'sub' in claims &&
      typeof claims.sub === 'string' &&
      claims.sub.length > 0
    ) {
      return claims.sub;
    }
    return null;
  } catch {
    return null;
  }
}

function initials(profile: UserProfileData): string {
  return `${profile.firstName.trim().charAt(0)}${profile.lastName.trim().charAt(0)}`.toUpperCase() || 'U';
}

export default function UserProfile({ accessToken, onSignOut }: UserProfileProps) {
  const [profile, setProfile] = useState<UserProfileData | null>(null);
  const [loading, setLoading] = useState(true);
  const [error, setError] = useState('');

  useEffect(() => {
    const controller = new AbortController();
    const userId = getUserIdFromToken(accessToken);

    if (!userId) {
      setError('We could not identify your account. Please sign in again.');
      setLoading(false);
      return () => controller.abort();
    }

    async function loadProfile() {
      setLoading(true);
      setError('');
      try {
        const response = await fetch(`/api/customers/by-user/${encodeURIComponent(userId)}`, {
          headers: { Authorization: `Bearer ${accessToken}`, Accept: 'application/json' },
          signal: controller.signal,
        });

        if (response.status === 401 || response.status === 403) {
          throw new Error('Your session is no longer valid. Please sign in again.');
        }
        if (response.status === 404) {
          throw new Error('We could not find a profile for this account.');
        }
        if (!response.ok) {
          throw new Error('We could not load your profile right now. Please try again.');
        }

        const payload: unknown = await response.json();
        if (
          typeof payload !== 'object' ||
          payload === null ||
          !('firstName' in payload) ||
          typeof payload.firstName !== 'string' ||
          !('lastName' in payload) ||
          typeof payload.lastName !== 'string' ||
          !('email' in payload) ||
          typeof payload.email !== 'string' ||
          !('status' in payload) ||
          typeof payload.status !== 'string'
        ) {
          throw new Error('The profile service returned an unexpected response.');
        }

        setProfile({
          id: 'id' in payload && typeof payload.id === 'string' ? payload.id : '',
          userId: 'userId' in payload && typeof payload.userId === 'string' ? payload.userId : userId,
          firstName: payload.firstName,
          lastName: payload.lastName,
          email: payload.email,
          status: payload.status,
          customerNumber:
            'customerNumber' in payload && typeof payload.customerNumber === 'string'
              ? payload.customerNumber
              : null,
        });
      } catch (cause) {
        if (cause instanceof DOMException && cause.name === 'AbortError') return;
        setError(
          cause instanceof TypeError
            ? 'Unable to reach the profile service. Check that the Customer Service is running.'
            : cause instanceof Error
              ? cause.message
              : 'We could not load your profile right now. Please try again.',
        );
      } finally {
        if (!controller.signal.aborted) setLoading(false);
      }
    }

    void loadProfile();
    return () => controller.abort();
  }, [accessToken]);

  return (
    <main className="page-shell profile-page">
      <header className="topbar">
        <a className="brand" href="/" aria-label="Fintech home" onClick={(event) => event.preventDefault()}>
          <span className="brand-mark" aria-hidden="true">
            <svg viewBox="0 0 32 32" fill="none"><path d="M7 23.5V8.5h4.2l4.8 7.1 4.8-7.1H25v15h-4.4v-8.1L16 22l-4.6-6.6v8.1H7Z" fill="currentColor" /></svg>
          </span>
          <span>fintech<span className="brand-period">.</span></span>
        </a>
        <div className="topbar-prompt">
          <button className="text-link" type="button" onClick={onSignOut}>Sign out</button>
        </div>
      </header>

      <section className="profile-content" aria-labelledby="profile-title">
        <div className="profile-page-heading">
          <span className="eyebrow"><span className="eyebrow-dot" /> YOUR ACCOUNT</span>
          <h1 id="profile-title">Your profile<span>.</span></h1>
          <p className="intro-copy">Your personal information, all in one place.</p>
        </div>

        {loading && (
          <div className="form-card profile-card" role="status" aria-live="polite">
            <div className="profile-loading-indicator" aria-hidden="true" />
            <p>Loading your profile…</p>
          </div>
        )}

        {!loading && error && (
          <div className="form-card profile-card">
            <div className="profile-error-mark" aria-hidden="true">!</div>
            <h2>Profile unavailable</h2>
            <p className="profile-message" role="alert">{error}</p>
            <button className="submit-button" type="button" onClick={() => window.location.reload()}>Try again</button>
          </div>
        )}

        {!loading && profile && (
          <div className="form-card profile-card">
            <div className="profile-identity">
              <div className="profile-avatar" aria-hidden="true">{initials(profile)}</div>
              <div>
                <h2>{profile.firstName} {profile.lastName}</h2>
                <p>{profile.email}</p>
              </div>
              <span className={profile.status.toLowerCase() === 'active' ? 'profile-status profile-status-active' : 'profile-status'}>
                {profile.status}
              </span>
            </div>
            <div className="profile-section-title">Personal information</div>
            <dl className="profile-details">
              <div className="profile-detail">
                <dt>First name</dt>
                <dd>{profile.firstName}</dd>
              </div>
              <div className="profile-detail">
                <dt>Last name</dt>
                <dd>{profile.lastName}</dd>
              </div>
              <div className="profile-detail profile-detail-wide">
                <dt>Email address</dt>
                <dd>{profile.email}</dd>
              </div>
              {profile.customerNumber && (
                <div className="profile-detail profile-detail-wide">
                  <dt>Customer number</dt>
                  <dd>{profile.customerNumber}</dd>
                </div>
              )}
            </dl>
            <p className="profile-privacy-note">This information is read from your customer profile. Profile editing is not part of this story.</p>
          </div>
        )}
        <footer className="form-footer">© 2026 Fintech Services <span aria-hidden="true">·</span> Your financial life, in clearer view</footer>
      </section>
    </main>
  );
}
