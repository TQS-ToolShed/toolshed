import axios from 'axios';
import type { RegisterRequest } from '../dto/RegisterRequest';
import type { LoginRequest } from '../dto/LoginRequest';

const API_URL = 'http://localhost:8080/api/auth'; // Assuming backend runs on 8080

// Function to register a new user
export const registerUser = async (userData: RegisterRequest) => {
  try {
    const response = await axios.post(`${API_URL}/register`, userData);
    return response.data;
  } catch (error) {
    if (axios.isAxiosError(error) && error.response) {
      throw new Error(error.response.data.message || 'Registration failed');
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
      throw new Error(error.response.data || 'Login failed'); // error.response.data might be the string message from backend
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
