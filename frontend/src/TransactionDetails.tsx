import { useEffect, useState } from 'react';
import { getAccessToken } from './authSession';

type FinancialTransaction = {
  id: string;
  accountId: string;
  type: string;
  direction: string;
  amount: number | string;
  currency: string;
  merchant: string | null;
  category: string | null;
  description: string | null;
  status: string;
  transactionDate: string;
};

type FinancialAccount = {
  id: string;
  name: string;
  currency: string;
  institutionName: string | null;
};

type TransactionDetailsProps = {
  transactionId: string;
  onBackToTransactions: () => void;
  onViewAccounts: () => void;
  onSignOut: () => void;
};

function isFinancialTransaction(value: unknown): value is FinancialTransaction {
  if (typeof value !== 'object' || value === null) return false;
  const item = value as Record<string, unknown>;
  return typeof item.id === 'string' &&
    typeof item.accountId === 'string' &&
    typeof item.type === 'string' &&
    typeof item.direction === 'string' &&
    (typeof item.amount === 'number' || typeof item.amount === 'string') &&
    typeof item.currency === 'string' &&
    (typeof item.merchant === 'string' || item.merchant === null) &&
    (typeof item.category === 'string' || item.category === null) &&
    (typeof item.description === 'string' || item.description === null) &&
    typeof item.status === 'string' &&
    typeof item.transactionDate === 'string';
}

function isFinancialAccount(value: unknown): value is FinancialAccount {
  if (typeof value !== 'object' || value === null) return false;
  const item = value as Record<string, unknown>;
  return typeof item.id === 'string' &&
    typeof item.name === 'string' &&
    typeof item.currency === 'string' &&
    (typeof item.institutionName === 'string' || item.institutionName === null);
}

function titleCase(value: string | null | undefined): string {
  if (!value) return '—';
  return value.toLowerCase().split('_').map(part => part.charAt(0).toUpperCase() + part.slice(1)).join(' ');
}

function formatAmount(amount: number | string, currency: string, direction: string): string {
  const numeric = typeof amount === 'number' ? amount : Number(amount);
  const prefix = direction.toUpperCase() === 'CREDIT' ? '+' : direction.toUpperCase() === 'DEBIT' ? '−' : '';
  if (!Number.isFinite(numeric)) return `${prefix}${currency} ${String(amount)}`;
  try {
    return `${prefix}${new Intl.NumberFormat(undefined, { style: 'currency', currency, minimumFractionDigits: 2, maximumFractionDigits: 2 }).format(Math.abs(numeric))}`;
  } catch {
    return `${prefix}${currency} ${Math.abs(numeric).toFixed(2)}`;
  }
}

function formatDate(value: string): string {
  const date = new Date(value);
  if (Number.isNaN(date.getTime())) return value;
  return new Intl.DateTimeFormat(undefined, {
    year: 'numeric', month: 'long', day: 'numeric',
    hour: 'numeric', minute: '2-digit', second: '2-digit',
    timeZoneName: 'short',
  }).format(date);
}

function responseError(status: number): string {
  if (status === 401) return 'Your session has expired. Please sign in again.';
  if (status === 403) return 'You do not have permission to view this transaction.';
  if (status === 404) return 'This transaction could not be found or is not available to your account.';
  return 'We could not load the transaction details. Please try again.';
}

