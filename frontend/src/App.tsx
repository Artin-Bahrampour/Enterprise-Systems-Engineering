import { useCallback, useEffect, useMemo, useState } from "react";
import type { FormEvent } from "react";
import { api } from "./api";
import type { CashAccount, CashMovement, ForecastResponse, Scenario } from "./types";

function money(value: string | number, currency: string) {
  return new Intl.NumberFormat("en-GB", { style: "currency", currency, maximumFractionDigits: 2 }).format(Number(value));
}
function localToday() {
  const now = new Date();
  return new Date(now.getTime() - now.getTimezoneOffset() * 60_000).toISOString().slice(0, 10);
}
function dateLabel(value: string) {
  return new Intl.DateTimeFormat("en-GB", { day: "2-digit", month: "short", year: "numeric" }).format(new Date(`${value}T12:00:00`));
}

function Login({ onLogin }: { onLogin: (token: string) => void }) {
  const [username, setUsername] = useState("treasury-admin");
  const [password, setPassword] = useState("");
  const [error, setError] = useState("");
  const [busy, setBusy] = useState(false);
  async function submit(event: FormEvent) {
    event.preventDefault(); setBusy(true); setError("");
    const token = btoa(`${username}:${password}`);
    try { await api.checkCredentials(token); onLogin(token); }
    catch (e) { setError(e instanceof Error ? e.message : "Sign-in failed."); }
    finally { setBusy(false); }
  }
  return <main className="login-shell">
    <section className="login-card">
      <div className="brand-mark">TC</div>
      <p className="eyebrow">TREASURY OPERATIONS</p>
      <h1>Liquidity Control Center</h1>
      <p className="muted">Sign in to review cash positions, post movements, and run liquidity scenarios.</p>
      <form onSubmit={submit} className="form-stack">
        <label>Username<input autoComplete="username" value={username} onChange={e => setUsername(e.target.value)} required /></label>
        <label>Password<input autoComplete="current-password" type="password" value={password} onChange={e => setPassword(e.target.value)} required /></label>
        {error && <div className="alert error">{error}</div>}
        <button className="primary" disabled={busy}>{busy ? "Checking…" : "Sign in securely"}</button>
      </form>
      <p className="tiny">Demo credentials are stored in your local <code>.env</code> file.</p>
    </section>
  </main>;
}

