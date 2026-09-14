import { apiClient } from './client';

export interface PostResponse {
  id: number;
  userId: number;
  userNickname: string;
  userAvatarUrl: string;
  merchantId: number;
  merchantName: string;
  content: string;
  imageUrls: string[];
  likeCount: number;
  commentCount: number;
  createdAt: string;
}

export interface CreatePostRequest {
  content: string;
  merchantId?: number;
  imageUrls?: string[];
}

export interface PageResult<T> {
  content: T[];
  totalElements: number;
  totalPages: number;
  number: number;
}

export const getPosts = (page = 0, size = 20) =>
  apiClient.get<{ code: number; data: PageResult<PostResponse> }>('/api/posts', {
    params: { page, size },
  });

export const getMyPosts = (userId: number, page = 0, size = 20) =>
  apiClient.get<{ code: number; data: PageResult<PostResponse> }>(`/api/posts/user/${userId}`, {
    params: { page, size },
  });

export const createPost = (data: CreatePostRequest) =>
  apiClient.post<{ code: number; data: PostResponse }>('/api/posts', data);

export const deletePost = (postId: number) =>
  apiClient.delete(`/api/posts/${postId}`);
