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
  onViewTransaction: (transactionId: string) => void;
  onSignOut: () => void;
  onManageCategories: () => void;
};

const transactionCategories = [
  'GROCERIES', 'DINING', 'TRANSPORTATION', 'SHOPPING', 'UTILITIES',
  'ENTERTAINMENT', 'HEALTHCARE', 'TRAVEL', 'HOUSING', 'SALARY',
  'TRANSFER', 'FEES', 'OTHER',
] as const;

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
  onViewTransaction,
  onSignOut,
  onManageCategories,
}: FinancialTransactionsProps) {
  const [transactions, setTransactions] = useState<TransactionRow[]>([]);
  const [accounts, setAccounts] = useState<FinancialAccount[]>([]);
  const [loading, setLoading] = useState(true);
  const [error, setError] = useState('');
  const [retry, setRetry] = useState(0);
  const [selectedAccountId, setSelectedAccountId] = useState('all');
  const [selectedCategory, setSelectedCategory] = useState('all');
  const [spendingPeriod, setSpendingPeriod] = useState<'month' | '30days' | 'all'>('month');
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

  const spendingSummary = useMemo(() => {
    const now = new Date();
    const periodStart = new Date(now.getFullYear(), now.getMonth(), 1);
    if (spendingPeriod === '30days') {
      periodStart.setTime(now.getTime());
      periodStart.setDate(periodStart.getDate() - 29);
      periodStart.setHours(0, 0, 0, 0);
    }

    const expenses = transactions.filter(transaction => {
      if (selectedAccountId !== 'all' && transaction.accountId !== selectedAccountId) return false;
      if (transaction.direction.toUpperCase() !== 'DEBIT') return false;
      if (transaction.type.toUpperCase() === 'TRANSFER' || (transaction.category ?? 'OTHER').toUpperCase() === 'TRANSFER') return false;
      if ((transaction.category ?? 'OTHER').toUpperCase() === 'SALARY') return false;
      if (spendingPeriod !== 'all') {
        const transactionDate = new Date(transaction.transactionDate);
        if (Number.isNaN(transactionDate.getTime()) || transactionDate < periodStart || transactionDate > now) return false;
      }
      const amount = typeof transaction.amount === 'number' ? transaction.amount : Number(transaction.amount);
      return Number.isFinite(amount) && amount >= 0;
    });

    const totals = new Map<string, { currency: string; total: number; categories: Map<string, { amount: number; count: number }> }>();
    for (const transaction of expenses) {
      const currency = transaction.currency.toUpperCase();
      const category = (transaction.category ?? 'OTHER').toUpperCase();
      const amount = Math.abs(typeof transaction.amount === 'number' ? transaction.amount : Number(transaction.amount));
      let currencyGroup = totals.get(currency);
      if (!currencyGroup) {
        currencyGroup = { currency, total: 0, categories: new Map() };
        totals.set(currency, currencyGroup);
      }
      currencyGroup.total += amount;
      const categoryGroup = currencyGroup.categories.get(category) ?? { amount: 0, count: 0 };
      categoryGroup.amount += amount;
      categoryGroup.count += 1;
      currencyGroup.categories.set(category, categoryGroup);
    }

    return Array.from(totals.values()).map(group => ({
      currency: group.currency,
      total: group.total,
      categories: Array.from(group.categories.entries())
        .map(([category, values]) => ({
          category,
          amount: values.amount,
          count: values.count,
          share: group.total > 0 ? (values.amount / group.total) * 100 : 0,
        }))
        .sort((a, b) => b.amount - a.amount),
    })).sort((a, b) => a.currency.localeCompare(b.currency));
  }, [transactions, selectedAccountId, spendingPeriod]);

  const filteredTransactions = useMemo(() => {
    const query = searchTerm.trim().toLowerCase();
    return transactions.filter(transaction => {
      const matchesAccount = selectedAccountId === 'all' || transaction.accountId === selectedAccountId;
      const matchesCategory = selectedCategory === 'all' || (transaction.category ?? 'OTHER') === selectedCategory;
      const haystack = [
        transaction.merchant,
        transaction.description,
        transaction.category,
        transaction.type,
        transaction.direction,
        transaction.status,
        transaction.accountName,
      ].filter(Boolean).join(' ').toLowerCase();
      return matchesAccount && matchesCategory && (!query || haystack.includes(query));
    });
  }, [transactions, selectedAccountId, selectedCategory, searchTerm]);

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
          <button className="text-link" type="button" onClick={onManageCategories}>Spending categories</button>
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
            <section className="form-card spending-summary-card" aria-labelledby="spending-summary-title">
              <div className="spending-summary-heading">
                <div>
                  <span className="form-step">SPENDING OVERVIEW</span>
                  <h2 id="spending-summary-title">Spending by category</h2>
                  <p>See where your money goes, based on recorded debits. Transfers and income are excluded.</p>
                </div>
                <label className="transaction-filter spending-period-filter">
                  <span>Period</span>
                  <select value={spendingPeriod} onChange={event => setSpendingPeriod(event.target.value as 'month' | '30days' | 'all')}>
                    <option value="month">This month</option>
                    <option value="30days">Last 30 days</option>
                    <option value="all">All time</option>
                  </select>
                </label>
              </div>
              {spendingSummary.length === 0 ? (
                <div className="spending-summary-empty">
                  <span className="spending-summary-empty-icon" aria-hidden="true">↗</span>
                  <h3>No spending to summarize yet</h3>
                  <p>Record an expense for this period, or choose a wider period to see your category breakdown.</p>
                </div>
              ) : (
                <div className="spending-currency-groups">
                  {spendingSummary.map(group => (
                    <section className="spending-currency-group" key={group.currency} aria-label={`Spending in ${group.currency}`}>
                      <div className="spending-total-row">
                        <span>Total spending <small>{group.currency}</small></span>
                        <strong>{formatAmount(group.total, group.currency, '')}</strong>
                      </div>
                      <div className="spending-category-list">
                        {group.categories.map(item => (
                          <div className="spending-category-row" key={item.category}>
                            <div className="spending-category-label">
                              <strong>{titleCase(item.category)}</strong>
                              <span>{item.count} {item.count === 1 ? 'transaction' : 'transactions'} · {item.share.toFixed(1)}%</span>
                            </div>
                            <strong className="spending-category-amount">{formatAmount(item.amount, group.currency, '')}</strong>
                            <div className="spending-category-track" role="progressbar" aria-label={`${titleCase(item.category)} share of spending`} aria-valuemin={0} aria-valuemax={100} aria-valuenow={Math.round(item.share)}>
                              <span style={{ width: `${item.share}%` }} />
                            </div>
                          </div>
                        ))}
                      </div>
                    </section>
                  ))}
                </div>
              )}
              <p className="spending-summary-note">Amounts are grouped by currency; different currencies are never combined into one total. Category results depend on the category saved with each transaction.</p>
            </section>

            <div className="transactions-toolbar">
              <label className="transaction-filter">
                <span>Account</span>
                <select value={selectedAccountId} onChange={event => setSelectedAccountId(event.target.value)}>
                  <option value="all">All accounts</option>
                  {accounts.map(account => <option key={account.id} value={account.id}>{account.name}</option>)}
                </select>
              </label>
              <label className="transaction-filter">
                <span>Category</span>
                <select value={selectedCategory} onChange={event => setSelectedCategory(event.target.value)}>
                  <option value="all">All categories</option>
                  {transactionCategories.map(category => <option key={category} value={category}>{titleCase(category)}</option>)}
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
                {(selectedAccountId !== 'all' || selectedCategory !== 'all' || searchTerm) && (
                  <button className="secondary-button" type="button" onClick={() => { setSelectedAccountId('all'); setSelectedCategory('all'); setSearchTerm(''); }}>Clear filters</button>
                )}
              </div>
            ) : (
              <section className="form-card transactions-card" aria-label="Transaction history">
                <div className="transactions-table-wrap">
                  <table className="transactions-table">
                    <thead><tr><th scope="col">Activity</th><th scope="col">Account</th><th scope="col">Date</th><th scope="col">Status</th><th scope="col" className="transaction-amount-heading">Amount</th><th scope="col"><span className="visually-hidden">Details</span></th></tr></thead>
                    <tbody>
                      {filteredTransactions.map(transaction => {
                        const isCredit = transaction.direction.toUpperCase() === 'CREDIT';
                        const activityTitle = transaction.merchant || transaction.description || titleCase(transaction.type);
                        return (
                          <tr key={transaction.id}>
                            <td>
                              <div className="transaction-activity">
                                <span className={isCredit ? 'transaction-symbol transaction-symbol-credit' : 'transaction-symbol'} aria-hidden="true">{isCredit ? '↓' : '↑'}</span>
                                <span className="transaction-activity-copy"><button className="transaction-detail-link" type="button" onClick={() => onViewTransaction(transaction.id)}>{activityTitle}</button><small>{titleCase(transaction.category)} · {titleCase(transaction.type)} / {titleCase(transaction.direction)}</small>{transaction.description && transaction.description !== activityTitle && <small>{transaction.description}</small>}</span>
                              </div>
                            </td>
                            <td><span className="transaction-account-name">{transaction.accountName}</span></td>
                            <td><time dateTime={transaction.transactionDate}>{formatDate(transaction.transactionDate)}</time></td>
                            <td><span className={transaction.status.toLowerCase() === 'completed' ? 'transaction-status transaction-status-complete' : 'transaction-status'}>{titleCase(transaction.status)}</span></td>
                            <td className={isCredit ? 'transaction-amount transaction-amount-credit' : 'transaction-amount'}>{formatAmount(transaction.amount, transaction.currency, transaction.direction)}</td>
                            <td><button className="transaction-view-details" type="button" onClick={() => onViewTransaction(transaction.id)} aria-label={`View details for ${activityTitle}`}>Details <span aria-hidden="true">→</span></button></td>
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
