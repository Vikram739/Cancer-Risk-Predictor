import { afterEach, beforeEach, describe, expect, it, vi } from 'vitest';
import axios, { AxiosError, AxiosHeaders, type InternalAxiosRequestConfig } from 'axios';
import { api, errorMessage, refreshAccessToken, setSessionExpiredHandler } from './client';
import { tokenStorage } from '../auth/tokenStorage';

function axiosError(status: number, data: unknown, config: Partial<InternalAxiosRequestConfig> = {}) {
  const cfg = { headers: new AxiosHeaders(), url: '/patients', ...config } as InternalAxiosRequestConfig;
  return new AxiosError('fail', 'ERR', cfg, null, {
    status,
    statusText: '',
    headers: {},
    config: cfg,
    data,
  });
}

describe('errorMessage', () => {
  it('prefers the API message and field details', () => {
    const err = axiosError(400, {
      message: 'Validation failed',
      details: [{ field: 'mrn', message: 'must not be blank' }],
    });
    expect(errorMessage(err)).toBe('Validation failed: mrn must not be blank');
  });

  it('describes network failures', () => {
    const err = new AxiosError('Network Error', 'ERR_NETWORK');
    expect(errorMessage(err)).toBe('Cannot reach the server, check your connection');
  });

  it('falls back for unknown values', () => {
    expect(errorMessage(42, 'fallback')).toBe('fallback');
  });
});

describe('token refresh', () => {
  beforeEach(() => tokenStorage.save('old-access', 'refresh-1'));
  afterEach(() => {
    vi.restoreAllMocks();
  });

  it('shares one refresh call between concurrent callers', async () => {
    const post = vi.spyOn(axios, 'post').mockResolvedValue({
      data: { accessToken: 'new-access', refreshToken: 'refresh-2', user: { id: 1, fullName: 'A', email: 'a@b.c', roles: [] } },
    });
    const [a, b] = await Promise.all([refreshAccessToken(), refreshAccessToken()]);
    expect(a).toBe('new-access');
    expect(b).toBe('new-access');
    expect(post).toHaveBeenCalledTimes(1);
    expect(tokenStorage.getRefreshToken()).toBe('refresh-2');
  });

  it('retries the original request after a 401', async () => {
    vi.spyOn(axios, 'post').mockResolvedValue({
      data: { accessToken: 'new-access', refreshToken: 'refresh-2', user: { id: 1, fullName: 'A', email: 'a@b.c', roles: [] } },
    });
    const seen: string[] = [];
    api.defaults.adapter = async (config) => {
      const auth = String(config.headers.Authorization);
      seen.push(auth);
      if (auth === 'Bearer old-access') throw axiosError(401, {}, config);
      return { data: 'ok', status: 200, statusText: 'OK', headers: {}, config };
    };

    const res = await api.get('/patients');
    expect(res.data).toBe('ok');
    expect(seen).toEqual(['Bearer old-access', 'Bearer new-access']);
  });

  it('clears the session when refresh fails', async () => {
    vi.spyOn(axios, 'post').mockRejectedValue(new Error('expired'));
    const expired = vi.fn();
    setSessionExpiredHandler(expired);
    api.defaults.adapter = async (config) => {
      throw axiosError(401, {}, config);
    };

    await expect(api.get('/patients')).rejects.toBeInstanceOf(AxiosError);
    expect(expired).toHaveBeenCalled();
    expect(tokenStorage.getAccessToken()).toBeNull();
  });
});
