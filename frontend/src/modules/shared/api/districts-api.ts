import axios from 'axios';
import { API_BASE_URL } from '@/lib/api';

const API_URL = `${API_BASE_URL}/api/districts`;

export interface District {
  id: string;
  name: string;
}

// Get all districts (from cache)
export const getAllDistricts = async (): Promise<District[]> => {
  try {
    const response = await axios.get<District[]>(API_URL);
    return response.data;
  } catch (error) {
    if (axios.isAxiosError(error) && error.response) {
      throw new Error(error.response.data.message || 'Failed to fetch districts');
    }
    throw new Error('Network error or unknown issue');
  }
};
