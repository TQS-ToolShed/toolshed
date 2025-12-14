import axios from "axios";
import { API_BASE_URL } from "@/lib/api";

const REPORTS_URL = `${API_BASE_URL}/api/reports`;

export interface ReportPayload {
  reporterId: string;
  toolId?: string;
  bookingId?: string;
  title: string;
  description: string;
}

export const createReport = async (payload: ReportPayload) => {
  const response = await axios.post(REPORTS_URL, payload);
  return response.data;
};
