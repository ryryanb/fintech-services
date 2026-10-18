import { useCallback, useEffect, useMemo, useState } from 'react';
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

type FinancialTransactionsProps = {
  onBackToProfile: () => void;
  onViewAccounts: () => void;
  onRecordTransaction: () => void;
  onSignOut: () => void;
};

type TransactionRow = FinancialTransaction & {
  accountName: string;
};

function getCustomerIdFromToken(token: string): string | null {
  try {
    const encodedPayload = token.split('.')[1];
    if (!encodedPayload) return null;
    const base64 = encodedPayload.replace(/-/g, '+').replace(/_/g, '/');
    const decoded = atob(base64.padEnd(base64.length + ((4 - (base64.length % 4)) % 4), '='));
    const claims: unknown = JSON.parse(decoded);
    if (typeof claims !== 'object' || claims === null || !('customerId' in claims)) return null;
    const customerId = claims.customerId;
    return typeof customerId === 'string' && customerId.length > 0 ? customerId : null;
  } catch {
    return null;
  }
}

function isFinancialAccount(value: unknown): value is FinancialAccount {
  if (typeof value !== 'object' || value === null) return false;
  const account = value as Record<string, unknown>;
  return typeof account.id === 'string' &&
    typeof account.customerId === 'string' &&
    typeof account.name === 'string' &&
    typeof account.type === 'string' &&
    typeof account.currency === 'string' &&
    (typeof account.balance === 'number' || typeof account.balance === 'string') &&
    typeof account.status === 'string' &&
    (typeof account.institutionName === 'string' || account.institutionName === null);
}

function isFinancialTransaction(value: unknown): value is FinancialTransaction {
  if (typeof value !== 'object' || value === null) return false;
  const transaction = value as Record<string, unknown>;
  return typeof transaction.id === 'string' &&
    typeof transaction.accountId === 'string' &&
    typeof transaction.type === 'string' &&
    typeof transaction.direction === 'string' &&
    (typeof transaction.amount === 'number' || typeof transaction.amount === 'string') &&
    typeof transaction.currency === 'string' &&
    (typeof transaction.merchant === 'string' || transaction.merchant === null) &&
    (typeof transaction.category === 'string' || transaction.category === null) &&
    (typeof transaction.description === 'string' || transaction.description === null) &&
    typeof transaction.status === 'string' &&
    typeof transaction.transactionDate === 'string';
}

function formatAmount(amount: number | string, currency: string, direction: string): string {
  const numericAmount = typeof amount === 'number' ? amount : Number(amount);
  const prefix = direction.toUpperCase() === 'CREDIT' ? '+' : direction.toUpperCase() === 'DEBIT' ? '−' : '';
  if (!Number.isFinite(numericAmount)) return `${prefix}${currency} ${String(amount)}`;
  try {
    const formatted = new Intl.NumberFormat(undefined, {
      style: 'currency',
      currency,
      minimumFractionDigits: 2,
      maximumFractionDigits: 2,
    }).format(Math.abs(numericAmount));
    return `${prefix}${formatted}`;
  } catch {
    return `${prefix}${currency} ${Math.abs(numericAmount).toFixed(2)}`;
  }
}

function formatDate(value: string): string {
  const date = new Date(value);
  if (Number.isNaN(date.getTime())) return value;
  return new Intl.DateTimeFormat(undefined, {
    year: 'numeric',
    month: 'short',
    day: 'numeric',
    hour: 'numeric',
    minute: '2-digit',
  }).format(date);
}

function titleCase(value: string | null | undefined): string {
  if (!value) return '—';
  return value.toLowerCase().split('_').map(part => part.charAt(0).toUpperCase() + part.slice(1)).join(' ');
}

function responseError(status: number): string {
  if (status === 401 || status === 403) {
    return 'Your session is no longer valid or you do not have permission to view these transactions. Please sign in again.';
  }
  if (status === 404) return 'The requested account or transaction history could not be found.';
  return 'We could not load your transactions. Please try again.';
}

