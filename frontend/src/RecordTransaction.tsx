import { useEffect, useState, type FormEvent } from 'react';
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

type TransactionType = 'DEPOSIT' | 'WITHDRAWAL' | 'TRANSFER' | 'PAYMENT';
type TransactionDirection = 'CREDIT' | 'DEBIT';
type TransactionCategory =
  | 'GROCERIES' | 'DINING' | 'TRANSPORTATION' | 'SHOPPING' | 'UTILITIES'
  | 'ENTERTAINMENT' | 'HEALTHCARE' | 'TRAVEL' | 'HOUSING' | 'SALARY'
  | 'TRANSFER' | 'FEES' | 'OTHER';

type TransactionResponse = {
  id: string;
  accountId: string;
  type: TransactionType;
  direction: TransactionDirection;
  amount: number | string;
  currency: string;
  merchant: string | null;
  category: TransactionCategory;
  description: string | null;
  status: string;
  transactionDate: string;
};

type RecordTransactionProps = {
  onBackToTransactions: () => void;
  onViewAccounts: () => void;
  onSignOut: () => void;
};

const categories: TransactionCategory[] = [
  'GROCERIES', 'DINING', 'TRANSPORTATION', 'SHOPPING', 'UTILITIES',
  'ENTERTAINMENT', 'HEALTHCARE', 'TRAVEL', 'HOUSING', 'SALARY',
  'TRANSFER', 'FEES', 'OTHER',
];

function isAccount(value: unknown): value is FinancialAccount {
  if (typeof value !== 'object' || value === null) return false;
  const item = value as Record<string, unknown>;
  return typeof item.id === 'string' && typeof item.customerId === 'string' &&
    typeof item.name === 'string' && typeof item.type === 'string' &&
    typeof item.currency === 'string' &&
    (typeof item.balance === 'number' || typeof item.balance === 'string') &&
    typeof item.status === 'string' &&
    (typeof item.institutionName === 'string' || item.institutionName === null);
}

function titleCase(value: string): string {
  return value.toLowerCase().split('_').map(part => part.charAt(0).toUpperCase() + part.slice(1)).join(' ');
}

function localDateTimeValue(date = new Date()): string {
  const local = new Date(date.getTime() - date.getTimezoneOffset() * 60_000);
  return local.toISOString().slice(0, 16);
}

function formatAmount(amount: number | string, currency: string): string {
  const numeric = typeof amount === 'number' ? amount : Number(amount);
  if (!Number.isFinite(numeric)) return `${currency} ${String(amount)}`;
  try {
    return new Intl.NumberFormat(undefined, { style: 'currency', currency }).format(numeric);
  } catch {
    return `${currency} ${numeric.toFixed(2)}`;
  }
}

