import { createContext, useCallback, useContext, useEffect, useMemo, useState, type ReactNode } from 'react';
import { setSessionExpiredHandler } from '../api/client';
import { authApi } from '../api/endpoints';
import type { AuthResponse, RoleName, User } from '../api/types';
import { tokenStorage } from './tokenStorage';

interface AuthContextValue {
  user: User | null;
  isAuthenticated: boolean;
  login: (email: string, password: string) => Promise<User>;
  register: (body: { fullName: string; email: string; password: string; role: RoleName }) => Promise<User>;
  logout: () => void;
  hasRole: (...roles: RoleName[]) => boolean;
}

const AuthContext = createContext<AuthContextValue | undefined>(undefined);

export function AuthProvider({ children }: { children: ReactNode }) {
  const [user, setUser] = useState<User | null>(() => (tokenStorage.getAccessToken() ? tokenStorage.getUser() : null));

  const accept = useCallback((res: AuthResponse) => {
    tokenStorage.save(res.accessToken, res.refreshToken, res.user);
    setUser(res.user);
    return res.user;
  }, []);

  const logout = useCallback(() => {
    tokenStorage.clear();
    setUser(null);
  }, []);

  useEffect(() => {
    setSessionExpiredHandler(() => setUser(null));
  }, []);

  const value = useMemo<AuthContextValue>(
    () => ({
      user,
      isAuthenticated: user !== null,
      login: (email, password) => authApi.login(email, password).then(accept),
      register: (body) => authApi.register(body).then(accept),
      logout,
      hasRole: (...roles) => !!user && user.roles.some((r) => roles.includes(r)),
    }),
    [user, accept, logout],
  );

  return <AuthContext.Provider value={value}>{children}</AuthContext.Provider>;
}

export function useAuth(): AuthContextValue {
  const ctx = useContext(AuthContext);
  if (!ctx) throw new Error('useAuth must be used inside AuthProvider');
  return ctx;
}