export default function FinancialTransactions({
  onBackToProfile,
  onViewAccounts,
  onRecordTransaction,
  onSignOut,
}: FinancialTransactionsProps) {
  const [transactions, setTransactions] = useState<TransactionRow[]>([]);
  const [accounts, setAccounts] = useState<FinancialAccount[]>([]);
  const [loading, setLoading] = useState(true);
  const [error, setError] = useState('');
  const [retry, setRetry] = useState(0);
  const [selectedAccountId, setSelectedAccountId] = useState('all');
  const [searchTerm, setSearchTerm] = useState('');

  const loadTransactions = useCallback(async (signal: AbortSignal) => {
    const token = getAccessToken();
    const customerId = token ? getCustomerIdFromToken(token) : null;
    if (!token || !customerId) {
      throw new Error('Your session has ended or your customer profile could not be identified. Please sign in again.');
    }

    const accountResponse = await fetch(`/api/accounts?customerId=${encodeURIComponent(customerId)}`, {
      headers: { Authorization: `Bearer ${token}`, Accept: 'application/json' },
      signal,
    });
    if (!accountResponse.ok) throw new Error(responseError(accountResponse.status));
    const accountPayload: unknown = await accountResponse.json();
    if (!Array.isArray(accountPayload) || !accountPayload.every(isFinancialAccount)) {
      throw new Error('The Account Service returned an unexpected response.');
    }
    const customerAccounts = accountPayload as FinancialAccount[];
    setAccounts(customerAccounts);

    const results = await Promise.all(customerAccounts.map(async account => {
      const response = await fetch(`/api/transactions?accountId=${encodeURIComponent(account.id)}`, {
        headers: { Authorization: `Bearer ${token}`, Accept: 'application/json' },
        signal,
      });
      if (!response.ok) throw new Error(responseError(response.status));
      const payload: unknown = await response.json();
      if (!Array.isArray(payload) || !payload.every(isFinancialTransaction)) {
        throw new Error('The Transaction Service returned an unexpected response.');
      }
      return (payload as FinancialTransaction[]).map(transaction => ({
        ...transaction,
        accountName: account.name,
      }));
    }));

    setTransactions(results.flat().sort((a, b) =>
      new Date(b.transactionDate).getTime() - new Date(a.transactionDate).getTime()
    ));
  }, []);

  useEffect(() => {
    const controller = new AbortController();
    setLoading(true);
    setError('');
    void loadTransactions(controller.signal)
      .catch(cause => {
        if (cause instanceof DOMException && cause.name === 'AbortError') return;
        setTransactions([]);
        setError(cause instanceof TypeError
          ? 'Unable to reach the Account or Transaction Service. Check that both services are running.'
          : cause instanceof Error ? cause.message : 'We could not load your transactions.');
      })
      .finally(() => {
        if (!controller.signal.aborted) setLoading(false);
      });
    return () => controller.abort();
  }, [loadTransactions, retry]);

  const filteredTransactions = useMemo(() => {
    const query = searchTerm.trim().toLowerCase();
    return transactions.filter(transaction => {
      const matchesAccount = selectedAccountId === 'all' || transaction.accountId === selectedAccountId;
      const haystack = [
        transaction.merchant,
        transaction.description,
        transaction.category,
        transaction.type,
        transaction.direction,
        transaction.status,
        transaction.accountName,
      ].filter(Boolean).join(' ').toLowerCase();
      return matchesAccount && (!query || haystack.includes(query));
    });
  }, [transactions, selectedAccountId, searchTerm]);

  return (
    <main className="page-shell transactions-page">
      <header className="topbar">
        <a className="brand" href="/" aria-label="Fintech home" onClick={event => event.preventDefault()}>
          <span className="brand-mark" aria-hidden="true"><svg viewBox="0 0 32 32" fill="none"><path d="M7 23.5V8.5h4.2l4.8 7.1 4.8-7.1H25v15h-4.4v-8.1L16 22l-4.6-6.6v8.1H7Z" fill="currentColor" /></svg></span>
          <span>fintech<span className="brand-period">.</span></span>
        </a>
        <nav className="topbar-prompt profile-nav-actions" aria-label="Main navigation">
          <button className="text-link" type="button" onClick={onBackToProfile}>Profile</button>
          <button className="text-link" type="button" onClick={onViewAccounts}>Accounts</button>
          <button className="text-link" type="button" onClick={onRecordTransaction}>Record transaction</button>
          <button className="text-link" type="button" onClick={onSignOut}>Sign out</button>
        </nav>
      </header>

      <section className="transactions-content" aria-labelledby="transactions-title">
        <div className="transactions-heading">
          <div>
            <span className="eyebrow"><span className="eyebrow-dot" /> YOUR MONEY</span>
            <h1 id="transactions-title">Transactions<span>.</span></h1>
            <p className="intro-copy">A clear view of your recent financial activity across your accounts.</p>
          </div>
          {!loading && !error && (
            <div className="transactions-heading-actions"><button className="secondary-button" type="button" onClick={() => setRetry(count => count + 1)}>Refresh activity</button><button className="submit-button transaction-record-button" type="button" onClick={onRecordTransaction}>Record transaction</button></div>
          )}
        </div>

        {loading && (
          <div className="form-card transactions-state" role="status" aria-live="polite">
            <div className="profile-loading-indicator" aria-hidden="true" />
            <p>Loading your transactions…</p>
          </div>
        )}
        {!loading && error && (
          <div className="form-card transactions-state">
            <div className="profile-error-mark" aria-hidden="true">!</div>
            <h2>Transactions unavailable</h2>
            <p className="profile-message" role="alert">{error}</p>
            <button className="submit-button" type="button" onClick={() => setRetry(count => count + 1)}>Try again</button>
          </div>
        )}
        {!loading && !error && (
          <>
            <div className="transactions-toolbar">
              <label className="transaction-filter">
                <span>Account</span>
                <select value={selectedAccountId} onChange={event => setSelectedAccountId(event.target.value)}>
                  <option value="all">All accounts</option>
                  {accounts.map(account => <option key={account.id} value={account.id}>{account.name}</option>)}
                </select>
              </label>
              <label className="transaction-search">
                <span>Search activity</span>
                <input value={searchTerm} onChange={event => setSearchTerm(event.target.value)} placeholder="Merchant, category, description…" />
              </label>
              <div className="transaction-count"><strong>{filteredTransactions.length}</strong><span>{filteredTransactions.length === 1 ? 'transaction' : 'transactions'}</span></div>
            </div>

            {filteredTransactions.length === 0 ? (
              <div className="form-card transactions-state">
                <div className="account-empty-icon" aria-hidden="true">↔</div>
                <h2>{transactions.length === 0 ? 'No transactions yet' : 'No matching transactions'}</h2>
                <p className="profile-message">
                  {transactions.length === 0
                    ? 'When activity is recorded on your accounts, it will appear here.'
                    : 'Try a different account or search term.'}
                </p>
                {(selectedAccountId !== 'all' || searchTerm) && (
                  <button className="secondary-button" type="button" onClick={() => { setSelectedAccountId('all'); setSearchTerm(''); }}>Clear filters</button>
                )}
              </div>
            ) : (
              <section className="form-card transactions-card" aria-label="Transaction history">
                <div className="transactions-table-wrap">
                  <table className="transactions-table">
                    <thead><tr><th scope="col">Activity</th><th scope="col">Account</th><th scope="col">Date</th><th scope="col">Status</th><th scope="col" className="transaction-amount-heading">Amount</th></tr></thead>
                    <tbody>
                      {filteredTransactions.map(transaction => {
                        const isCredit = transaction.direction.toUpperCase() === 'CREDIT';
                        const activityTitle = transaction.merchant || transaction.description || titleCase(transaction.type);
                        return (
                          <tr key={transaction.id}>
                            <td>
                              <div className="transaction-activity">
                                <span className={isCredit ? 'transaction-symbol transaction-symbol-credit' : 'transaction-symbol'} aria-hidden="true">{isCredit ? '↓' : '↑'}</span>
                                <span className="transaction-activity-copy"><strong>{activityTitle}</strong><small>{titleCase(transaction.category)} · {titleCase(transaction.type)} / {titleCase(transaction.direction)}</small>{transaction.description && transaction.description !== activityTitle && <small>{transaction.description}</small>}</span>
                              </div>
                            </td>
                            <td><span className="transaction-account-name">{transaction.accountName}</span></td>
                            <td><time dateTime={transaction.transactionDate}>{formatDate(transaction.transactionDate)}</time></td>
                            <td><span className={transaction.status.toLowerCase() === 'completed' ? 'transaction-status transaction-status-complete' : 'transaction-status'}>{titleCase(transaction.status)}</span></td>
                            <td className={isCredit ? 'transaction-amount transaction-amount-credit' : 'transaction-amount'}>{formatAmount(transaction.amount, transaction.currency, transaction.direction)}</td>
                          </tr>
                        );
                      })}
                    </tbody>
                  </table>
                </div>
              </section>
            )}
            <p className="profile-privacy-note transactions-note">Transaction history is loaded for accounts returned by your authenticated account list. The Transaction Service must verify ownership for each requested account.</p>
          </>
        )}
        <footer className="form-footer">© 2026 Fintech Services <span aria-hidden="true">·</span> Your financial life, in clearer view</footer>
      </section>
    </main>
  );
}