export default function App() {
  const [token, setToken] = useState(() => sessionStorage.getItem("treasury.auth") ?? "");
  const [accounts, setAccounts] = useState<CashAccount[]>([]);
  const [movements, setMovements] = useState<CashMovement[]>([]);
  const [forecast, setForecast] = useState<ForecastResponse | null>(null);
  const [scenario, setScenario] = useState<Scenario>("BASE");
  const [days, setDays] = useState(30);
  const [busy, setBusy] = useState(false);
  const [error, setError] = useState("");
  const [notice, setNotice] = useState("");

  const refresh = useCallback(async () => {
    if (!token) return;
    setBusy(true); setError("");
    try {
      const [a, m, f] = await Promise.all([
        api.accounts(token), api.recentMovements(token), api.forecast(token, days, scenario),
      ]);
      setAccounts(a); setMovements(m); setForecast(f);
    } catch (e) {
      const message = e instanceof Error ? e.message : "Could not load treasury data.";
      if (message.includes("authorized")) { sessionStorage.removeItem("treasury.auth"); setToken(""); }
      setError(message);
    } finally { setBusy(false); }
  }, [token, days, scenario]);

  useEffect(() => { void refresh(); }, [refresh]);

  const currencyTotals = useMemo(() => {
    const totals = new Map<string, number>();
    accounts.forEach(a => totals.set(a.currency, (totals.get(a.currency) ?? 0) + Number(a.currentBalance)));
    return [...totals.entries()].sort(([a], [b]) => a.localeCompare(b));
  }, [accounts]);

  function login(value: string) { sessionStorage.setItem("treasury.auth", value); setToken(value); }
  function logout() { sessionStorage.removeItem("treasury.auth"); setToken(""); setAccounts([]); setMovements([]); setForecast(null); }
  function showNotice(message: string) { setNotice(message); window.setTimeout(() => setNotice(""), 4500); }

  if (!token) return <Login onLogin={login} />;

  return <div className="app-shell">
    <aside className="sidebar">
      <div className="brand"><span className="brand-mark small">TC</span><div><strong>Treasury</strong><small>CONTROL CENTER</small></div></div>
      <nav><a className="nav-active" href="#overview">◫ <span>Overview</span></a><a href="#accounts">▤ <span>Cash accounts</span></a><a href="#activity">↕ <span>Movements</span></a><a href="#forecast">⌁ <span>Liquidity forecast</span></a></nav>
      <div className="sidebar-bottom"><div className="status-dot" /> <span>Connected to treasury API</span><button className="link-button" onClick={logout}>Sign out</button></div>
    </aside>
    <main className="main-content">
      <header className="topbar"><div><p className="eyebrow">TREASURY / DAILY POSITION</p><h1>Liquidity overview</h1></div><div className="topbar-right"><span className="today">{new Intl.DateTimeFormat("en-GB", { dateStyle: "full" }).format(new Date())}</span><button className="secondary" onClick={() => void refresh()} disabled={busy}>{busy ? "Refreshing…" : "↻ Refresh"}</button></div></header>
      {error && <div className="alert error">{error}</div>}
      {notice && <div className="alert success">{notice}</div>}
      <section id="overview" className="metric-grid">
        <article className="metric-card"><div className="metric-label">Active cash accounts</div><div className="metric-value">{accounts.length}</div><div className="metric-foot">Across bank relationships</div></article>
        {currencyTotals.slice(0, 3).map(([currency, total]) => <article className="metric-card" key={currency}><div className="metric-label">Available balance · {currency}</div><div className="metric-value">{money(total, currency)}</div><div className="metric-foot">Current posted cash position</div></article>)}
      </section>
      <section id="accounts" className="content-grid">
        <div className="panel">
          <div className="panel-heading"><div><p className="eyebrow">BANKING RELATIONSHIPS</p><h2>Cash accounts</h2></div><span className="pill">{accounts.length} accounts</span></div>
          <div className="table-wrap"><table><thead><tr><th>Account</th><th>Bank</th><th>Currency</th><th>Current balance</th><th>Overdraft limit</th></tr></thead>
            <tbody>{accounts.map(a => <tr key={a.id}><td><strong>{a.name}</strong><small className="cell-sub">{a.id.slice(0, 8)}…</small></td><td>{a.bankName}</td><td><span className="currency-tag">{a.currency}</span></td><td className={Number(a.currentBalance) < 0 ? "negative" : ""}>{money(a.currentBalance, a.currency)}</td><td>{money(a.overdraftLimit, a.currency)}</td></tr>)}
            {accounts.length === 0 && <tr><td colSpan={5} className="empty">No accounts yet. Create one below to start tracking liquidity.</td></tr>}</tbody></table></div>
          <AccountForm token={token} onCreated={async () => { showNotice("Cash account created and audit event recorded."); await refresh(); }} onError={setError} />
        </div>
        <div className="panel">
          <div className="panel-heading"><div><p className="eyebrow">CONTROLLED POSTING</p><h2>Record movement</h2></div><span className="pill neutral">Idempotent</span></div>
          <MovementForm accounts={accounts} token={token} onPosted={async replay => { showNotice(replay ? "Duplicate request recognized; no second posting occurred." : "Movement posted successfully."); await refresh(); }} onError={setError} />
          <div className="rule-note"><strong>Posting controls</strong><p>Amounts use decimal arithmetic. Debits cannot exceed the configured overdraft limit. Each submission requires an idempotency key.</p></div>
        </div>
      </section>
      <section id="forecast" className="panel forecast-panel">
        <div className="panel-heading forecast-heading"><div><p className="eyebrow">FORWARD LIQUIDITY</p><h2>Cash forecast</h2><p className="muted">Weekday-matched historical flows · no cross-currency aggregation</p></div>
          <div className="forecast-controls"><label>Scenario<select value={scenario} onChange={e => setScenario(e.target.value as Scenario)}><option value="BASE">Base case</option><option value="STRESS">Stress case</option><option value="UPSIDE">Upside case</option></select></label><label>Horizon<select value={days} onChange={e => setDays(Number(e.target.value))}><option value={7}>7 days</option><option value={14}>14 days</option><option value={30}>30 days</option><option value={60}>60 days</option><option value={90}>90 days</option></select></label></div>
        </div>
        {forecast && <ForecastView forecast={forecast} />}
      </section>
      <section id="activity" className="panel">
        <div className="panel-heading"><div><p className="eyebrow">TRACEABLE ACTIVITY</p><h2>Recent movements</h2></div><span className="pill">{movements.length} latest</span></div>
        <div className="table-wrap"><table><thead><tr><th>Value date</th><th>Account</th><th>Description</th><th>Reference</th><th>Direction</th><th>Amount</th><th>Balance after</th></tr></thead>
          <tbody>{movements.slice(0, 15).map(m => <tr key={m.id}><td>{dateLabel(m.valueDate)}</td><td>{m.accountName}<small className="cell-sub">{m.currency}</small></td><td>{m.description}</td><td>{m.externalReference || "—"}</td><td><span className={`direction ${m.direction.toLowerCase()}`}>{m.direction}</span></td><td className={m.direction === "DEBIT" ? "negative" : "positive"}>{m.direction === "DEBIT" ? "−" : "+"}{money(m.amount, m.currency)}</td><td>{money(m.balanceAfter, m.currency)}</td></tr>)}
          {movements.length === 0 && <tr><td colSpan={7} className="empty">No movements recorded.</td></tr>}</tbody></table></div>
      </section>
      <footer className="footer"><span>Internal treasury workspace · Portfolio reference implementation</span><span>Balances and forecasts are account-level and currency-specific.</span></footer>
    </main>
  </div>;
}

