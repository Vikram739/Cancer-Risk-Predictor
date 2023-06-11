import { api } from './client';
import type {
  Assessment,
  AssessmentInput,
  AuthResponse,
  ModelPerformance,
  MonthlyPoint,
  Page,
  Patient,
  PatientInput,
  RiskBand,
  RoleName,
  Summary,
  User,
} from './types';

export interface PageQuery {
  page?: number;
  size?: number;
  sort?: string;
}

export const authApi = {
  login: (email: string, password: string) =>
    api.post<AuthResponse>('/auth/login', { email, password }).then((r) => r.data),
  register: (body: { fullName: string; email: string; password: string; role: RoleName }) =>
    api.post<AuthResponse>('/auth/register', body).then((r) => r.data),
  me: () => api.get<User>('/auth/me').then((r) => r.data),
};

export const patientApi = {
  list: (query: PageQuery & { search?: string }) =>
    api.get<Page<Patient>>('/patients', { params: query }).then((r) => r.data),
  get: (id: number) => api.get<Patient>(`/patients/${id}`).then((r) => r.data),
  create: (body: PatientInput) => api.post<Patient>('/patients', body).then((r) => r.data),
  update: (id: number, body: PatientInput) => api.put<Patient>(`/patients/${id}`, body).then((r) => r.data),
  remove: (id: number) => api.delete(`/patients/${id}`),
  history: (id: number, query: PageQuery = { size: 100, sort: 'createdAt,desc' }) =>
    api.get<Page<Assessment>>(`/patients/${id}/assessments`, { params: query }).then((r) => r.data),
};

export const assessmentApi = {
  create: (body: AssessmentInput) => api.post<Assessment>('/assessments', body).then((r) => r.data),
  get: (id: number) => api.get<Assessment>(`/assessments/${id}`).then((r) => r.data),
  list: (query: PageQuery & { riskBand?: RiskBand }) =>
    api.get<Page<Assessment>>('/assessments', { params: query }).then((r) => r.data),
  exportCsv: (params: { from?: string; to?: string; patientId?: number } = {}) =>
    api.get<Blob>('/assessments/export', { params, responseType: 'blob' }).then((r) => r.data),
};

export const statsApi = {
  summary: () => api.get<Summary>('/stats/summary').then((r) => r.data),
  monthly: () => api.get<MonthlyPoint[]>('/stats/monthly').then((r) => r.data),
  model: () => api.get<ModelPerformance>('/stats/model').then((r) => r.data),
};
