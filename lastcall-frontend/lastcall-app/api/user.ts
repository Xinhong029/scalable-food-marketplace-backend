import { apiClient } from './client';

export interface UserProfileResponse {
  id: number;
  email: string;
  nickname: string;
  avatarUrl: string;
}

export interface FavoriteListingResponse {
  listingId: number;
  merchantId: number;
  merchantName: string;
  productName: string;
  description: string;
  originalPrice: number;
  discountPrice: number;
  remainingQuantity: number;
  pickupStart: string;
  pickupEnd: string;
  date: string;
  isAvailable: boolean;
  favoritedAt: string;
}

export const getUserProfile = () =>
  apiClient.get<{ code: number; data: UserProfileResponse }>('/api/user/profile');

export const updateUserProfile = (nickname: string) =>
  apiClient.put<{ code: number; data: UserProfileResponse }>('/api/user/profile', { nickname });

export const getFavorites = () =>
  apiClient.get<{ code: number; data: FavoriteListingResponse[] }>('/api/user/favorites');

export const addFavorite = (listingId: number) =>
  apiClient.post(`/api/user/favorites/${listingId}`);

export const removeFavorite = (listingId: number) =>
  apiClient.delete(`/api/user/favorites/${listingId}`);

export const checkFavorite = (listingId: number) =>
  apiClient.get<{ code: number; data: boolean }>(`/api/user/favorites/${listingId}`);
