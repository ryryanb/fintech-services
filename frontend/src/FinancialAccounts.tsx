import { useEffect, useState } from 'react';
import { getAccessToken } from './authSession';

type FinancialAccount = {
  id: string;
  customerId: string;
  name: string;
  type: string;
  currency: string;
  balance: number | string;
  institutionName: string | null;
  status: string;
};

type TokenClaims = {
  customerId?: unknown;
};

function getCustomerIdFromToken(token: string): string | null {
  try {
    const encodedPayload = token.split('.')[1];
    if (!encodedPayload) return null;
    const base64 = encodedPayload.replace(/-/g, '+').replace(/_/g, '/');
    const decoded = atob(base64.padEnd(base64.length + ((4 - (base64.length % 4)) % 4), '='));
    const claims: unknown = JSON.parse(decoded);
    if (typeof claims !== 'object' || claims === null) return null;
    const customerId = (claims as TokenClaims).customerId;
    return typeof customerId === 'string' && customerId.length > 0 ? customerId : null;
  } catch {
    return null;
  }
}

function isFinancialAccount(value: unknown): value is FinancialAccount {
  if (typeof value !== 'object' || value === null) return false;
  const account = value as Record<string, unknown>;
  return (
    typeof account.id === 'string' &&
    typeof account.customerId === 'string' &&
    typeof account.name === 'string' &&
    typeof account.type === 'string' &&
    typeof account.currency === 'string' &&
    (typeof account.balance === 'number' || typeof account.balance === 'string') &&
    typeof account.status === 'string' &&
    (typeof account.institutionName === 'string' || account.institutionName === null)
  );
}

function formatBalance(balance: number | string, currency: string): string {
  const amount = typeof balance === 'number' ? balance : Number(balance);
  if (!Number.isFinite(amount)) return String(balance);
  try {
    return new Intl.NumberFormat(undefined, {
      style: 'currency',
      currency,
      minimumFractionDigits: 2,
      maximumFractionDigits: 2,
    }).format(amount);
  } catch {
    return `${currency} ${amount.toFixed(2)}`;
  }
}

function friendlyError(responseStatus: number, context: 'list' | 'detail'): string {
  if (responseStatus === 401 || responseStatus === 403) {
    return 'Your session is no longer valid or you do not have permission to view this account. Please sign in again.';
  }
  if (responseStatus === 404) {
    return context === 'detail'
      ? 'This account could not be found. It may have been removed or may not belong to your customer profile.'
      : 'We could not find your financial accounts.';
  }
  return context === 'detail'
    ? 'We could not load this account’s details. Please try again.'
    : 'We could not load your financial accounts. Please try again.';
}

