import axios from 'axios';
import { API_BASE_URL } from '@/lib/api';

const API_URL = `${API_BASE_URL}/api/payments`;

export interface PayoutResponse {
  id: string;
  amount: number;
  status: 'PENDING' | 'COMPLETED' | 'FAILED';
  stripeTransferId: string;
  requestedAt: string;
  completedAt: string | null;
  description?: string;
  isIncome?: boolean;
}

export interface WalletResponse {
  balance: number;
  recentPayouts: PayoutResponse[];
}

export interface PayoutRequest {
  amount: number;
}

/**
 * Get owner wallet information including balance and recent payouts.
 */
export async function getOwnerWallet(ownerId: string): Promise<WalletResponse> {
  try {
    const response = await axios.get<WalletResponse>(`${API_URL}/wallet/${ownerId}`);
    return response.data;
  } catch (error) {
    if (axios.isAxiosError(error) && error.response) {
      throw new Error(error.response.data.message || 'Failed to fetch wallet');
    }
    throw new Error('Network error or unknown issue');
  }
}

/**
 * Get full payout history for an owner.
 */
export async function getPayoutHistory(ownerId: string): Promise<PayoutResponse[]> {
  try {
    const response = await axios.get<PayoutResponse[]>(`${API_URL}/payouts/${ownerId}`);
    return response.data;
  } catch (error) {
    if (axios.isAxiosError(error) && error.response) {
      throw new Error(error.response.data.message || 'Failed to fetch payouts');
    }
    throw new Error('Network error or unknown issue');
  }
}

/**
 * Request a payout for an owner.
 */
export async function requestPayout(ownerId: string, amount: number): Promise<PayoutResponse> {
  try {
    const response = await axios.post<PayoutResponse>(`${API_URL}/payout/${ownerId}`, { amount });
    return response.data;
  } catch (error) {
    if (axios.isAxiosError(error) && error.response) {
      throw new Error(error.response.data.message || 'Failed to process payout');
    }
    throw new Error('Network error or unknown issue');
  }
}

export interface MonthlyEarnings {
  month: string;
  year: number;
  amount: number;
}

/**
 * Get monthly earnings for an owner.
 */
export async function getMonthlyEarnings(ownerId: string): Promise<MonthlyEarnings[]> {
  try {
    const response = await axios.get<MonthlyEarnings[]>(`${API_URL}/wallet/${ownerId}/earnings`);
    return response.data;
  } catch (error) {
    if (axios.isAxiosError(error) && error.response) {
      throw new Error(error.response.data.message || 'Failed to fetch earnings');
    }
    throw new Error('Network error or unknown issue');
  }
}
