import { useCallback, useEffect, useState } from 'react';
import { getAccessToken } from './authSession';

type Props = { paymentId: string; onDone: () => void };
type Payment = { status: string; provider: string; amount: number | string; currency: string; description: string };

export default function PaymentStatus({ paymentId, onDone }: Props) {
  const [payment, setPayment] = useState<Payment | null>(null);
  const [error, setError] = useState('');
  const [loading, setLoading] = useState(false);
  const refresh = useCallback(async () => {
    const token = getAccessToken();
    if (!token) { setError('Your session has expired. Sign in again.'); return; }
    setLoading(true); setError('');
    try {
      const response = await fetch(`/api/payments/${encodeURIComponent(paymentId)}`, {
        headers: { Authorization: `Bearer ${token}`, Accept: 'application/json' },
      });
      const payload: unknown = await response.json().catch(() => null);
      if (!response.ok) throw new Error('Unable to retrieve your payment status.');
      const result = payload as Payment;
      if (result.provider !== 'STRIPE') throw new Error('Payment provider does not match.');
      setPayment(result);
      if (result.status === 'SUCCEEDED') sessionStorage.removeItem('pendingStripePayment');
    } catch (cause) { setError(cause instanceof Error ? cause.message : 'Unable to retrieve payment status.'); }
    finally { setLoading(false); }
  }, [paymentId]);
  useEffect(() => { void refresh(); }, [refresh]);
  return (
    <main className="page-shell login-page"><section className="form-panel login-form-panel"><div className="form-card login-card">
      <div className="form-heading"><div className="form-step">SECURE PAYMENT</div>
        <h2>{payment?.status === 'SUCCEEDED' ? 'Payment complete' : payment?.status === 'FAILED' ? 'Payment failed' : 'Payment status'}</h2>
        <p role="status" aria-live="polite">{error || (payment ? `${payment.amount} ${payment.currency} · ${payment.status}` : 'Checking your payment status…')}</p>
        {payment?.status === 'PENDING' && <p>Stripe has not yet confirmed payment. Status is updated only after a verified provider webhook.</p>}
      </div>
      {payment?.status === 'PENDING' && <button className="secondary-button" type="button" onClick={() => void refresh()} disabled={loading}>Refresh status</button>}
      <button className="submit-button" type="button" onClick={onDone}>Return to transactions <span aria-hidden="true">→</span></button>
    </div></section></main>
  );
}
