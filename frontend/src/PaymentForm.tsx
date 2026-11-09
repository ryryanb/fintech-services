import { useState, type FormEvent } from 'react';
import { getAccessToken } from './authSession';

type PaymentFormProps = {
  onBackToTransactions: () => void;
  onViewAccounts: () => void;
  onSignOut: () => void;
};

type CheckoutResponse = {
  paymentId: string;
  amount: number | string;
  currency: string;
  description: string;
  provider: 'STRIPE' | 'PAYPAL';
  status: string;
  checkoutUrl: string;
};

const supportedCurrencies = ['PHP', 'USD', 'EUR', 'GBP', 'SGD', 'AUD', 'CAD'] as const;

function isCheckoutResponse(value: unknown): value is CheckoutResponse {
  if (typeof value !== 'object' || value === null) return false;
  const result = value as Record<string, unknown>;
  return typeof result.paymentId === 'string' &&
    (typeof result.amount === 'number' || typeof result.amount === 'string') &&
    typeof result.currency === 'string' &&
    typeof result.description === 'string' &&
    (result.provider === 'STRIPE' || result.provider === 'PAYPAL') &&
    typeof result.status === 'string' &&
    typeof result.checkoutUrl === 'string';
}

function errorMessage(payload: unknown, status: number): string {
  if (typeof payload === 'object' && payload !== null) {
    const data = payload as Record<string, unknown>;
    for (const key of ['detail', 'message', 'title']) {
      if (typeof data[key] === 'string' && data[key]) return data[key] as string;
    }
  }
  if (status === 400) return 'The payment details were not accepted. Check the amount and currency, then try again.';
  if (status === 401 || status === 403) return 'Your session has expired. Please sign in again.';
  return 'We could not start your payment. Please try again.';
}

