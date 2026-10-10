import type { CashAccount, CashMovement, ForecastResponse, Scenario } from "./types";

const API = "/api";

async function request<T>(path: string, token: string, init: RequestInit = {}): Promise<T> {
  const response = await fetch(`${API}${path}`, {
    ...init,
    headers: {
      "Content-Type": "application/json",
      Authorization: `Basic ${token}`,
      ...(init.headers ?? {}),
    },
  });
  if (response.status === 401 || response.status === 403) {
    throw new Error("Your session is no longer authorized. Sign in again.");
  }
  if (!response.ok) {
    let message = `Request failed (${response.status})`;
    try {
      const body = await response.json() as { detail?: string; title?: string };
      message = body.detail || body.title || message;
    } catch { /* retain HTTP status message */ }
    throw new Error(message);
  }
  if (response.status === 204) return undefined as T;
  return response.json() as Promise<T>;
}

export const api = {
  checkCredentials: async (token: string) => request<CashAccount[]>("/accounts", token),
  accounts: (token: string) => request<CashAccount[]>("/accounts", token),
  createAccount: (token: string, body: {
    name: string; bankName: string; currency: string; openingBalance: string; overdraftLimit: string;
  }) => request<CashAccount>("/accounts", token, { method: "POST", body: JSON.stringify(body) }),
  recentMovements: (token: string) => request<CashMovement[]>("/movements/recent", token),
  postMovement: (token: string, accountId: string, key: string, body: {
    direction: "CREDIT" | "DEBIT"; amount: string; valueDate: string;
    description: string; externalReference?: string;
  }) => request<CashMovement>(`/accounts/${accountId}/movements`, token, {
    method: "POST", headers: { "Idempotency-Key": key }, body: JSON.stringify(body),
  }),
  forecast: (token: string, days: number, scenario: Scenario) =>
    request<ForecastResponse>(`/forecasts?days=${days}&scenario=${scenario}`, token),
};
