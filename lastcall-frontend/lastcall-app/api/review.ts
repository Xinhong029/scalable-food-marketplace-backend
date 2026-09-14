import { apiClient } from './client';

export interface ReviewResponse {
  id: number;
  orderId: number;
  userId: number;
  merchantId: number;
  templateId: number;
  rating: number;
  content: string;
  imageUrls: string[];
  createdAt: string;
}

export interface CreateReviewRequest {
  orderId: number;
  rating: number;
  content?: string;
}

export const createReview = (data: CreateReviewRequest) =>
  apiClient.post<{ code: number; data: ReviewResponse }>('/api/reviews', data);

export const getReviewByOrder = (orderId: number) =>
  apiClient.get<{ code: number; data: ReviewResponse | null }>(`/api/reviews/order/${orderId}`);

export const getReviewsByTemplate = (templateId: number, page = 0, size = 5) =>
  apiClient.get<{ code: number; data: { content: ReviewResponse[]; totalElements: number } }>(
    `/api/reviews/template/${templateId}`, { params: { page, size } }
  );