export default function PaymentForm({ onBackToTransactions, onViewAccounts, onSignOut }: PaymentFormProps) {
  const [amount, setAmount] = useState('');
  const [currency, setCurrency] = useState<(typeof supportedCurrencies)[number]>('PHP');
  const [provider, setProvider] = useState<'STRIPE' | 'PAYPAL'>('STRIPE');
  const [description, setDescription] = useState('Demo purchase');
  const [submitting, setSubmitting] = useState(false);
  const [error, setError] = useState('');

  async function handleSubmit(event: FormEvent<HTMLFormElement>) {
    event.preventDefault();
    setError('');

    const numericAmount = Number(amount);
    if (!Number.isFinite(numericAmount) || numericAmount < 0.5) {
      setError('Enter an amount of at least 0.50.');
      return;
    }
    if (!/^\d+(\.\d{1,2})?$/.test(amount.trim())) {
      setError('Enter an amount with no more than two decimal places.');
      return;
    }
    const cleanDescription = description.trim();
    if (!cleanDescription || cleanDescription.length > 120) {
      setError('Enter a description of up to 120 characters.');
      return;
    }

    const token = getAccessToken();
    if (!token) {
      setError('Your session has expired. Please sign in again.');
      return;
    }

    setSubmitting(true);
    try {
      const response = await fetch('/api/payments/checkout', {
        method: 'POST',
        headers: {
          Authorization: `Bearer ${token}`,
          'Content-Type': 'application/json',
          Accept: 'application/json',
        },
        body: JSON.stringify({ amount: numericAmount, currency, description: cleanDescription, provider }),
      });
      const payload: unknown = await response.json().catch(() => null);
      if (!response.ok) throw new Error(errorMessage(payload, response.status));
      if (!isCheckoutResponse(payload)) {
        throw new Error('The Payment Service returned an unexpected response. Please try again.');
      }

      let checkoutUrl: URL;
      try {
        checkoutUrl = new URL(payload.checkoutUrl);
      } catch {
        throw new Error('The Payment Service returned an invalid Checkout URL.');
      }
      const trustedHosts = payload.provider === 'STRIPE'
        ? ['checkout.stripe.com']
        : ['www.sandbox.paypal.com', 'sandbox.paypal.com'];
      if (checkoutUrl.protocol !== 'https:' || !trustedHosts.includes(checkoutUrl.hostname)) {
        throw new Error('The Payment Service returned an untrusted Checkout URL.');
      }

      window.location.assign(checkoutUrl.toString());
    } catch (cause) {
      setError(cause instanceof TypeError
        ? 'Unable to reach the Payment Service. Check that it is running and try again.'
        : cause instanceof Error ? cause.message : 'We could not start your payment. Please try again.');
    } finally {
      setSubmitting(false);
    }
  }

  return (
    <main className="page-shell payment-page">
      <header className="topbar">
        <a className="brand" href="/" aria-label="Fintech home" onClick={event => event.preventDefault()}>
          <span className="brand-mark" aria-hidden="true"><svg viewBox="0 0 32 32" fill="none"><path d="M7 23.5V8.5h4.2l4.8 7.1 4.8-7.1H25v15h-4.4v-8.1L16 22l-4.6-6.6v8.1H7Z" fill="currentColor" /></svg></span>
          <span>fintech<span className="brand-period">.</span></span>
        </a>
        <nav className="topbar-prompt profile-nav-actions" aria-label="Main navigation">
          <button className="text-link" type="button" onClick={onBackToTransactions}>Transactions</button>
          <button className="text-link" type="button" onClick={onViewAccounts}>Accounts</button>
          <button className="text-link" type="button" onClick={onSignOut}>Sign out</button>
        </nav>
      </header>

      <section className="payment-content" aria-labelledby="payment-title">
        <div className="transactions-heading">
          <div>
            <span className="eyebrow"><span className="eyebrow-dot" /> SECURE PAYMENT</span>
            <h1 id="payment-title">Make a payment<span>.</span></h1>
            <p className="intro-copy">Enter the payment details and choose Stripe or PayPal for secure hosted checkout.</p>
          </div>
          <button className="secondary-button" type="button" onClick={onBackToTransactions} disabled={submitting}>Back to transactions</button>
        </div>

        <form className="form-card payment-form" onSubmit={handleSubmit} noValidate>
          {error && <p className="record-feedback record-feedback-error" role="alert">{error}</p>}

          <div className="payment-form-summary">
            <span className="payment-form-summary-icon" aria-hidden="true">↗</span>
            <div><strong>Choose how to pay</strong><p>Select an available payment provider. You will enter your payment details on its secure checkout page.</p></div>
          </div>

          <fieldset className="provider-selector" aria-describedby="provider-help">
            <legend>Payment provider <span aria-hidden="true">*</span></legend>
            <div className="provider-options">
              <label className={`provider-option ${provider === 'STRIPE' ? 'provider-option-selected' : ''}`}>
                <input
                  type="radio"
                  name="payment-provider"
                  value="STRIPE"
                  checked={provider === 'STRIPE'}
                  onChange={() => setProvider('STRIPE')}
                  disabled={submitting}
                />
                <span className="provider-option-mark" aria-hidden="true">S</span>
                <span className="provider-option-copy">
                  <strong>Stripe</strong>
                  <small>Secure hosted checkout · Available</small>
                </span>
                <span className="provider-option-check" aria-hidden="true">✓</span>
              </label>
              <label className={`provider-option ${provider === 'PAYPAL' ? 'provider-option-selected' : ''}`}>
                <input type="radio" name="payment-provider" value="PAYPAL" checked={provider === 'PAYPAL'} onChange={() => setProvider('PAYPAL')} disabled={submitting} />
                <span className="provider-option-mark provider-option-mark-paypal" aria-hidden="true">P</span>
                <span className="provider-option-copy">
                  <strong>PayPal</strong>
                  <small>PayPal sandbox checkout · Available</small>
                </span>
                {provider === 'PAYPAL' && <span className="provider-option-check" aria-hidden="true">✓</span>}
              </label>
            </div>
            <small id="provider-help" className="provider-help">PayPal uses sandbox credentials for testing. Select a provider to continue to its hosted approval or checkout page.</small>
          </fieldset>

          <div className="record-form-grid">
            <label className="record-field">
              <span>Amount <span aria-hidden="true">*</span></span>
              <input
                required
                type="number"
                min="0.50"
                max="9999999999.99"
                step="0.01"
                inputMode="decimal"
                value={amount}
                onChange={event => setAmount(event.target.value)}
                placeholder="0.00"
                disabled={submitting}
                autoComplete="off"
              />
              <small>Minimum 0.50. Your selected provider may enforce additional currency or account limits.</small>
            </label>

            <label className="record-field">
              <span>Currency <span aria-hidden="true">*</span></span>
              <select value={currency} onChange={event => setCurrency(event.target.value as (typeof supportedCurrencies)[number])} disabled={submitting}>
                {supportedCurrencies.map(code => <option key={code} value={code}>{code}</option>)}
              </select>
            </label>

            <label className="record-field record-field-wide">
              <span>Description <span aria-hidden="true">*</span></span>
              <input
                type="text"
                required
                maxLength={120}
                value={description}
                onChange={event => setDescription(event.target.value)}
                placeholder="What is this payment for?"
                disabled={submitting}
              />
              <small>{description.length}/120 characters</small>
            </label>
          </div>

          <div className="record-form-actions">
            <button className="secondary-button" type="button" onClick={onBackToTransactions} disabled={submitting}>Cancel</button>
            <button className="submit-button" type="submit" disabled={submitting}>
              {submitting ? 'Preparing secure checkout…' : 'Continue to secure checkout'} <span aria-hidden="true">→</span>
            </button>
          </div>
          <p className="payment-form-disclaimer">Payment details are entered on the provider's hosted page. PayPal approval creates an order but does not capture funds yet; capture and verified payment confirmation require a follow-up integration.</p>
        </form>
        <footer className="form-footer">© 2026 Fintech Services <span aria-hidden="true">·</span> Your financial life, in clearer view</footer>
      </section>
    </main>
  );
}
