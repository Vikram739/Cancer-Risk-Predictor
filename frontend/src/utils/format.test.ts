import { describe, expect, it } from 'vitest';
import { formatDate, formatMonth, formatNumber, formatPercent, modelDisplayName } from './format';

describe('formatPercent', () => {
  it('formats fractions as percentages', () => {
    expect(formatPercent(0.8731)).toBe('87.3%');
    expect(formatPercent(0.05, 0)).toBe('5%');
  });

  it('returns a dash for missing values', () => {
    expect(formatPercent(undefined)).toBe('-');
    expect(formatPercent(Number.NaN)).toBe('-');
  });
});

describe('date helpers', () => {
  it('formats ISO dates', () => {
    expect(formatDate('2023-03-14T10:22:00Z')).toBe('14 Mar 2023');
  });

  it('keeps date only values on the same calendar day in any timezone', () => {
    expect(formatDate('1974-05-14')).toBe('14 May 1974');
  });

  it('handles invalid input', () => {
    expect(formatDate('not a date')).toBe('-');
    expect(formatDate(null)).toBe('-');
  });

  it('formats month keys', () => {
    expect(formatMonth('2023-07')).toBe('Jul 2023');
    expect(formatMonth('garbage')).toBe('garbage');
  });
});

describe('misc', () => {
  it('limits significant digits', () => {
    expect(formatNumber(0.0123456)).toBe('0.01235');
    expect(formatNumber(1001.9)).toBe('1,002');
  });

  it('maps model ids to display names', () => {
    expect(modelDisplayName('xgboost')).toBe('XGBoost');
    expect(modelDisplayName('unknown_model')).toBe('unknown_model');
  });
});
