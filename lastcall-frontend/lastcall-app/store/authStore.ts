import { create } from 'zustand';
import * as SecureStore from 'expo-secure-store';

interface AuthState {
  token: string | null;
  userId: number | null;
  role: 'ROLE_USER' | 'ROLE_MERCHANT' | null;
  isLoaded: boolean;
  setAuth: (token: string, userId: number, role: string) => Promise<void>;
  clearAuth: () => Promise<void>;
  loadAuth: () => Promise<void>;
}

export const useAuthStore = create<AuthState>((set) => ({
  token: null,
  userId: null,
  role: null,
  isLoaded: false,

  setAuth: async (token, userId, role) => {
    await SecureStore.setItemAsync('access_token', token);
    await SecureStore.setItemAsync('user_id', String(userId));
    await SecureStore.setItemAsync('role', role);
    set({ token, userId, role: role as AuthState['role'], isLoaded: true });
  },

  clearAuth: async () => {
    await SecureStore.deleteItemAsync('access_token');
    await SecureStore.deleteItemAsync('user_id');
    await SecureStore.deleteItemAsync('role');
    set({ token: null, userId: null, role: null });
  },

  loadAuth: async () => {
    const token = await SecureStore.getItemAsync('access_token');
    const userIdStr = await SecureStore.getItemAsync('user_id');
    const role = await SecureStore.getItemAsync('role');
    set({
      token,
      userId: userIdStr ? Number(userIdStr) : null,
      role: role as AuthState['role'],
      isLoaded: true,
    });
  },
}));
