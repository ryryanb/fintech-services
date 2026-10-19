import { useEffect, useState, type FormEvent } from 'react';
import { getAccessToken } from './authSession';

type Category = { id: string; name: string };
type SpendingCategoriesProps = { onBack: () => void; onSignOut: () => void };

function isCategory(value: unknown): value is Category {
  if (typeof value !== 'object' || value === null) return false;
  const item = value as Record<string, unknown>;
  return typeof item.id === 'string' && typeof item.name === 'string';
}

export default function SpendingCategories({ onBack, onSignOut }: SpendingCategoriesProps) {
  const [categories, setCategories] = useState<Category[]>([]);
  const [name, setName] = useState('');
  const [loading, setLoading] = useState(true);
  const [saving, setSaving] = useState(false);
  const [error, setError] = useState('');
  const [notice, setNotice] = useState('');
  const [retry, setRetry] = useState(0);

  useEffect(() => {
    const controller = new AbortController();
    async function load() {
      setLoading(true);
      setError('');
      const token = getAccessToken();
      if (!token) { setError('Your session has ended. Please sign in again.'); setLoading(false); return; }
      try {
        const response = await fetch('/api/categories', {
          headers: { Authorization: \`Bearer \${token}\`, Accept: 'application/json' }, signal: controller.signal,
        });
        if (!response.ok) throw new Error(response.status === 401 || response.status === 403 ? 'Your session is no longer valid. Please sign in again.' : 'We could not load your custom categories. Please try again.');
        const payload: unknown = await response.json();
        if (!Array.isArray(payload) || !payload.every(isCategory)) throw new Error('The service returned an unexpected category list.');
        setCategories(payload);
      } catch (cause) {
        if (cause instanceof DOMException && cause.name === 'AbortError') return;
        setError(cause instanceof TypeError ? 'Unable to reach the Transaction Service.' : cause instanceof Error ? cause.message : 'Unable to load categories.');
      } finally { if (!controller.signal.aborted) setLoading(false); }
    }
    void load();
    return () => controller.abort();
  }, [retry]);

  async function createCategory(event: FormEvent<HTMLFormElement>) {
    event.preventDefault(); setError(''); setNotice('');
    const normalized = name.trim().replace(/\s+/g, ' ');
    if (!normalized) { setError('Enter a category name.'); return; }
    if (normalized.length > 60) { setError('Category names must be 60 characters or fewer.'); return; }
    const token = getAccessToken();
    if (!token) { setError('Your session has ended. Please sign in again.'); return; }
    setSaving(true);
    try {
      const response = await fetch('/api/categories', {
        method: 'POST',
        headers: { Authorization: \`Bearer \${token}\`, 'Content-Type': 'application/json', Accept: 'application/json' },
        body: JSON.stringify({ name: normalized }),
      });
      const payload: unknown = await response.json().catch(() => null);
      if (!response.ok) {
        const message = typeof payload === 'object' && payload !== null && 'message' in payload && typeof payload.message === 'string'
          ? payload.message : response.status === 409 ? 'A category with that name already exists.' : 'We could not create this category.';
        throw new Error(message);
      }
      if (!isCategory(payload)) throw new Error('The service returned an unexpected category response.');
      setCategories(current => [...current, payload].sort((a, b) => a.name.localeCompare(b.name)));
      setName(''); setNotice(\`Created “\${payload.name}”. It is now available when recording a transaction.\`);
    } catch (cause) {
      setError(cause instanceof TypeError ? 'Unable to reach the Transaction Service.' : cause instanceof Error ? cause.message : 'Unable to create category.');
    } finally { setSaving(false); }
  }

  async function deleteCategory(category: Category) {
    if (!window.confirm(\`Delete the custom category “\${category.name}”? Existing transactions will keep their saved category label.\`)) return;
    const token = getAccessToken();
    if (!token) { setError('Your session has ended. Please sign in again.'); return; }
    setError(''); setNotice('');
    try {
      const response = await fetch(\`/api/categories/\${encodeURIComponent(category.id)}\`, {
        method: 'DELETE', headers: { Authorization: \`Bearer \${token}\`, Accept: 'application/json' },
      });
      if (!response.ok) throw new Error(response.status === 404 ? 'That category was not found.' : 'We could not delete this category.');
      setCategories(current => current.filter(item => item.id !== category.id));
      setNotice(\`Deleted “\${category.name}”. Existing transaction labels are unchanged.\`);
    } catch (cause) {
      setError(cause instanceof TypeError ? 'Unable to reach the Transaction Service.' : cause instanceof Error ? cause.message : 'Unable to delete category.');
    }
  }

  return (
    <main className="page-shell transactions-page">
      <header className="topbar">
        <a className="brand" href="/" aria-label="Fintech home" onClick={event => event.preventDefault()}>
          <span className="brand-mark" aria-hidden="true"><svg viewBox="0 0 32 32" fill="none"><path d="M7 23.5V8.5h4.2l4.8 7.1 4.8-7.1H25v15h-4.4v-8.1L16 22l-4.6-6.6v8.1H7Z" fill="currentColor" /></svg></span>
          <span>fintech<span className="brand-period">.</span></span>
        </a>
        <nav className="topbar-prompt profile-nav-actions" aria-label="Main navigation">
          <button className="text-link" type="button" onClick={onBack}>Transactions</button>
          <button className="text-link" type="button" onClick={onSignOut}>Sign out</button>
        </nav>
      </header>
      <section className="transactions-content spending-categories-content" aria-labelledby="spending-categories-title">
        <div className="transactions-heading">
          <div><span className="eyebrow"><span className="eyebrow-dot" /> PERSONAL FINANCE</span><h1 id="spending-categories-title">Spending categories<span>.</span></h1><p className="intro-copy">Create categories that match the way you organize your finances.</p></div>
          <button className="secondary-button" type="button" onClick={onBack}>Back to transactions</button>
        </div>
        <form className="form-card spending-category-create" onSubmit={createCategory}>
          <h2>Create a custom category</h2>
          <p>Use a clear name such as Pets, Gifts, Education, or Family support. Names are private to your account.</p>
          {error && <p className="record-feedback record-feedback-error" role="alert">{error}</p>}
          {notice && <p className="record-feedback record-feedback-success" role="status">{notice}</p>}
          <label className="record-field"><span>Category name</span><input value={name} onChange={event => setName(event.target.value)} maxLength={60} placeholder="e.g. Pets" required disabled={saving} /><small>{name.trim().length}/60 characters. Names matching built-in categories are reserved.</small></label>
          <div className="record-form-actions"><button className="submit-button" type="submit" disabled={saving}>{saving ? 'Creating…' : 'Create category'}</button></div>
        </form>
        <section className="form-card spending-category-list-card" aria-labelledby="custom-category-list-title">
          <div className="spending-category-list-heading"><div><h2 id="custom-category-list-title">Your custom categories</h2><p>These categories are available when you record a transaction.</p></div><span className="spending-category-count">{categories.length}</span></div>
          {loading ? <p role="status">Loading your categories…</p> : error && categories.length === 0 ? <div className="transactions-state"><p>Categories could not be loaded.</p><button className="secondary-button" type="button" onClick={() => setRetry(value => value + 1)}>Try again</button></div> : categories.length === 0 ? <div className="spending-category-empty"><h3>No custom categories yet</h3><p>Create your first category above. Built-in categories remain available too.</p></div> : <ul className="spending-category-manage-list">{categories.map(category => <li key={category.id}><span><strong>{category.name}</strong><small>Custom category</small></span><button className="secondary-button spending-category-delete" type="button" onClick={() => void deleteCategory(category)}>Delete</button></li>)}</ul>}
        </section>
        <footer className="form-footer">© 2026 Fintech Services <span aria-hidden="true">·</span> Your financial life, in clearer view</footer>
      </section>
    </main>
  );
}
