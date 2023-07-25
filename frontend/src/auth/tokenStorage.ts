import type { User } from '../api/types';

const ACCESS = 'crp.accessToken';
const REFRESH = 'crp.refreshToken';
const USER = 'crp.user';

function safeGet(key: string): string | null {
  try {
    return localStorage.getItem(key);
  } catch {
    return null;
  }
}

export const tokenStorage = {
  getAccessToken: () => safeGet(ACCESS),
  getRefreshToken: () => safeGet(REFRESH),
  getUser(): User | null {
    const raw = safeGet(USER);
    if (!raw) return null;
    try {
      return JSON.parse(raw) as User;
    } catch {
      return null;
    }
  },
  save(accessToken: string, refreshToken: string, user?: User) {
    try {
      localStorage.setItem(ACCESS, accessToken);
      localStorage.setItem(REFRESH, refreshToken);
      if (user) localStorage.setItem(USER, JSON.stringify(user));
    } catch {
      // storage may be unavailable in private mode, session just will not persist
    }
  },
  clear() {
    try {
      [ACCESS, REFRESH, USER].forEach((k) => localStorage.removeItem(k));
    } catch {
      // ignore
    }
  },
};
