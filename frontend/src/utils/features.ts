/**
 * The 30 Wisconsin Diagnostic features, grouped the way the form shows them.
 * Ranges and medians come from the training data and are used for defaults and hints.
 */
export interface FeatureRange {
  min: number;
  max: number;
  median: number;
}

export const MEASUREMENTS = [
  'radius',
  'texture',
  'perimeter',
  'area',
  'smoothness',
  'compactness',
  'concavity',
  'concave_points',
  'symmetry',
  'fractal_dimension',
] as const;

export type FeatureGroupKey = 'mean' | 'error' | 'worst';

export const FEATURE_GROUPS: { key: FeatureGroupKey; title: string; description: string; features: string[] }[] = [
  {
    key: 'mean',
    title: 'Mean values',
    description: 'Average of each measurement across all cell nuclei in the sample',
    features: MEASUREMENTS.map((m) => `mean_${m}`),
  },
  {
    key: 'error',
    title: 'Standard error',
    description: 'Standard error of each measurement, a measure of variability',
    features: MEASUREMENTS.map((m) => `${m}_error`),
  },
  {
    key: 'worst',
    title: 'Worst values',
    description: 'Mean of the three largest values for each measurement',
    features: MEASUREMENTS.map((m) => `worst_${m}`),
  },
];

export const FEATURE_NAMES: string[] = FEATURE_GROUPS.flatMap((g) => g.features);

export const FEATURE_RANGES: Record<string, FeatureRange> = {
  mean_radius: { min: 6.981, max: 28.11, median: 13.37 },
  mean_texture: { min: 9.71, max: 39.28, median: 18.84 },
  mean_perimeter: { min: 43.79, max: 188.5, median: 86.24 },
  mean_area: { min: 143.5, max: 2501.0, median: 551.1 },
  mean_smoothness: { min: 0.05263, max: 0.1634, median: 0.09587 },
  mean_compactness: { min: 0.01938, max: 0.3454, median: 0.09263 },
  mean_concavity: { min: 0, max: 0.4268, median: 0.06154 },
  mean_concave_points: { min: 0, max: 0.2012, median: 0.0335 },
  mean_symmetry: { min: 0.106, max: 0.304, median: 0.1792 },
  mean_fractal_dimension: { min: 0.04996, max: 0.09744, median: 0.06154 },
  radius_error: { min: 0.1115, max: 2.873, median: 0.3242 },
  texture_error: { min: 0.3602, max: 4.885, median: 1.108 },
  perimeter_error: { min: 0.757, max: 21.98, median: 2.287 },
  area_error: { min: 6.802, max: 542.2, median: 24.53 },
  smoothness_error: { min: 0.001713, max: 0.03113, median: 0.00638 },
  compactness_error: { min: 0.002252, max: 0.1354, median: 0.02045 },
  concavity_error: { min: 0, max: 0.396, median: 0.02589 },
  concave_points_error: { min: 0, max: 0.05279, median: 0.01093 },
  symmetry_error: { min: 0.007882, max: 0.07895, median: 0.01873 },
  fractal_dimension_error: { min: 0.0008948, max: 0.02984, median: 0.003187 },
  worst_radius: { min: 7.93, max: 36.04, median: 14.97 },
  worst_texture: { min: 12.02, max: 49.54, median: 25.41 },
  worst_perimeter: { min: 50.41, max: 251.2, median: 97.66 },
  worst_area: { min: 185.2, max: 4254.0, median: 686.5 },
  worst_smoothness: { min: 0.07117, max: 0.2226, median: 0.1313 },
  worst_compactness: { min: 0.02729, max: 1.058, median: 0.2119 },
  worst_concavity: { min: 0, max: 1.252, median: 0.2267 },
  worst_concave_points: { min: 0, max: 0.291, median: 0.09993 },
  worst_symmetry: { min: 0.1565, max: 0.6638, median: 0.2822 },
  worst_fractal_dimension: { min: 0.05504, max: 0.2075, median: 0.08004 },
};

/** "worst_concave_points" becomes "Worst concave points". */
export function featureLabel(name: string): string {
  const text = name.replace(/_/g, ' ');
  return text.charAt(0).toUpperCase() + text.slice(1);
}

/** Label without the group prefix, used inside a group card. */
export function shortFeatureLabel(name: string): string {
  return featureLabel(name.replace(/^(mean|worst)_/, '').replace(/_error$/, ''));
}

export function defaultFeatureValues(): Record<string, number> {
  return Object.fromEntries(FEATURE_NAMES.map((f) => [f, FEATURE_RANGES[f].median]));
}

/** Input step sized to the magnitude of the median so spinners move sensibly. */
export function stepFor(name: string): number {
  const median = FEATURE_RANGES[name]?.median ?? 1;
  if (median >= 100) return 1;
  if (median >= 10) return 0.1;
  if (median >= 1) return 0.01;
  if (median >= 0.01) return 0.0001;
  return 0.00001;
}

/** Upper bound accepted by the form: generous headroom over the observed maximum. */
export function maxAllowed(name: string): number {
  return Number((FEATURE_RANGES[name].max * 3).toPrecision(4));
}