export default function TransactionDetails({
  transactionId,
  onBackToTransactions,
  onViewAccounts,
  onSignOut,
}: TransactionDetailsProps) {
  const [transaction, setTransaction] = useState<FinancialTransaction | null>(null);
  const [account, setAccount] = useState<FinancialAccount | null>(null);
  const [loading, setLoading] = useState(true);
  const [error, setError] = useState('');
  const [retry, setRetry] = useState(0);

  useEffect(() => {
    const controller = new AbortController();

    async function loadDetails() {
      setLoading(true);
      setError('');
      setTransaction(null);
      setAccount(null);

      const token = getAccessToken();
      if (!token) {
        setError('Your session has ended. Please sign in again.');
        setLoading(false);
        return;
      }

      try {
        const response = await fetch(`/api/transactions/${encodeURIComponent(transactionId)}`, {
          headers: { Authorization: `Bearer ${token}`, Accept: 'application/json' },
          signal: controller.signal,
        });
        if (!response.ok) throw new Error(responseError(response.status));

        const payload: unknown = await response.json();
        if (!isFinancialTransaction(payload)) {
          throw new Error('The Transaction Service returned an unexpected response.');
        }
        if (payload.id !== transactionId) {
          throw new Error('The Transaction Service returned a different transaction than requested.');
        }
        setTransaction(payload);

        // Account metadata is supplementary; transaction details remain available
        // if the account lookup fails or returns an unexpected payload.
        try {
          const accountResponse = await fetch(`/api/accounts/${encodeURIComponent(payload.accountId)}`, {
            headers: { Authorization: `Bearer ${token}`, Accept: 'application/json' },
            signal: controller.signal,
          });
          if (accountResponse.ok) {
            const accountPayload: unknown = await accountResponse.json();
            if (isFinancialAccount(accountPayload) && accountPayload.id === payload.accountId) {
              setAccount(accountPayload);
            }
          }
        } catch (cause) {
          if (cause instanceof DOMException && cause.name === 'AbortError') return;
          // The transaction API is the source of truth for this screen.
        }
      } catch (cause) {
        if (cause instanceof DOMException && cause.name === 'AbortError') return;
        setError(cause instanceof TypeError
          ? 'Unable to reach the Transaction Service. Check that it is running and try again.'
          : cause instanceof Error ? cause.message : 'We could not load the transaction details.');
      } finally {
        if (!controller.signal.aborted) setLoading(false);
      }
    }

    void loadDetails();
    return () => controller.abort();
  }, [transactionId, retry]);

  const isCredit = transaction?.direction.toUpperCase() === 'CREDIT';

  return (
    <main className="page-shell transactions-page">
      <header className="topbar">
        <a className="brand" href="/" aria-label="Fintech home" onClick={event => event.preventDefault()}>
          <span className="brand-mark" aria-hidden="true"><svg viewBox="0 0 32 32" fill="none"><path d="M7 23.5V8.5h4.2l4.8 7.1 4.8-7.1H25v15h-4.4v-8.1L16 22l-4.6-6.6v8.1H7Z" fill="currentColor" /></svg></span>
          <span>fintech<span className="brand-period">.</span></span>
        </a>
        <nav className="topbar-prompt profile-nav-actions" aria-label="Main navigation">
          <button className="text-link" type="button" onClick={onViewAccounts}>Accounts</button>
          <button className="text-link" type="button" onClick={onBackToTransactions}>Transactions</button>
          <button className="text-link" type="button" onClick={onSignOut}>Sign out</button>
        </nav>
      </header>

      <section className="transactions-content transaction-details-content" aria-labelledby="transaction-details-title">
        <button className="back-link transaction-details-back" type="button" onClick={onBackToTransactions}>← Back to transactions</button>
        <div className="transactions-heading">
          <div>
            <span className="eyebrow"><span className="eyebrow-dot" /> FINANCIAL ACTIVITY</span>
            <h1 id="transaction-details-title">Transaction details<span>.</span></h1>
            <p className="intro-copy">Review the information recorded for this transaction.</p>
          </div>
        </div>

        {loading && (
          <div className="form-card transactions-state" role="status" aria-live="polite">
            <div className="profile-loading-indicator" aria-hidden="true" />
            <p>Loading transaction details…</p>
          </div>
        )}

        {!loading && error && (
          <div className="form-card transactions-state">
            <div className="profile-error-mark" aria-hidden="true">!</div>
            <h2>Transaction unavailable</h2>
            <p className="profile-message" role="alert">{error}</p>
            <button className="submit-button" type="button" onClick={() => setRetry(value => value + 1)}>Try again</button>
          </div>
        )}

        {!loading && !error && transaction && (
          <article className="form-card transaction-detail-card">
            <div className="transaction-detail-hero">
              <span className={isCredit ? 'transaction-symbol transaction-symbol-credit transaction-detail-symbol' : 'transaction-symbol transaction-detail-symbol'} aria-hidden="true">
                {isCredit ? '↓' : '↑'}
              </span>
              <span className="transaction-detail-label">{titleCase(transaction.type)} · {titleCase(transaction.direction)}</span>
              <p className={isCredit ? 'transaction-detail-amount transaction-detail-amount-credit' : 'transaction-detail-amount'}>
                {formatAmount(transaction.amount, transaction.currency, transaction.direction)}
              </p>
              <span className={transaction.status.toUpperCase() === 'COMPLETED' ? 'transaction-status transaction-status-complete' : 'transaction-status'}>
                {titleCase(transaction.status)}
              </span>
              <time className="transaction-detail-date" dateTime={transaction.transactionDate}>
                {formatDate(transaction.transactionDate)}
              </time>
            </div>

            <section className="transaction-detail-section" aria-labelledby="transaction-info-heading">
              <h2 id="transaction-info-heading">Transaction information</h2>
              <dl className="transaction-detail-grid">
                <div><dt>Transaction ID</dt><dd className="transaction-detail-id">{transaction.id}</dd></div>
                <div><dt>Type</dt><dd>{titleCase(transaction.type)}</dd></div>
                <div><dt>Direction</dt><dd>{titleCase(transaction.direction)}</dd></div>
                <div><dt>Category</dt><dd>{titleCase(transaction.category)}</dd></div>
                <div><dt>Status</dt><dd>{titleCase(transaction.status)}</dd></div>
                <div><dt>Transaction date</dt><dd>{formatDate(transaction.transactionDate)}</dd></div>
                <div><dt>Amount</dt><dd>{formatAmount(transaction.amount, transaction.currency, transaction.direction)}</dd></div>
                <div><dt>Currency</dt><dd>{transaction.currency}</dd></div>
                <div><dt>Merchant / payee</dt><dd>{transaction.merchant || 'Not provided'}</dd></div>
                <div className="transaction-detail-grid-wide"><dt>Description</dt><dd className="transaction-detail-description">{transaction.description || 'No description provided'}</dd></div>
              </dl>
            </section>

            <section className="transaction-detail-section" aria-labelledby="transaction-account-heading">
              <h2 id="transaction-account-heading">Linked account</h2>
              <dl className="transaction-detail-grid">
                <div><dt>Account name</dt><dd>{account?.name || 'Account details unavailable'}</dd></div>
                <div><dt>Financial institution</dt><dd>{account?.institutionName || 'Not provided'}</dd></div>
                <div><dt>Account ID</dt><dd className="transaction-detail-id">{transaction.accountId}</dd></div>
              </dl>
            </section>

            <p className="profile-privacy-note transactions-note">Details are retrieved from the authenticated Transaction Service. Access to this transaction must be authorized by the backend.</p>
          </article>
        )}
        <footer className="form-footer">© 2026 Fintech Services <span aria-hidden="true">·</span> Your financial life, in clearer view</footer>
      </section>
    </main>
  );
}
