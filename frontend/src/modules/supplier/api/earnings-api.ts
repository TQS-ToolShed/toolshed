import axios from 'axios';
import { API_BASE_URL } from '@/lib/api';

const API_URL = `${API_BASE_URL}/api/bookings`;

export interface MonthlyEarnings {
  year: number;
  month: number;
  totalEarnings: number;
  bookingCount: number;
}

export interface OwnerEarningsResponse {
  monthlyEarnings: MonthlyEarnings[];
  totalEarnings: number;
}

export const getOwnerEarnings = async (ownerId: string): Promise<OwnerEarningsResponse> => {
  try {
    const response = await axios.get<OwnerEarningsResponse>(`${API_URL}/earnings`, {
      params: { ownerId },
    });
    return response.data;
  } catch (error) {
    if (axios.isAxiosError(error) && error.response) {
      throw new Error(error.response.data.message || 'Failed to fetch earnings');
    }
    throw new Error('Network error or unknown issue');
  }
};
