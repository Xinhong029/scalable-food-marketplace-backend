import { apiClient } from './client';

export interface OrderResponse {
  id: number;
  listingId: number;
  merchantId: number;
  productName: string;
  price: number;
  status: 'PENDING_PAYMENT' | 'PAID' | 'COMPLETED' | 'CANCELLED';
  pickupCode: string;
  qrCodeContent: string;
  createdAt: string;
}

export interface CodeRequest {
  pickupCode?: string;
  qrCodeContent?: string;
}

export interface CodeResponse {
  orderId: number;
  customerNickname: string;
  productName: string;
  success: boolean;
  message: string;
}

export const createOrder = (listingId: number) =>
  apiClient.post<{ code: number; data: OrderResponse }>('/api/orders', { listingId });

export const getUserOrders = () =>
  apiClient.get<{ code: number; data: OrderResponse[] }>('/api/orders');

export const getOrderDetail = (id: number) =>
  apiClient.get<{ code: number; data: OrderResponse }>(`/api/orders/${id}`);

export const getPickupCode = (id: number) =>
  apiClient.get<{ code: number; data: OrderResponse }>(`/api/orders/${id}/pickup-code`);

export const getMerchantOrders = () =>
  apiClient.get<{ code: number; data: OrderResponse[] }>('/api/merchant/orders');

export const verifyPickupCode = (data: CodeRequest) =>
  apiClient.put<{ code: number; data: CodeResponse }>('/api/merchant/orders/verify', data);
