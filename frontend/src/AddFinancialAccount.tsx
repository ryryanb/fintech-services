import { useState, type FormEvent } from 'react';
import { getAccessToken } from './authSession';

type AddFinancialAccountProps = {
  onBackToAccounts: () => void;
  onSignOut: () => void;
};

type AccountType =
  | 'CASH'
  | 'BANK_ACCOUNT'
  | 'CREDIT_CARD'
  | 'DEBIT_CARD'
  | 'INVESTMENT'
  | 'LOAN'
  | 'OTHER';

const accountTypes: { value: AccountType; label: string }[] = [
  { value: 'BANK_ACCOUNT', label: 'Bank account' },
  { value: 'CASH', label: 'Cash' },
  { value: 'CREDIT_CARD', label: 'Credit card' },
  { value: 'DEBIT_CARD', label: 'Debit card' },
  { value: 'INVESTMENT', label: 'Investment' },
  { value: 'LOAN', label: 'Loan' },
  { value: 'OTHER', label: 'Other' },
];

function getCustomerIdFromToken(token: string): string | null {
  try {
    const encodedPayload = token.split('.')[1];
    if (!encodedPayload) return null;
    const base64 = encodedPayload.replace(/-/g, '+').replace(/_/g, '/');
    const decoded = atob(base64.padEnd(base64.length + ((4 - (base64.length % 4)) % 4), '='));
    const claims: unknown = JSON.parse(decoded);
    if (
      typeof claims === 'object' &&
      claims !== null &&
      'customerId' in claims &&
      typeof claims.customerId === 'string' &&
      claims.customerId.length > 0
    ) {
      return claims.customerId;
    }
    return null;
  } catch {
    return null;
  }
}


