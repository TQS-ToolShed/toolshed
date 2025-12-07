import axios from 'axios';
import { API_BASE_URL } from '@/lib/api';

const API_URL = `${API_BASE_URL}/api/bookings`;

export interface CreateBookingInput {
  toolId: string;
  renterId: string;
  startDate: string;
  endDate: string;
}

export interface BookingResponse {
  id: string;
  toolId: string;
  renterId: string;
  ownerId: string;
  toolTitle?: string;
  startDate: string;
  endDate: string;
  status: string;
  paymentStatus: string;
  totalPrice: number;
}

export const getBookingsForRenter = async (renterId: string): Promise<BookingResponse[]> => {
  try {
    const response = await axios.get<BookingResponse[]>(API_URL, { params: { renterId } });
    return response.data;
  } catch (error) {
    if (axios.isAxiosError(error) && error.response) {
      throw new Error(error.response.data.message || 'Failed to load bookings');
    }
    throw new Error('Network error or unknown issue');
  }
};

export const getBookingsForTool = async (toolId: string): Promise<BookingResponse[]> => {
  try {
    const response = await axios.get<BookingResponse[]>(API_URL, { params: { toolId } });
    return response.data.sort(
      (a, b) => new Date(a.startDate).getTime() - new Date(b.startDate).getTime()
    );
  } catch (error) {
    if (axios.isAxiosError(error) && error.response) {
      throw new Error(error.response.data.message || 'Failed to load bookings');
    }
    throw new Error('Network error or unknown issue');
  }
};

export const createBooking = async (input: CreateBookingInput): Promise<BookingResponse> => {
  try {
    const response = await axios.post<BookingResponse>(API_URL, input);
    return response.data;
  } catch (error) {
    if (axios.isAxiosError(error) && error.response) {
      throw new Error(error.response.data.message || 'Failed to create booking');
    }
    throw new Error('Network error or unknown issue');
  }
};
