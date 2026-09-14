import { apiClient } from './client';

export interface MerchantProfileResponse {
  id: number;
  email: string;
  name: string;
  address: string;
  businessHours: string;
  isActive: boolean;
}

export interface MerchantProfileRequest {
  name: string;
  address: string;
  businessHours?: string;
}

export interface MerchantDashboardResponse {
  todayOrderCount: number;
  todayRevenue: number;
  activeListingCount: number;
}

export const getMerchantProfile = () =>
  apiClient.get<{ code: number; data: MerchantProfileResponse }>('/api/merchant/profile');

export const updateMerchantProfile = (data: MerchantProfileRequest) =>
  apiClient.put<{ code: number; data: MerchantProfileResponse }>('/api/merchant/profile', data);

export const getMerchantDashboard = () =>
  apiClient.get<{ code: number; data: MerchantDashboardResponse }>('/api/merchant/dashboard');
