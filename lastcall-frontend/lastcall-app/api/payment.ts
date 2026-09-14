import { apiClient } from './client';

export interface PaymentRequest {
  orderId: number;
  paymentMethodId: string;
  paymentType: string;
}

export interface PaymentResponse {
  orderId: number;
  status: string;
  paymentIntentId: string;
  requiresAction: boolean;
  clientSecret: string;
}

export const createPaymentIntent = (data: PaymentRequest) =>
  apiClient.post<{ code: number; data: PaymentResponse }>('/api/payment/create', data);
