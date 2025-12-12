import { API_BASE_URL } from '@/lib/api';

export interface CreateReviewRequest {
  bookingId: string;
  rating: number;
  comment: string;
  type?: 'RENTER_TO_OWNER' | 'OWNER_TO_RENTER';
}

export interface ReviewResponse {
  id: string;
  bookingId: string;
  reviewerId: string;
  reviewerName: string;
  ownerId: string;
  toolId: string;
  rating: number;
  comment: string;
  date: string;
}

export async function createReview(request: CreateReviewRequest): Promise<ReviewResponse> {
  const response = await fetch(`${API_BASE_URL}/api/reviews`, {
    method: 'POST',
    headers: {
      'Content-Type': 'application/json',
    },
    body: JSON.stringify(request),
  });

  if (!response.ok) {
    const errorData = await response.json();
    throw new Error(errorData.message || 'Failed to create review');
  }

  return response.json();
}

export async function updateReview(reviewId: string, request: CreateReviewRequest): Promise<ReviewResponse> {
  const response = await fetch(`${API_BASE_URL}/api/reviews/${reviewId}`, {
    method: 'PUT',
    headers: {
      'Content-Type': 'application/json',
    },
    body: JSON.stringify(request),
  });

  if (!response.ok) {
    const errorData = await response.json();
    throw new Error(errorData.message || 'Failed to update review');
  }

  return response.json();
}
