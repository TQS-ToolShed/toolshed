import axios from 'axios';
import { API_BASE_URL } from '@/lib/api';
import type { RegisterRequest } from '../dto/RegisterRequest';
import type { LoginRequest } from '../dto/LoginRequest';

const API_URL = `${API_BASE_URL}/api/auth`;

// Function to register a new user
export const registerUser = async (userData: RegisterRequest) => {
  try {
    const response = await axios.post(`${API_URL}/register`, userData);
    return response.data;
  } catch (error) {
    if (axios.isAxiosError(error) && error.response) {
      // ResponseStatusException returns error in response.data as either string or object with message/reason
      const data = error.response.data;
      if (typeof data === 'string') {
        throw new Error(data);
      } else if (data.message) {
        throw new Error(data.message);
      } else if (data.reason) {
        throw new Error(data.reason);
      }
      throw new Error('Registration failed');
    }
    throw new Error('Network error or unknown issue');
  }
};

// Function to login a user
export const loginUser = async (credentials: LoginRequest) => {
  try {
    const response = await axios.post(`${API_URL}/login`, credentials);
    return response.data;
  } catch (error) {
    if (axios.isAxiosError(error) && error.response) {
      // ResponseStatusException returns error in response.data as either string or object with message/reason
      const data = error.response.data;
      if (typeof data === 'string') {
        throw new Error(data);
      } else if (data.message) {
        throw new Error(data.message);
      } else if (data.reason) {
        throw new Error(data.reason);
      }
      throw new Error('Login failed');
    }
    throw new Error('Network error or unknown issue');
  }
};

// Function to check if an email is already taken
export const checkEmailTaken = async (email: string): Promise<boolean> => {
  try {
    const response = await axios.get(`${API_URL}/check-email`, {
      params: { email }
    });
    return response.data; // Expecting boolean true if exists, false if not
  } catch (error) {
    // Handle error, e.g., network issues. For this check, return true to prevent registration
    console.error('Error checking email availability:', error);
    return true; // Assume email is taken on error to prevent overwriting
  }
};
