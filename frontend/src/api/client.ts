import axios, { AxiosError, type AxiosInstance, type InternalAxiosRequestConfig } from 'axios';
import { tokenStorage } from '../auth/tokenStorage';
import type { ApiError, AuthResponse } from './types';

const baseURL = import.meta.env.VITE_API_URL ?? '/api';

export const api: AxiosInstance = axios.create({ baseURL, timeout: 20000 });

type RetriableConfig = InternalAxiosRequestConfig & { _retry?: boolean };

let refreshPromise: Promise<string> | null = null;
let onSessionExpired: () => void = () => undefined;

export function setSessionExpiredHandler(handler: () => void) {
  onSessionExpired = handler;
}

/** Concurrent 401s share one refresh call instead of each firing their own. */
export function refreshAccessToken(): Promise<string> {
  if (!refreshPromise) {
    const refreshToken = tokenStorage.getRefreshToken();
    refreshPromise = (
      refreshToken
        ? axios.post<AuthResponse>(`${baseURL}/auth/refresh`, { refreshToken }).then((res) => {
            tokenStorage.save(res.data.accessToken, res.data.refreshToken, res.data.user);
            return res.data.accessToken;
          })
        : Promise.reject(new Error('no refresh token'))
    ).finally(() => {
      refreshPromise = null;
    });
  }
  return refreshPromise;
}

api.interceptors.request.use((config) => {
  const token = tokenStorage.getAccessToken();
  if (token) {
    config.headers.Authorization = `Bearer ${token}`;
  }
  return config;
});

api.interceptors.response.use(
  (response) => response,
  async (error: AxiosError) => {
    const original = error.config as RetriableConfig | undefined;
    const isAuthCall = original?.url?.startsWith('/auth/');
    if (error.response?.status === 401 && original && !original._retry && !isAuthCall) {
      original._retry = true;
      try {
        const token = await refreshAccessToken();
        original.headers.Authorization = `Bearer ${token}`;
        return api(original);
      } catch {
        tokenStorage.clear();
        onSessionExpired();
      }
    }
    return Promise.reject(error);
  },
);

export function errorMessage(error: unknown, fallback = 'Something went wrong'): string {
  if (axios.isAxiosError(error)) {
    const body = error.response?.data as Partial<ApiError> | undefined;
    if (body?.details?.length) {
      return `${body.message ?? fallback}: ${body.details.map((d) => `${d.field} ${d.message}`).join(', ')}`;
    }
    if (body?.message) return body.message;
    if (error.code === 'ECONNABORTED') return 'The server took too long to respond';
    if (!error.response) return 'Cannot reach the server, check your connection';
  }
  return error instanceof Error && error.message ? error.message : fallback;
}
