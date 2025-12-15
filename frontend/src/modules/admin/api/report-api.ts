import axios from "axios";
import { API_BASE_URL } from "@/lib/api";

export type ReportStatus = "OPEN" | "IN_PROGRESS" | "RESOLVED";

export interface Report {
  id: string;
  reporterId: string | null;
  reporterEmail: string | null;
  toolId: string | null;
  toolTitle: string | null;
  bookingId: string | null;
  title: string;
  description: string;
  status: ReportStatus;
  createdAt: string;
  updatedAt: string;
}

const REPORTS_URL = `${API_BASE_URL}/api/reports`;

export const fetchReports = async (status?: ReportStatus): Promise<Report[]> => {
  const response = await axios.get<Report[]>(REPORTS_URL, {
    params: status ? { status } : undefined,
  });
  return response.data;
};

export const updateReportStatus = async (id: string, status: ReportStatus) => {
  const response = await axios.put<Report>(`${REPORTS_URL}/${id}/status`, {
    status,
  });
  return response.data;
};

export const deleteReport = async (id: string) => {
  await axios.delete(`${REPORTS_URL}/${id}`);
};
