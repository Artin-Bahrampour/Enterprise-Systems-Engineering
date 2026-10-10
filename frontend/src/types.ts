export type Direction = "CREDIT" | "DEBIT";
export type Scenario = "BASE" | "STRESS" | "UPSIDE";

export interface CashAccount {
  id: string;
  name: string;
  bankName: string;
  currency: string;
  openingBalance: string;
  currentBalance: string;
  overdraftLimit: string;
  createdAt: string;
  version: number;
}

export interface CashMovement {
  id: string;
  accountId: string;
  accountName: string;
  currency: string;
  idempotencyKey: string;
  externalReference?: string | null;
  direction: Direction;
  amount: string;
  valueDate: string;
  description: string;
  balanceAfter: string;
  createdAt: string;
  idempotentReplay: boolean;
}

export interface ForecastPoint {
  date: string;
  accountId: string;
  accountName: string;
  currency: string;
  openingBalance: string;
  expectedInflows: string;
  expectedOutflows: string;
  closingBalance: string;
}

export interface ForecastResponse {
  scenario: Scenario;
  days: number;
  generatedAt: string;
  historyFrom: string;
  historyTo: string;
  points: ForecastPoint[];
}
