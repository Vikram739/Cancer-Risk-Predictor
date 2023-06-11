export type RoleName = 'ADMIN' | 'DOCTOR' | 'ANALYST';
export type RiskBand = 'LOW' | 'MODERATE' | 'HIGH';
export type Sex = 'FEMALE' | 'MALE' | 'OTHER';

export interface User {
  id: number;
  fullName: string;
  email: string;
  roles: RoleName[];
}

export interface AuthResponse {
  accessToken: string;
  refreshToken: string;
  tokenType: string;
  expiresIn: number;
  user: User;
}

export interface Page<T> {
  content: T[];
  page: number;
  size: number;
  totalElements: number;
  totalPages: number;
  last: boolean;
}

export interface Patient {
  id: number;
  mrn: string;
  firstName: string;
  lastName: string;
  fullName: string;
  dateOfBirth: string;
  age: number;
  sex: Sex;
  phone?: string;
  email?: string;
  notes?: string;
  createdAt: string;
  updatedAt: string;
}

export interface PatientInput {
  mrn: string;
  firstName: string;
  lastName: string;
  dateOfBirth: string;
  sex: Sex;
  phone?: string;
  email?: string;
  notes?: string;
}

export interface FeatureContribution {
  feature: string;
  value: number;
  shapValue: number;
  direction: 'increases_risk' | 'decreases_risk';
}

export interface Assessment {
  id: number;
  patientId: number;
  patientName: string;
  patientMrn: string;
  probability: number;
  riskBand: RiskBand;
  prediction: 'malignant' | 'benign';
  modelName: string;
  modelVersion: string;
  topFeatures: FeatureContribution[];
  features: Record<string, number>;
  notes?: string;
  createdBy?: string;
  createdAt: string;
}

export interface AssessmentInput {
  patientId: number;
  features: Record<string, number>;
  notes?: string;
}

export interface Summary {
  totalPatients: number;
  totalAssessments: number;
  assessmentsByRiskBand: Record<RiskBand, number>;
  highRiskPatients: number;
  averageProbability: number;
  lastAssessmentAt?: string;
}

export interface MonthlyPoint {
  month: string;
  total: number;
  low: number;
  moderate: number;
  high: number;
  averageProbability: number;
}

export interface ModelScores {
  accuracy: number;
  precision: number;
  recall: number;
  f1: number;
  roc_auc: number;
  confusion_matrix?: { tn: number; fp: number; fn: number; tp: number };
}

export interface TrainedModel {
  display_name: string;
  best_params: Record<string, string | number | null>;
  cv: ModelScores;
  test: ModelScores;
}

export interface ModelInfo {
  model_name: string;
  model_version: string;
  trained_at?: string;
  features: string[];
  selected_features: string[];
  feature_ranges: Record<string, { min: number; max: number; median: number }>;
  risk_thresholds: { low_max: number; high_min: number };
  metrics?: {
    best_model: string;
    selection_metric: string;
    dataset: {
      name: string;
      samples: number;
      features: number;
      malignant: number;
      benign: number;
      train_size: number;
      test_size: number;
    };
    models: Record<string, TrainedModel>;
  };
}

export interface ModelPerformance {
  available: boolean;
  model: ModelInfo | Record<string, never>;
}

export interface ApiError {
  status: number;
  error: string;
  message: string;
  path?: string;
  details?: { field: string; message: string }[];
}