export default function AddFinancialAccount({ onBackToAccounts, onSignOut }: AddFinancialAccountProps) {
  const [name, setName] = useState('');
  const [type, setType] = useState<AccountType>('BANK_ACCOUNT');
  const [currency, setCurrency] = useState('PHP');
  const [balance, setBalance] = useState('0.00');
  const [institutionName, setInstitutionName] = useState('');
  const [submitting, setSubmitting] = useState(false);
  const [error, setError] = useState('');
  const [fieldError, setFieldError] = useState('');

  async function handleSubmit(event: FormEvent<HTMLFormElement>) {
    event.preventDefault();
    setError('');
    setFieldError('');

    const trimmedName = name.trim();
    const trimmedInstitution = institutionName.trim();
    const normalizedCurrency = currency.trim().toUpperCase();
    const numericBalance = Number(balance);

    if (!trimmedName || trimmedName.length > 100) {
      setFieldError('Enter an account name of 1–100 characters.');
      return;
    }
    if (!/^[A-Za-z]{3}$/.test(normalizedCurrency)) {
      setFieldError('Enter a valid three-letter currency code, such as PHP or USD.');
      return;
    }
    if (!Number.isFinite(numericBalance) || numericBalance < 0) {
      setFieldError('Enter a valid opening balance of zero or more.');
      return;
    }
    if (trimmedInstitution.length > 100) {
      setFieldError('Institution name must be 100 characters or fewer.');
      return;
    }

    const token = getAccessToken();
    if (!token) {
      setError('Your session has ended. Please sign in again.');
      return;
    }
    const customerId = getCustomerIdFromToken(token);
    if (!customerId) {
      setError('Your sign-in token does not include a customer identity. Please sign in again or contact support.');
      return;
    }

    setSubmitting(true);
    try {
      const response = await fetch('/api/accounts', {
        method: 'POST',
        headers: {
          Authorization: `Bearer ${token}`,
          'Content-Type': 'application/json',
          Accept: 'application/json',
        },
        body: JSON.stringify({
          customerId,
          name: trimmedName,
          type,
          currency: normalizedCurrency,
          balance: numericBalance,
          institutionName: trimmedInstitution || null,
        }),
      });

      if (!response.ok) {
        if (response.status === 401) throw new Error('Your session has expired. Please sign in again.');
        if (response.status === 403) throw new Error('You are not authorized to create an account for this customer.');
        if (response.status === 400) throw new Error('Please check the account details and try again.');
        throw new Error('We could not create your financial account. Please try again.');
      }

      // Only navigate after the server confirms the account was created.
      onBackToAccounts();
    } catch (cause) {
      setError(cause instanceof TypeError
        ? 'Unable to reach the Account Service. Check that it is running and try again.'
        : cause instanceof Error
          ? cause.message
          : 'We could not create your financial account. Please try again.');
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
          <button className="text-link" type="button" onClick={onBackToAccounts}>Accounts</button>
          <button className="text-link" type="button" onClick={onSignOut}>Sign out</button>
        </nav>
      </header>

      <section className="record-transaction-content" aria-labelledby="add-account-title">
        <div className="transactions-heading">
          <div>
            <span className="eyebrow"><span className="eyebrow-dot" /> YOUR MONEY</span>
            <h1 id="add-account-title">Add a financial account<span>.</span></h1>
            <p className="intro-copy">Keep your cash, bank accounts, cards, and investments organized in one place.</p>
          </div>
          <button className="secondary-button" type="button" onClick={onBackToAccounts}>Back to accounts</button>
        </div>

        <form className="form-card record-transaction-form" onSubmit={handleSubmit} noValidate>
          {error && <p className="record-feedback record-feedback-error" role="alert">{error}</p>}
          {fieldError && <p className="record-feedback record-feedback-error" role="alert">{fieldError}</p>}

          <div className="record-form-grid">
            <label className="record-field record-field-wide">
              <span>Account name <span aria-hidden="true">*</span></span>
              <input autoFocus required type="text" maxLength={100} value={name} onChange={event => setName(event.target.value)} placeholder="e.g. Everyday savings" disabled={submitting} />
              <small>Choose a name you will recognize.</small>
            </label>

            <label className="record-field">
              <span>Account type <span aria-hidden="true">*</span></span>
              <select required value={type} onChange={event => setType(event.target.value as AccountType)} disabled={submitting}>
                {accountTypes.map(item => <option key={item.value} value={item.value}>{item.label}</option>)}
              </select>
            </label>

            <label className="record-field">
              <span>Currency <span aria-hidden="true">*</span></span>
              <input required type="text" inputMode="text" autoCapitalize="characters" maxLength={3} pattern="[A-Za-z]{3}" value={currency} onChange={event => setCurrency(event.target.value.toUpperCase())} placeholder="PHP" disabled={submitting} />
              <small>Three-letter currency code, such as PHP, USD, or EUR.</small>
            </label>

            <label className="record-field">
              <span>Opening balance <span aria-hidden="true">*</span></span>
              <input required type="number" min="0" step="0.01" inputMode="decimal" value={balance} onChange={event => setBalance(event.target.value)} placeholder="0.00" disabled={submitting} />
              <small>Enter the balance you want to start tracking.</small>
            </label>

            <label className="record-field">
              <span>Financial institution <span className="record-optional">Optional</span></span>
              <input type="text" maxLength={100} value={institutionName} onChange={event => setInstitutionName(event.target.value)} placeholder="e.g. Your bank or provider" disabled={submitting} />
            </label>
          </div>

          <div className="record-form-actions">
            <button className="secondary-button" type="button" onClick={onBackToAccounts} disabled={submitting}>Cancel</button>
            <button className="submit-button" type="submit" disabled={submitting}>
              {submitting ? 'Creating account…' : 'Add account'}
            </button>
          </div>
          <p className="record-transaction-disclaimer">This adds a tracking account to Fintech Services. It does not open a real bank account, connect to an institution, or move money.</p>
        </form>
        <footer className="form-footer">© 2026 Fintech Services <span aria-hidden="true">·</span> Your financial life, in clearer view</footer>
      </section>
    </main>
  );
}