export default function FinancialAccounts() {
  const [accounts, setAccounts] = useState<FinancialAccount[]>([]);
  const [loading, setLoading] = useState(true);
  const [listError, setListError] = useState('');
  const [listRetry, setListRetry] = useState(0);
  const [selectedAccountId, setSelectedAccountId] = useState<string | null>(null);
  const [selectedAccount, setSelectedAccount] = useState<FinancialAccount | null>(null);
  const [detailLoading, setDetailLoading] = useState(false);
  const [detailError, setDetailError] = useState('');
  const [detailRetry, setDetailRetry] = useState(0);

  useEffect(() => {
    const controller = new AbortController();
    const token = getAccessToken();
    const customerId = token ? getCustomerIdFromToken(token) : null;

    async function loadAccounts() {
      setLoading(true);
      setListError('');
      if (!token || !customerId) {
        setAccounts([]);
        setListError('We could not identify your customer profile. Please sign in again.');
        setLoading(false);
        return;
      }

      try {
        const response = await fetch(`/api/accounts?customerId=${encodeURIComponent(customerId)}`, {
          headers: { Authorization: `Bearer ${token}`, Accept: 'application/json' },
          signal: controller.signal,
        });
        if (!response.ok) throw new Error(friendlyError(response.status, 'list'));

        const payload: unknown = await response.json();
        if (!Array.isArray(payload) || !payload.every(isFinancialAccount)) {
          throw new Error('The account service returned an unexpected response.');
        }
        setAccounts(payload);
      } catch (cause) {
        if (cause instanceof DOMException && cause.name === 'AbortError') return;
        setListError(
          cause instanceof TypeError
            ? 'Unable to reach the Account Service. Check that it is running and try again.'
            : cause instanceof Error
              ? cause.message
              : 'We could not load your financial accounts. Please try again.',
        );
      } finally {
        if (!controller.signal.aborted) setLoading(false);
      }
    }

    void loadAccounts();
    return () => controller.abort();
  }, [listRetry]);

  async function viewAccount(accountId: string) {
    const token = getAccessToken();
    if (!token) {
      setDetailError('Your session has ended. Please sign in again.');
      setSelectedAccountId(accountId);
      setSelectedAccount(null);
      return;
    }

    setSelectedAccountId(accountId);
    setSelectedAccount(null);
    setDetailLoading(true);
    setDetailError('');

    try {
      const response = await fetch(`/api/accounts/${encodeURIComponent(accountId)}`, {
        headers: { Authorization: `Bearer ${token}`, Accept: 'application/json' },
      });
      if (!response.ok) throw new Error(friendlyError(response.status, 'detail'));

      const payload: unknown = await response.json();
      if (!isFinancialAccount(payload)) {
        throw new Error('The account service returned an unexpected response.');
      }
      setSelectedAccount(payload);
    } catch (cause) {
      setDetailError(
        cause instanceof TypeError
          ? 'Unable to reach the Account Service. Check that it is running and try again.'
          : cause instanceof Error
            ? cause.message
            : 'We could not load this account’s details. Please try again.',
      );
    } finally {
      setDetailLoading(false);
    }
  }

  function closeDetails() {
    setSelectedAccountId(null);
    setSelectedAccount(null);
    setDetailError('');
    setDetailLoading(false);
  }

  return (
    <section className="financial-accounts" aria-labelledby="financial-accounts-title">
      <div className="financial-accounts-heading">
        <div>
          <span className="eyebrow"><span className="eyebrow-dot" /> YOUR MONEY</span>
          <h2 id="financial-accounts-title">Financial accounts</h2>
          <p>Choose an account to see its current balance and details.</p>
        </div>
        {!selectedAccount && !loading && !listError && accounts.length > 0 && (
          <button className="secondary-button" type="button" onClick={() => setListRetry((count) => count + 1)}>
            Refresh
          </button>
        )}
      </div>

      {selectedAccountId ? (
        <div className="form-card account-detail-card">
          <button className="back-link" type="button" onClick={closeDetails}>← All accounts</button>
          {detailLoading && (
            <div className="account-state" role="status" aria-live="polite">
              <div className="profile-loading-indicator" aria-hidden="true" />
              <p>Loading account details…</p>
            </div>
          )}
          {!detailLoading && detailError && (
            <div className="account-state">
              <div className="profile-error-mark" aria-hidden="true">!</div>
              <h3>Account details unavailable</h3>
              <p className="profile-message" role="alert">{detailError}</p>
              <button className="submit-button" type="button" onClick={() => setDetailRetry((count) => count + 1)}>Try again</button>
            </div>
          )}
          {!detailLoading && selectedAccount && (
            <>
              <div className="account-detail-identity">
                <div className="account-icon" aria-hidden="true">₱</div>
                <div className="account-detail-name">
                  <h3>{selectedAccount.name}</h3>
                  <p>{selectedAccount.institutionName || 'Financial account'}</p>
                </div>
                <span className={selectedAccount.status.toLowerCase() === 'active' ? 'profile-status profile-status-active' : 'profile-status'}>
                  {selectedAccount.status.toLowerCase()}
                </span>
              </div>
              <div className="account-balance-label">Current balance</div>
              <p className="account-balance">{formatBalance(selectedAccount.balance, selectedAccount.currency)}</p>
              <dl className="profile-details account-detail-fields">
                <div className="profile-detail">
                  <dt>Account name</dt>
                  <dd>{selectedAccount.name}</dd>
                </div>
                <div className="profile-detail">
                  <dt>Account type</dt>
                  <dd>{selectedAccount.type.replace(/_/g, ' ')}</dd>
                </div>
                <div className="profile-detail">
                  <dt>Currency</dt>
                  <dd>{selectedAccount.currency}</dd>
                </div>
                <div className="profile-detail">
                  <dt>Institution</dt>
                  <dd>{selectedAccount.institutionName || 'Not specified'}</dd>
                </div>
                <div className="profile-detail profile-detail-wide">
                  <dt>Account ID</dt>
                  <dd className="account-id">{selectedAccount.id}</dd>
                </div>
              </dl>
              <p className="profile-privacy-note">Account information is retrieved securely from the Account Service. Only accounts you are authorized to access can be displayed.</p>
            </>
          )}
        </div>
      ) : (
        <>
          {loading && (
            <div className="form-card account-list-state" role="status" aria-live="polite">
              <div className="profile-loading-indicator" aria-hidden="true" />
              <p>Loading your financial accounts…</p>
            </div>
          )}
          {!loading && listError && (
            <div className="form-card account-list-state">
              <div className="profile-error-mark" aria-hidden="true">!</div>
              <h3>Accounts unavailable</h3>
              <p className="profile-message" role="alert">{listError}</p>
              <button className="submit-button" type="button" onClick={() => setListRetry((count) => count + 1)}>Try again</button>
            </div>
          )}
          {!loading && !listError && accounts.length === 0 && (
            <div className="form-card account-list-state">
              <div className="account-empty-icon" aria-hidden="true">＋</div>
              <h3>No financial accounts yet</h3>
              <p className="profile-message">When you add an account, its current balance and details will appear here.</p>
            </div>
          )}
          {!loading && !listError && accounts.length > 0 && (
            <div className="account-list">
              {accounts.map((account) => (
                <article className="account-list-item" key={account.id}>
                  <div className="account-icon" aria-hidden="true">₱</div>
                  <div className="account-list-copy">
                    <h3>{account.name}</h3>
                    <p>{account.institutionName || account.type.replace(/_/g, ' ')}</p>
                    <span className="account-list-type">{account.type.replace(/_/g, ' ')} · {account.currency}</span>
                  </div>
                  <div className="account-list-balance">
                    <strong>{formatBalance(account.balance, account.currency)}</strong>
                    <span className={account.status.toLowerCase() === 'active' ? 'account-status account-status-active' : 'account-status'}>{account.status.toLowerCase()}</span>
                  </div>
                  <button className="account-view-button" type="button" onClick={() => void viewAccount(account.id)} aria-label={`View details for ${account.name}`}>
                    View details <span aria-hidden="true">→</span>
                  </button>
                </article>
              ))}
            </div>
          )}
        </>
      )}
      <span className="visually-hidden" aria-live="polite">{detailRetry > 0 ? 'Retrying account details.' : ''}</span>
    </section>
  );
}