export default function RecordTransaction({
  onBackToTransactions,
  onViewAccounts,
  onSignOut,
}: RecordTransactionProps) {
  const [accounts, setAccounts] = useState<FinancialAccount[]>([]);
  const [accountId, setAccountId] = useState('');
  const [type, setType] = useState<TransactionType>('PAYMENT');
  const [direction, setDirection] = useState<TransactionDirection>('DEBIT');
  const [amount, setAmount] = useState('');
  const [category, setCategory] = useState<TransactionCategory | ''>('');
  const [merchant, setMerchant] = useState('');
  const [description, setDescription] = useState('');
  const [transactionDate, setTransactionDate] = useState(localDateTimeValue());
  const [loadingAccounts, setLoadingAccounts] = useState(true);
  const [submitting, setSubmitting] = useState(false);
  const [error, setError] = useState('');
  const [success, setSuccess] = useState('');

  useEffect(() => {
    const controller = new AbortController();
    async function loadAccounts() {
      const token = getAccessToken();
      if (!token) {
        setError('Your session has ended. Please sign in again.');
        setLoadingAccounts(false);
        return;
      }
      try {
        const response = await fetch('/api/accounts', {
          headers: { Authorization: `Bearer ${token}`, Accept: 'application/json' },
          signal: controller.signal,
        });
        if (response.status === 401 || response.status === 403) {
          throw new Error('Your session is no longer valid. Please sign in again.');
        }
        if (!response.ok) throw new Error('We could not load your accounts. Please try again.');
        const payload: unknown = await response.json();
        if (!Array.isArray(payload) || !payload.every(isAccount)) {
          throw new Error('The Account Service returned an unexpected response.');
        }
        const activeAccounts = payload.filter((account: FinancialAccount) => account.status.toUpperCase() === 'ACTIVE');
        setAccounts(activeAccounts);
        setAccountId(activeAccounts[0]?.id ?? '');
        if (activeAccounts.length === 0) setError('You need an active financial account before you can record a transaction.');
      } catch (cause) {
        if (cause instanceof DOMException && cause.name === 'AbortError') return;
        setError(cause instanceof TypeError
          ? 'Unable to reach the Account Service. Check that it is running and try again.'
          : cause instanceof Error ? cause.message : 'We could not load your accounts.');
      } finally {
        if (!controller.signal.aborted) setLoadingAccounts(false);
      }
    }
    void loadAccounts();
    return () => controller.abort();
  }, []);

  const selectedAccount = accounts.find(account => account.id === accountId);

  function handleTypeChange(value: TransactionType) {
    setType(value);
    if (value === 'DEPOSIT') setDirection('CREDIT');
    else if (value === 'WITHDRAWAL' || value === 'PAYMENT') setDirection('DEBIT');
  }

  async function handleSubmit(event: FormEvent<HTMLFormElement>) {
    event.preventDefault();
    setError('');
    setSuccess('');

    const numericAmount = Number(amount);
    if (!accountId) {
      setError('Select an active account.');
      return;
    }
    if (!Number.isFinite(numericAmount) || numericAmount <= 0) {
      setError('Enter an amount greater than zero.');
      return;
    }
    if (description.trim().length > 255) {
      setError('Description must be 255 characters or fewer.');
      return;
    }
    if (merchant.trim().length > 150) {
      setError('Merchant or payee must be 150 characters or fewer.');
      return;
    }

    const token = getAccessToken();
    if (!token) {
      setError('Your session has ended. Please sign in again.');
      return;
    }

    setSubmitting(true);
    try {
      const response = await fetch('/api/transactions', {
        method: 'POST',
        headers: {
          Authorization: `Bearer ${token}`,
          'Content-Type': 'application/json',
          Accept: 'application/json',
        },
        body: JSON.stringify({
          accountId,
          type,
          direction,
          amount: numericAmount,
          currency: selectedAccount?.currency,
          merchant: merchant.trim() || null,
          category: category || null,
          description: description.trim() || null,
          transactionDate: transactionDate ? new Date(transactionDate).toISOString() : null,
        }),
      });
      const payload: unknown = await response.json().catch(() => null);
      if (!response.ok) {
        if (response.status === 401 || response.status === 403) {
          throw new Error('Your session is no longer valid or you do not have permission to record a transaction.');
        }
        if (response.status === 400) {
          throw new Error('Please check the transaction details and try again.');
        }
        if (response.status === 404) {
          throw new Error('The selected account could not be found. Refresh the page and choose another account.');
        }
        throw new Error('The transaction could not be saved. Please try again.');
      }
      if (typeof payload !== 'object' || payload === null ||
          !('id' in payload) || typeof payload.id !== 'string' ||
          !('status' in payload) || typeof payload.status !== 'string') {
        throw new Error('The service returned an unexpected response. Check transaction history before retrying.');
      }
      const saved = payload as TransactionResponse;
      setSuccess(`Transaction recorded successfully as ${titleCase(saved.category)} (${saved.id}).`);
      setAmount('');
      setCategory('');
      setMerchant('');
      setDescription('');
      setTransactionDate(localDateTimeValue());
    } catch (cause) {
      setError(cause instanceof TypeError
        ? 'Unable to reach the Transaction Service. Check that it is running and try again.'
        : cause instanceof Error ? cause.message : 'The transaction could not be saved.');
    } finally {
      setSubmitting(false);
    }
  }

  return (
    <main className="page-shell record-transaction-page">
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

      <section className="record-transaction-content" aria-labelledby="record-transaction-title">
        <div className="transactions-heading">
          <div>
            <span className="eyebrow"><span className="eyebrow-dot" /> FINANCIAL ACTIVITY</span>
            <h1 id="record-transaction-title">Record a transaction<span>.</span></h1>
            <p className="intro-copy">Add a deposit, withdrawal, transfer, or payment to an account you own.</p>
          </div>
          <button className="secondary-button" type="button" onClick={onBackToTransactions}>Back to transactions</button>
        </div>

        {loadingAccounts ? (
          <div className="form-card record-transaction-state" role="status"><div className="profile-loading-indicator" aria-hidden="true" /><p>Loading your accounts…</p></div>
        ) : (
          <form className="form-card record-transaction-form" onSubmit={handleSubmit} noValidate>
            {error && <p className="record-feedback record-feedback-error" role="alert">{error}</p>}
            {success && <p className="record-feedback record-feedback-success" role="status">{success} You can view it in transaction history.</p>}

            <div className="record-form-grid">
              <label className="record-field record-field-wide">
                <span>Financial account <span aria-hidden="true">*</span></span>
                <select required value={accountId} onChange={event => setAccountId(event.target.value)} disabled={accounts.length === 0 || submitting}>
                  <option value="">Select an account</option>
                  {accounts.map(account => <option key={account.id} value={account.id}>{account.name} · {account.currency} · {account.institutionName || 'Personal account'}</option>)}
                </select>
                {selectedAccount && <small>Current balance: {formatAmount(selectedAccount.balance, selectedAccount.currency)}</small>}
              </label>

              <label className="record-field">
                <span>Transaction type <span aria-hidden="true">*</span></span>
                <select required value={type} onChange={event => handleTypeChange(event.target.value as TransactionType)} disabled={submitting}>
                  <option value="PAYMENT">Payment</option><option value="DEPOSIT">Deposit</option><option value="WITHDRAWAL">Withdrawal</option><option value="TRANSFER">Transfer</option>
                </select>
              </label>

              <label className="record-field">
                <span>Direction <span aria-hidden="true">*</span></span>
                <select required value={direction} onChange={event => setDirection(event.target.value as TransactionDirection)} disabled={submitting}>
                  <option value="DEBIT">Debit — money out</option><option value="CREDIT">Credit — money in</option>
                </select>
              </label>

              <label className="record-field">
                <span>Amount ({selectedAccount?.currency ?? 'currency'}) <span aria-hidden="true">*</span></span>
                <input required type="number" min="0.01" step="0.01" inputMode="decimal" value={amount} onChange={event => setAmount(event.target.value)} placeholder="0.00" disabled={submitting} />
              </label>

              <label className="record-field">
                <span>Category</span>
                <select value={category} onChange={event => setCategory(event.target.value as TransactionCategory | '')} disabled={submitting}>
                  <option value="">Auto-categorize from merchant and description</option>
                  {categories.map(item => <option key={item} value={item}>{titleCase(item)}</option>)}
                </select>
                <small>Choose a category yourself, or let the service suggest one from the merchant and description. You can review the saved category in transaction history.</small>
              </label>

              <label className="record-field record-field-wide">
                <span>Merchant or payee <span className="record-optional">Optional</span></span>
                <input type="text" maxLength={150} value={merchant} onChange={event => setMerchant(event.target.value)} placeholder="e.g. Grocery store or employer" disabled={submitting} />
              </label>

              <label className="record-field record-field-wide">
                <span>Description <span className="record-optional">Optional</span></span>
                <textarea maxLength={255} rows={3} value={description} onChange={event => setDescription(event.target.value)} placeholder="Add a note about this transaction" disabled={submitting} />
                <small>{description.length}/255 characters</small>
              </label>

              <label className="record-field record-field-wide">
                <span>Transaction date and time <span aria-hidden="true">*</span></span>
                <input required type="datetime-local" value={transactionDate} onChange={event => setTransactionDate(event.target.value)} disabled={submitting} />
              </label>
            </div>

            <div className="record-form-actions">
              <button className="secondary-button" type="button" onClick={onBackToTransactions} disabled={submitting}>Cancel</button>
              <button className="submit-button" type="submit" disabled={submitting || accounts.length === 0}>
                {submitting ? 'Saving transaction…' : 'Record transaction'}
              </button>
            </div>
            <p className="record-transaction-disclaimer">Recording an entry adds it to transaction history. It does not initiate a real bank transfer or update your account balance.</p>
          </form>
        )}
        <footer className="form-footer">© 2026 Fintech Services <span aria-hidden="true">·</span> Your financial life, in clearer view</footer>
      </section>
    </main>
  );
}
