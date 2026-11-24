import { useEffect, useRef, useState, type FormEvent } from 'react';
import MarkdownMessage from './MarkdownMessage';
import { getAccessToken } from './authSession';

type Conversation = { id: string; title: string | null; };
type Message = { id: string; role: 'USER' | 'ASSISTANT'; content: string; createdAt: string };
const API = 'http://localhost:8089/api';

async function request<T>(path: string, options: RequestInit = {}): Promise<T> {
  const token = getAccessToken();
  if (!token) throw new Error('Your session has ended. Please sign in again.');
  const response = await fetch(`${API}${path}`, {
    ...options,
    headers: { Authorization: `Bearer ${token}`, ...(options.body ? { 'Content-Type': 'text/plain' } : {}), ...options.headers },
  });
  if (!response.ok) throw new Error(response.status === 401 ? 'Your session has expired. Please sign in again.' : 'The AI assistant is unavailable. Please try again.');
  return response.json() as Promise<T>;
}

export default function FinanceAssistant({ onBack, onSignOut }: { onBack: () => void; onSignOut: () => void }) {
  const [conversations, setConversations] = useState<Conversation[]>([]);
  const [selectedId, setSelectedId] = useState('');
  const [messages, setMessages] = useState<Message[]>([]);
  const [question, setQuestion] = useState('');
  const [loading, setLoading] = useState(false);
  const [error, setError] = useState('');
  const messagesEndRef = useRef<HTMLDivElement | null>(null);

  useEffect(() => {
    messagesEndRef.current?.scrollIntoView({ behavior: loading ? 'auto' : 'smooth', block: 'end' });
  }, [messages, loading]);

  async function loadMessages(id: string) {
    const data = await request<Message[]>(`/conversations/${id}/messages`);
    setMessages(data);
  }

  async function refreshConversations() {
    setConversations(await request<Conversation[]>('/conversations'));
  }

  useEffect(() => {
    let active = true;
    void (async () => {
      try {
        const data = await request<Conversation[]>('/conversations');
        if (!active) return;
        setConversations(data);
        if (data.length) {
          setSelectedId(data[0].id);
          const history = await request<Message[]>(`/conversations/${data[0].id}/messages`);
          if (active) setMessages(history);
        }
      } catch (cause) {
        if (active) setError(cause instanceof Error ? cause.message : 'Could not load conversations.');
      }
    })();
    return () => { active = false; };
  }, []);

  async function newConversation() {
    setError('');
    try {
      const conversation = await request<Conversation>('/conversations', { method: 'POST' });
      setConversations(current => [conversation, ...current]);
      setSelectedId(conversation.id);
      setMessages([]);
    } catch (cause) { setError(cause instanceof Error ? cause.message : 'Could not create a conversation.'); }
  }

  async function selectConversation(id: string) {
    setSelectedId(id);
    setError('');
    try { await loadMessages(id); }
    catch (cause) { setError(cause instanceof Error ? cause.message : 'Could not load messages.'); }
  }

  async function sendQuestion(event: FormEvent<HTMLFormElement>) {
    event.preventDefault();
    const content = question.trim();
    if (!content || loading) return;
    setLoading(true);
    setError('');
    try {
      let conversationId = selectedId;
      if (!conversationId) {
        const created = await request<Conversation>('/conversations', { method: 'POST' });
        conversationId = created.id;
        setSelectedId(conversationId);
        setConversations(current => [created, ...current]);
      }
      const now = new Date().toISOString();
      const assistantId = crypto.randomUUID();
      setMessages(current => [...current,
      { id: crypto.randomUUID(), role: 'USER', content, createdAt: now },
      { id: assistantId, role: 'ASSISTANT', content: '', createdAt: now },
      ]);
      setQuestion('');
      const token = getAccessToken();
      const response = await fetch(`${API}/conversations/${conversationId}/messages/stream`, {
        method: 'POST',
        headers: { Authorization: `Bearer ${token}`, 'Content-Type': 'text/plain' },
        body: content,
      });
      if (!response.ok || !response.body) throw new Error('The AI assistant could not answer. Please try again.');
      const reader = response.body.getReader();
      const decoder = new TextDecoder();
      while (true) {
        const { value, done } = await reader.read();
        if (done) break;
        const chunk = decoder.decode(value, { stream: true });
        if (chunk) setMessages(current => current.map(m => m.id === assistantId ? { ...m, content: m.content + chunk } : m));
      }
      const tail = decoder.decode();
      if (tail) setMessages(current => current.map(m => m.id === assistantId ? { ...m, content: m.content + tail } : m));
      await loadMessages(conversationId);
      await refreshConversations();
    } catch (cause) {
      setError(cause instanceof Error ? cause.message : 'Could not send your question.');
    } finally { setLoading(false); }
  }

  return <main className="page-shell">
    <header className="topbar">
      <a className="brand" href="/" onClick={event => event.preventDefault()}><span className="brand-mark" aria-hidden="true">M</span><span>fintech<span className="brand-period">.</span></span></a>
      <div className="topbar-prompt profile-nav-actions">
        <button className="text-link" type="button" onClick={onBack}>Profile</button>
        <button className="text-link" type="button" onClick={onSignOut}>Sign out</button>
      </div>
    </header>
    <section className="profile-content" aria-labelledby="assistant-title">
      <div className="profile-page-heading">
        <span className="eyebrow"><span className="eyebrow-dot" /> YOUR FINANCIAL ASSISTANT</span>
        <h1 id="assistant-title">Ask about your money<span>.</span></h1>
        <p className="intro-copy">Ask questions about your transactions, spending patterns, income, and categories.</p>
      </div>
      <div className="form-card assistant-chat-card">
        <aside aria-label="Conversation history">
          <button className="submit-button" type="button" onClick={() => void newConversation()}>+ New conversation</button>
          <div style={{ display: 'grid', gap: '.5rem', marginTop: '1rem' }}>
            {conversations.map(c => <button key={c.id} className="secondary-button" type="button" onClick={() => void selectConversation(c.id)} aria-current={selectedId === c.id ? 'true' : undefined}>{c.title || 'New conversation'}</button>)}
          </div>
        </aside>
        <div className="assistant-chat-main">
          <div className="assistant-messages" aria-live="polite" aria-relevant="additions text">
            {messages.length === 0 && <div className="assistant-empty-state"><h2>What would you like to understand?</h2><p className="profile-message">Try “How much did I spend on groceries in the last 30 days?” or “What were my biggest expenses this month?”</p></div>}
            {messages.map(m => <article key={m.id} className={'assistant-message assistant-message-' + m.role.toLowerCase()}>
              <div className="assistant-message-header">
                <span className="assistant-message-author">{m.role === 'USER' ? 'You' : 'Fintech AI'}</span>
                {m.createdAt && <time dateTime={m.createdAt}>{new Date(m.createdAt).toLocaleTimeString([], { hour: 'numeric', minute: '2-digit' })}</time>}
              </div>
              {m.content
                ? <MarkdownMessage content={m.content} />
                : loading && m.role === 'ASSISTANT' ? <span className="assistant-thinking"><i /><i /><i /><span className="sr-only">Thinking…</span></span> : null}
            </article>)}
            <div ref={messagesEndRef} aria-hidden="true" />
          </div>
          {error && <p className="record-feedback record-feedback-error" role="alert">{error}</p>}
          <form onSubmit={sendQuestion} style={{ display: 'grid', gap: '.75rem' }}>
            <label htmlFor="finance-question">Your question</label>
            <textarea id="finance-question" value={question} onChange={event => setQuestion(event.target.value)} rows={3} maxLength={2000} placeholder="Ask a question about your financial activity…" disabled={loading} required />
            <button className="submit-button" type="submit" disabled={loading || !question.trim()}>{loading ? 'Thinking…' : 'Ask question'} <span aria-hidden="true">→</span></button>
          </form>
          <p className="profile-privacy-note">Answers are based on your available transaction history. AI-generated answers can be inaccurate; verify important financial decisions.</p>
        </div>
      </div>
      <footer className="form-footer">© 2026 Fintech Services <span aria-hidden="true">·</span> Your financial life, in clearer view</footer>
    </section>
  </main>;
}
