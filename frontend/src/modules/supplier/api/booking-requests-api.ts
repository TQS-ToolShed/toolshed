import axios from 'axios';
import { API_BASE_URL } from '@/lib/api';
import type { ReviewResponse } from '../../renter/api/reviews-api';

const API_URL = `${API_BASE_URL}/api/bookings`;

export type BookingStatus = 'PENDING' | 'APPROVED' | 'REJECTED' | 'CANCELLED' | 'COMPLETED';
export type ConditionStatus = 'OK' | 'USED' | 'MINOR_DAMAGE' | 'BROKEN' | 'MISSING_PARTS';
export type DepositStatus = 'NOT_REQUIRED' | 'REQUIRED' | 'PAID';

export interface SupplierBookingRequest {
  id: string;
  toolId: string;
  toolTitle?: string;
  renterId: string;
  renterName?: string;
  startDate: string;
  endDate: string;
  status: BookingStatus;
  totalPrice?: number;
  review?: ReviewResponse;
  ownerReview?: ReviewResponse;
  // Condition Report Fields
  conditionStatus?: ConditionStatus;
  conditionDescription?: string;
  conditionReportedAt?: string;
  conditionReportedByName?: string;
  // Deposit Fields
  depositStatus?: DepositStatus;
  depositAmount?: number;
  depositPaidAt?: string;
}

export const getBookingsForOwner = async (ownerId: string): Promise<SupplierBookingRequest[]> => {
  try {
    const response = await axios.get<SupplierBookingRequest[]>(API_URL, {
      params: { ownerId },
    });
    return response.data;
  } catch (error) {
    if (axios.isAxiosError(error) && error.response) {
      throw new Error(error.response.data.message || 'Failed to fetch booking requests');
    }
    throw new Error('Network error or unknown issue');
  }
};

export const updateBookingStatus = async (
  bookingId: string,
  status: Extract<BookingStatus, 'APPROVED' | 'REJECTED'>
): Promise<void> => {
  try {
    await axios.put(`${API_URL}/${bookingId}/status`, { status });
  } catch (error) {
    if (axios.isAxiosError(error) && error.response) {
      throw new Error(error.response.data.message || 'Failed to update booking status');
    }
    throw new Error('Network error or unknown issue');
  }
};
