import { describe, expect, it } from 'vitest';
import {
  FEATURE_GROUPS,
  FEATURE_NAMES,
  FEATURE_RANGES,
  defaultFeatureValues,
  featureLabel,
  maxAllowed,
  shortFeatureLabel,
  stepFor,
} from './features';
import { assessmentSchema } from './assessmentSchema';

describe('feature catalogue', () => {
  it('has 30 unique features in three groups of ten', () => {
    expect(FEATURE_NAMES).toHaveLength(30);
    expect(new Set(FEATURE_NAMES).size).toBe(30);
    FEATURE_GROUPS.forEach((g) => expect(g.features).toHaveLength(10));
  });

  it('has a range for every feature with min <= median <= max', () => {
    FEATURE_NAMES.forEach((name) => {
      const r = FEATURE_RANGES[name];
      expect(r, name).toBeDefined();
      expect(r.min).toBeLessThanOrEqual(r.median);
      expect(r.median).toBeLessThanOrEqual(r.max);
    });
  });

  it('builds readable labels', () => {
    expect(featureLabel('worst_concave_points')).toBe('Worst concave points');
    expect(shortFeatureLabel('worst_concave_points')).toBe('Concave points');
    expect(shortFeatureLabel('fractal_dimension_error')).toBe('Fractal dimension');
  });

  it('picks steps that match the magnitude', () => {
    expect(stepFor('mean_area')).toBe(1);
    expect(stepFor('mean_radius')).toBe(0.1);
    expect(stepFor('mean_smoothness')).toBe(0.0001);
  });
});

describe('assessment schema', () => {
  it('accepts the default values', () => {
    const result = assessmentSchema.safeParse({ patientId: 4, features: defaultFeatureValues() });
    expect(result.success).toBe(true);
  });

  it('coerces numeric strings and rejects negatives and huge values', () => {
    const features: Record<string, unknown> = { ...defaultFeatureValues(), mean_radius: '14.2' };
    expect(assessmentSchema.safeParse({ patientId: 1, features }).success).toBe(true);

    features.mean_area = -5;
    features.worst_area = maxAllowed('worst_area') * 2;
    const result = assessmentSchema.safeParse({ patientId: 1, features });
    expect(result.success).toBe(false);
    if (!result.success) {
      const paths = result.error.issues.map((i) => i.path.join('.'));
      expect(paths).toEqual(expect.arrayContaining(['features.mean_area', 'features.worst_area']));
    }
  });

  it('requires a patient', () => {
    expect(assessmentSchema.safeParse({ features: defaultFeatureValues() }).success).toBe(false);
  });
});