function AccountForm({ token, onCreated, onError }: { token: string; onCreated: () => Promise<void>; onError: (s: string) => void }) {
  const [open, setOpen] = useState(false);
  const [name, setName] = useState(""); const [bankName, setBankName] = useState("");
  const [currency, setCurrency] = useState("NOK"); const [openingBalance, setOpeningBalance] = useState("0.0000");
  const [overdraftLimit, setOverdraftLimit] = useState("0.0000"); const [saving, setSaving] = useState(false);
  async function submit(e: FormEvent) {
    e.preventDefault(); setSaving(true); onError("");
    try {
      await api.createAccount(token, { name, bankName, currency, openingBalance, overdraftLimit });
      setName(""); setBankName(""); setOpeningBalance("0.0000"); setOverdraftLimit("0.0000"); setOpen(false);
      await onCreated();
    } catch (error) { onError(error instanceof Error ? error.message : "Account could not be created."); }
    finally { setSaving(false); }
  }
  return <div className="inline-form">
    <button className="text-action" onClick={() => setOpen(!open)}>{open ? "− Cancel" : "+ Add cash account"}</button>
    {open && <form className="form-grid" onSubmit={submit}>
      <label>Account name<input value={name} onChange={e => setName(e.target.value)} maxLength={120} required /></label>
      <label>Bank name<input value={bankName} onChange={e => setBankName(e.target.value)} maxLength={120} required /></label>
      <label>Currency<select value={currency} onChange={e => setCurrency(e.target.value)}><option>NOK</option><option>EUR</option><option>USD</option><option>GBP</option><option>SEK</option><option>DKK</option></select></label>
      <label>Opening balance<input type="number" step="0.0001" value={openingBalance} onChange={e => setOpeningBalance(e.target.value)} required /></label>
      <label>Overdraft limit<input type="number" min="0" step="0.0001" value={overdraftLimit} onChange={e => setOverdraftLimit(e.target.value)} required /></label>
      <button className="primary form-submit" disabled={saving}>{saving ? "Saving…" : "Create account"}</button>
    </form>}
  </div>;
}

