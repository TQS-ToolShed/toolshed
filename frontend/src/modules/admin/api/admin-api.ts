import axios from 'axios';
import { API_BASE_URL } from '@/lib/api';
import type { User } from '@/modules/auth/context/AuthContext';

const API_URL = `${API_BASE_URL}/api/admin`;

export interface AdminStats {
  totalUsers: number;
  activeUsers: number;
  inactiveUsers: number;
  totalBookings: number;
  activeBookings: number;
  completedBookings: number;
  cancelledBookings: number;
  totalRevenue: number;
}

export const getAdminStats = async (): Promise<AdminStats> => {
  try {
    const response = await axios.get<AdminStats>(`${API_URL}/stats`);
    return response.data;
  } catch (error) {
    throw new Error('Failed to load admin stats');
  }
};

export const getUsers = async (): Promise<User[]> => {
  try {
    const response = await axios.get<User[]>(`${API_URL}/users`);
    return response.data;
  } catch (error) {
    throw new Error('Failed to load users');
  }
};

export const activateUser = async (userId: string): Promise<User> => {
  try {
    const response = await axios.post<User>(`${API_URL}/users/${userId}/activate`);
    return response.data;
  } catch (error) {
    throw new Error('Failed to activate user');
  }
};

export const deactivateUser = async (userId: string): Promise<User> => {
  try {
    const response = await axios.post<User>(`${API_URL}/users/${userId}/deactivate`);
    return response.data;
  } catch (error) {
    throw new Error('Failed to deactivate user');
  }
};
