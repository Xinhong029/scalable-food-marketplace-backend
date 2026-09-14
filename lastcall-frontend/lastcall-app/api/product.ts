import { apiClient } from './client';

export interface UserBrowseResponse {
  listingId: number;
  templateId: number;
  merchantId: number;
  merchantName: string;
  merchantAddress: string;
  productName: string;
  description: string;
  originalPrice: number;
  discountPrice: number;
  remainingQuantity: number;
  pickupStart: string;
  pickupEnd: string;
}

export interface SliceResult<T> {
  content: T[];
  page: number;
  size: number;
  hasNext: boolean;
}

export interface TemplateResponse {
  id: number;
  merchantId: number;
  name: string;
  description: string;
  originalPrice: number;
  isActive: boolean;
  createdAt: string;
}

export interface TemplateRequest {
  name: string;
  description?: string;
  originalPrice: number;
}

export interface ListingResponse {
  id: number;
  merchantId: number;
  templateId: number;
  templateName: string;
  originalPrice: number;
  discountPrice: number;
  quantity: number;
  remainingQuantity: number;
  pickupStart: string;
  pickupEnd: string;
  date: string;
  isAvailable: boolean;
}

export interface ListingRequest {
  templateId: number;
  discountPrice: number;
  quantity: number;
  pickupStart: string;
  pickupEnd: string;
  date: string;
}

export const browseListings = (page = 0, size = 20) =>
  apiClient.get<{ code: number; data: SliceResult<UserBrowseResponse> }>('/api/products/browse', {
    params: { page, size },
  });

export const getTemplates = () =>
  apiClient.get<{ code: number; data: TemplateResponse[] }>('/api/merchant/templates');

export const createTemplate = (data: TemplateRequest) =>
  apiClient.post<{ code: number; data: TemplateResponse }>('/api/merchant/templates', data);

export const updateTemplate = (id: number, data: TemplateRequest) =>
  apiClient.put<{ code: number; data: TemplateResponse }>(`/api/merchant/templates/${id}`, data);

export const deleteTemplate = (id: number) =>
  apiClient.delete(`/api/merchant/templates/${id}`);

export const getMerchantListings = () =>
  apiClient.get<{ code: number; data: ListingResponse[] }>('/api/merchant/listings');

export const createListing = (data: ListingRequest) =>
  apiClient.post<{ code: number; data: ListingResponse }>('/api/merchant/listings', data);

export const deleteListing = (id: number) =>
  apiClient.delete(`/api/merchant/listings/${id}`);