function MovementForm({ accounts, token, onPosted, onError }: { accounts: CashAccount[]; token: string; onPosted: (replay: boolean) => Promise<void>; onError: (s: string) => void }) {
  const [accountId, setAccountId] = useState(""); const [direction, setDirection] = useState<"CREDIT" | "DEBIT">("DEBIT");
  const [amount, setAmount] = useState(""); const [valueDate, setValueDate] = useState(localToday());
  const [description, setDescription] = useState(""); const [reference, setReference] = useState(""); const [saving, setSaving] = useState(false);
  useEffect(() => { if (!accounts.some(a => a.id === accountId) && accounts[0]) setAccountId(accounts[0].id); }, [accounts, accountId]);
  async function submit(e: FormEvent) {
    e.preventDefault(); if (!accountId) { onError("Create or select a cash account first."); return; }
    setSaving(true); onError("");
    try {
      const result = await api.postMovement(token, accountId, crypto.randomUUID(), {
        direction, amount, valueDate, description, externalReference: reference || undefined,
      });
      setAmount(""); setDescription(""); setReference(""); await onPosted(result.idempotentReplay);
    } catch (error) { onError(error instanceof Error ? error.message : "Movement could not be posted."); }
    finally { setSaving(false); }
  }
  return <form className="form-stack movement-form" onSubmit={submit}>
    <label>Cash account<select value={accountId} onChange={e => setAccountId(e.target.value)} required><option value="">Select account</option>{accounts.map(a => <option key={a.id} value={a.id}>{a.name} · {a.currency}</option>)}</select></label>
    <div className="form-grid two"><label>Direction<select value={direction} onChange={e => setDirection(e.target.value as "CREDIT" | "DEBIT")}><option value="DEBIT">Debit / outflow</option><option value="CREDIT">Credit / inflow</option></select></label><label>Amount<input type="number" min="0.0001" step="0.0001" value={amount} onChange={e => setAmount(e.target.value)} required /></label></div>
    <label>Value date<input type="date" max={localToday()} value={valueDate} onChange={e => setValueDate(e.target.value)} required /></label>
    <label>Description<input value={description} onChange={e => setDescription(e.target.value)} maxLength={240} placeholder="e.g. Supplier invoice 48192" required /></label>
    <label>External reference <span className="muted">(optional)</span><input value={reference} onChange={e => setReference(e.target.value)} maxLength={120} placeholder="Bank statement / ERP reference" /></label>
    <button className="primary" disabled={saving || accounts.length === 0}>{saving ? "Posting…" : "Post cash movement"}</button>
  </form>;
}

function ForecastView({ forecast }: { forecast: ForecastResponse }) {
  const [accountFilter, setAccountFilter] = useState("ALL");
  const accountOptions = [...new Map(forecast.points.map(p => [p.accountId, p.accountName] as const)).entries()];
  const rows = forecast.points.filter(p => accountFilter === "ALL" || p.accountId === accountFilter);
  const lowBalance = rows.some(p => Number(p.closingBalance) < 0);
  return <div>
    <div className="forecast-summary"><div><span className="metric-label">Model</span><strong>{forecast.scenario} scenario</strong></div><div><span className="metric-label">Historical window</span><strong>{dateLabel(forecast.historyFrom)} – {dateLabel(forecast.historyTo)}</strong></div><div><span className="metric-label">Forecast horizon</span><strong>{forecast.days} days</strong></div><label>Account<select value={accountFilter} onChange={e => setAccountFilter(e.target.value)}><option value="ALL">All accounts</option>{accountOptions.map(([id, name]) => <option value={id} key={id}>{name}</option>)}</select></label></div>
    {lowBalance && <div className="alert warning">At least one projected account balance falls below zero. Review the account-level path and overdraft policy.</div>}
    <div className="table-wrap forecast-table"><table><thead><tr><th>Forecast date</th><th>Account</th><th>Opening</th><th>Expected inflows</th><th>Expected outflows</th><th>Projected close</th></tr></thead><tbody>
      {rows.map(p => <tr key={`${p.accountId}-${p.date}`}><td>{dateLabel(p.date)}</td><td>{p.accountName}<small className="cell-sub">{p.currency}</small></td><td>{money(p.openingBalance, p.currency)}</td><td className="positive">+{money(p.expectedInflows, p.currency)}</td><td className="negative">−{money(p.expectedOutflows, p.currency)}</td><td className={Number(p.closingBalance) < 0 ? "negative strong" : "strong"}>{money(p.closingBalance, p.currency)}</td></tr>)}
      {rows.length === 0 && <tr><td colSpan={6} className="empty">No forecast data available.</td></tr>}
    </tbody></table></div>
    <p className="tiny">Forecast is a planning estimate based on up to eight prior observations for each weekday. It is not a payment commitment or a guarantee of future cash flows.</p>
  </div>;
}
