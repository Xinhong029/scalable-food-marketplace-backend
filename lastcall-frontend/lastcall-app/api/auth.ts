import { apiClient } from './client';

export interface AuthResponse {
  accessToken: string;
  tokenType: string;
  expiresIn: number;
  userId: number;
  email: string;
  role: string;
}

export interface LoginRequest {
  email: string;
  password: string;
}

export interface UserRegisterRequest {
  email: string;
  password: string;
  nickname: string;
}

export interface MerchantRegisterRequest {
  email: string;
  password: string;
  name: string;
  address: string;
}

export const loginUser = (data: LoginRequest) =>
  apiClient.post<{ code: number; data: AuthResponse }>('/api/auth/login/user', data);

export const loginMerchant = (data: LoginRequest) =>
  apiClient.post<{ code: number; data: AuthResponse }>('/api/auth/login/merchant', data);

export const registerUser = (data: UserRegisterRequest) =>
  apiClient.post<{ code: number; data: AuthResponse }>('/api/auth/register/user', data);

export const registerMerchant = (data: MerchantRegisterRequest) =>
  apiClient.post<{ code: number; data: AuthResponse }>('/api/auth/register/merchant', data);
