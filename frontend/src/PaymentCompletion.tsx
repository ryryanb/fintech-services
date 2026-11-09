import { useEffect, useState } from 'react';
import { getAccessToken } from './authSession';

type Props = { paymentId: string; orderId: string; onDone: () => void };
type Result = { status?: string; amount?: number | string; currency?: string; provider?: string };

export default function PaymentCompletion({ paymentId, orderId, onDone }: Props) {
  const [message, setMessage] = useState('Confirming your PayPal payment securely…');
  const [status, setStatus] = useState('PROCESSING');

  useEffect(() => {
    let active = true;
    async function complete() {
      const token = getAccessToken();
      if (!token) { setMessage('Please sign in again to complete this payment.'); return; }
      try {
        const response = await fetch(`/api/payments/${encodeURIComponent(paymentId)}/complete/paypal?token=${encodeURIComponent(orderId)}`, {
          method: 'POST',
          headers: { Authorization: `Bearer ${token}`, Accept: 'application/json' },
        });
        const payload: unknown = await response.json().catch(() => null);
        if (!response.ok) {
          const detail = typeof payload === 'object' && payload !== null && 'detail' in payload && typeof payload.detail === 'string'
            ? payload.detail : 'We could not confirm the PayPal payment.';
          throw new Error(detail);
        }
        const result = payload as Result;
        if (result.provider !== 'PAYPAL' || result.status !== 'SUCCEEDED') {
          throw new Error('PayPal has not confirmed a completed payment.');
        }
        sessionStorage.removeItem('pendingPaypalPayment');
        if (active) { setStatus('SUCCEEDED'); setMessage('Payment completed successfully. Your payment has been captured and verified.'); }
      } catch (error) {
        if (active) { setStatus('ERROR'); setMessage(error instanceof Error ? error.message : 'Unable to complete payment.'); }
      }
    }
    void complete();
    return () => { active = false; };
  }, [paymentId, orderId]);

  return (
    <main className="page-shell login-page">
      <section className="form-panel login-form-panel">
        <div className="form-card login-card">
          <div className="form-heading">
            <div className="form-step">SECURE PAYMENT</div>
            <h2>{status === 'SUCCEEDED' ? 'Payment complete' : status === 'ERROR' ? 'Payment needs attention' : 'Finalizing payment'}</h2>
            <p role="status" aria-live="polite">{message}</p>
          </div>
          <button className="submit-button" type="button" onClick={onDone}>Return to transactions <span aria-hidden="true">→</span></button>
        </div>
      </section>
    </main>
  );
}
