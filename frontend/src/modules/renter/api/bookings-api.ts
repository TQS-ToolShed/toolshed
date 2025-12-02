import axios from 'axios';

const API_URL = 'http://localhost:8080/api/bookings';

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
  startDate: string;
  endDate: string;
  status: string;
  paymentStatus: string;
  totalPrice: number;
}

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
