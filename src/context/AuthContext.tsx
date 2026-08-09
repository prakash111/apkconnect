import React, { createContext, useCallback, useContext, useEffect, useMemo, useState } from 'react';
import AsyncStorage from '@react-native-async-storage/async-storage';
import * as authApi from '../api/auth';
import { getBaseUrl } from '../api/client';
import { Limits, User } from '../types';

interface AuthContextValue {
  isLoading: boolean;
  isLoggedIn: boolean;
  user: User | null;
  limits: Limits | null;
  serverConfigured: boolean;
  refreshServerConfigured: () => Promise<void>;
  login: (login: string, password: string) => Promise<{ ok: boolean; message?: string }>;
  register: (
    email: string,
    username: string,
    password: string,
  ) => Promise<{ ok: boolean; message?: string }>;
  logout: () => Promise<void>;
  refreshLimits: () => Promise<void>;
}

const AuthContext = createContext<AuthContextValue | undefined>(undefined);

const USER_KEY = 'apktool.user';

export function AuthProvider({ children }: { children: React.ReactNode }) {
  const [isLoading, setIsLoading] = useState(true);
  const [user, setUser] = useState<User | null>(null);
  const [limits, setLimits] = useState<Limits | null>(null);
  const [serverConfigured, setServerConfigured] = useState(false);

  const refreshServerConfigured = useCallback(async () => {
    const url = await getBaseUrl();
    setServerConfigured(!!url);
  }, []);

  useEffect(() => {
    (async () => {
      await refreshServerConfigured();
      const raw = await AsyncStorage.getItem(USER_KEY);
      if (raw) {
        try {
          setUser(JSON.parse(raw));
        } catch {
          // ignore corrupt cache
        }
      }
      setIsLoading(false);
      refreshLimits().catch(() => {});
    })();
  }, [refreshServerConfigured]);

  const refreshLimits = useCallback(async () => {
    try {
      const res = await authApi.getLimits();
      if (res.status === 'success') {
        setLimits(res.limits);
        if (res.user?.user_type) {
          const updatedUser: User = {
            id: res.user.id,
            username: res.user.username,
            user_type: res.user.user_type,
          };
          setUser(updatedUser);
          await AsyncStorage.setItem(USER_KEY, JSON.stringify(updatedUser));
        }
      }
    } catch {
      // non-fatal
    }
  }, []);

  const login = useCallback(async (loginId: string, password: string) => {
    const res = await authApi.login(loginId, password);
    if (res.status === 'success') {
      const nextUser: User = {
        id: res.user?.id,
        username: res.user?.username || loginId,
        user_type: res.user?.user_type || 'user',
      };
      setUser(nextUser);
      await AsyncStorage.setItem(USER_KEY, JSON.stringify(nextUser));
      refreshLimits().catch(() => {});
      return { ok: true };
    }
    return { ok: false, message: res.message };
  }, [refreshLimits]);

  const register = useCallback(async (email: string, username: string, password: string) => {
    const res = await authApi.register(email, username, password);
    return { ok: res.status === 'success', message: res.message };
  }, []);

  const logout = useCallback(async () => {
    try {
      await authApi.logout();
    } catch {
      // ignore network errors on logout
    }
    setUser(null);
    setLimits(null);
  }, []);

  const value = useMemo(
    () => ({
      isLoading,
      isLoggedIn: !!user,
      user,
      limits,
      serverConfigured,
      refreshServerConfigured,
      login,
      register,
      logout,
      refreshLimits,
    }),
    [isLoading, user, limits, serverConfigured, refreshServerConfigured, login, register, logout, refreshLimits],
  );

  return <AuthContext.Provider value={value}>{children}</AuthContext.Provider>;
}

export function useAuth() {
  const ctx = useContext(AuthContext);
  if (!ctx) throw new Error('useAuth must be used within AuthProvider');
  return ctx;
}
