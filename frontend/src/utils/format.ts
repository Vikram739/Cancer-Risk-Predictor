import type { RiskBand } from '../api/types';

export function formatPercent(value: number | null | undefined, digits = 1): string {
  if (value === null || value === undefined || Number.isNaN(value)) return '-';
  return `${(value * 100).toFixed(digits)}%`;
}

const dateFormatter = new Intl.DateTimeFormat('en-GB', { day: '2-digit', month: 'short', year: 'numeric' });
// date only values such as a date of birth are parsed as UTC midnight, format them in UTC
// so they do not shift to the previous day for users west of Greenwich
const dateOnlyFormatter = new Intl.DateTimeFormat('en-GB', {
  day: '2-digit',
  month: 'short',
  year: 'numeric',
  timeZone: 'UTC',
});
const DATE_ONLY = /^\d{4}-\d{2}-\d{2}$/;
const dateTimeFormatter = new Intl.DateTimeFormat('en-GB', {
  day: '2-digit',
  month: 'short',
  year: 'numeric',
  hour: '2-digit',
  minute: '2-digit',
});

export function formatDate(iso?: string | null): string {
  if (!iso) return '-';
  const date = new Date(iso);
  if (Number.isNaN(date.getTime())) return '-';
  return (DATE_ONLY.test(iso) ? dateOnlyFormatter : dateFormatter).format(date);
}

export function formatDateTime(iso?: string | null): string {
  if (!iso) return '-';
  const date = new Date(iso);
  return Number.isNaN(date.getTime()) ? '-' : dateTimeFormatter.format(date);
}

/** "2023-03" becomes "Mar 2023". */
export function formatMonth(month: string): string {
  const [year, m] = month.split('-').map(Number);
  if (!year || !m) return month;
  return new Intl.DateTimeFormat('en-GB', { month: 'short', year: 'numeric', timeZone: 'UTC' }).format(
    new Date(Date.UTC(year, m - 1, 1)),
  );
}

export function formatNumber(value: number, maxDigits = 4): string {
  return new Intl.NumberFormat('en-US', { maximumSignificantDigits: maxDigits }).format(value);
}

export const RISK_LABELS: Record<RiskBand, string> = {
  LOW: 'Low risk',
  MODERATE: 'Moderate risk',
  HIGH: 'High risk',
};

export const RISK_COLORS: Record<RiskBand, string> = {
  LOW: '#2e9d6a',
  MODERATE: '#e0a020',
  HIGH: '#d64545',
};

export function modelDisplayName(name?: string): string {
  switch (name) {
    case 'logistic_regression':
      return 'Logistic Regression';
    case 'random_forest':
      return 'Random Forest';
    case 'svm':
      return 'Support Vector Machine';
    case 'xgboost':
      return 'XGBoost';
    default:
      return name ?? '-';
  }
}
