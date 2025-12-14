import axios from 'axios';
import { API_BASE_URL } from '@/lib/api';

const SUBSCRIPTION_API_URL = `${API_BASE_URL}/api/subscriptions`;

export interface SubscriptionStatus {
  tier: 'FREE' | 'PRO';
  active: boolean;
  subscriptionStart: string | null;
  subscriptionEnd: string | null;
  discountPercentage: number;
}

export interface CheckoutSessionResponse {
  sessionId: string;
  checkoutUrl: string;
}

/**
 * Get current subscription status for a user
 */
export const getSubscriptionStatus = async (userId: string): Promise<SubscriptionStatus> => {
  const response = await axios.get<SubscriptionStatus>(`${SUBSCRIPTION_API_URL}/status/${userId}`);
  return response.data;
};

/**
 * Create a Pro Member subscription checkout session
 */
export const createProSubscription = async (userId: string): Promise<CheckoutSessionResponse> => {
  const response = await axios.post<CheckoutSessionResponse>(`${SUBSCRIPTION_API_URL}/pro/${userId}`);
  return response.data;
};

/**
 * Activate subscription after successful checkout
 */
export const activateSubscription = async (
  userId: string,
  stripeSubscriptionId: string
): Promise<SubscriptionStatus> => {
  const response = await axios.post<SubscriptionStatus>(`${SUBSCRIPTION_API_URL}/activate/${userId}`, {
    stripeSubscriptionId,
  });
  return response.data;
};

/**
 * Cancel Pro subscription
 */
export const cancelSubscription = async (userId: string): Promise<void> => {
  await axios.delete(`${SUBSCRIPTION_API_URL}/${userId}`);
};
