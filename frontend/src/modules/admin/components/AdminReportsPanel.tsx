import { useEffect, useState } from "react";
import { Card, CardContent, CardHeader, CardTitle } from "@/components/ui/card";
import { Badge } from "@/components/ui/badge";
import { Button } from "@/components/ui/button";
import type { Report, ReportStatus } from "../api/report-api";
import { fetchReports, updateReportStatus, deleteReport } from "../api/report-api";

const statusColors: Record<ReportStatus, string> = {
  OPEN: "bg-amber-100 text-amber-800",
  IN_PROGRESS: "bg-blue-100 text-blue-800",
  RESOLVED: "bg-emerald-100 text-emerald-800",
};

const statusOptions: ReportStatus[] = ["OPEN", "IN_PROGRESS", "RESOLVED"];

export const AdminReportsPanel = () => {
  const [reports, setReports] = useState<Report[]>([]);
  const [statusFilter, setStatusFilter] = useState<ReportStatus | "ALL">(
    "ALL"
  );
  const [error, setError] = useState<string | null>(null);
  const [isLoading, setIsLoading] = useState(false);
  const [updatingId, setUpdatingId] = useState<string | null>(null);

  const loadReports = async () => {
    try {
      setIsLoading(true);
      setError(null);
      const data = await fetchReports(
        statusFilter === "ALL" ? undefined : statusFilter
      );
      setReports(data);
    } catch (err) {
      setError("Failed to load reports");
    } finally {
      setIsLoading(false);
    }
  };

  useEffect(() => {
    loadReports();
    // eslint-disable-next-line react-hooks/exhaustive-deps
  }, [statusFilter]);

  const handleStatusChange = async (reportId: string, status: ReportStatus) => {
    try {
      setUpdatingId(reportId);
      await updateReportStatus(reportId, status);
      await loadReports();
    } catch (err) {
      setError("Failed to update report status");
    } finally {
      setUpdatingId(null);
    }
  };

  const handleDelete = async (reportId: string) => {
    try {
      setUpdatingId(reportId);
      await deleteReport(reportId);
      setReports((prev) => prev.filter((r) => r.id !== reportId));
    } catch (err) {
      setError("Failed to delete report");
    } finally {
      setUpdatingId(null);
    }
  };

  return (
    <Card>
      <CardHeader className="flex flex-col gap-2 sm:flex-row sm:items-center sm:justify-between">
        <div>
          <CardTitle>Reports</CardTitle>
          <p className="text-sm text-muted-foreground">
            Issues submitted by renters about their rentals
          </p>
        </div>
        <div className="flex gap-2">
          <select
            value={statusFilter}
            onChange={(e) =>
              setStatusFilter(e.target.value as ReportStatus | "ALL")
            }
            className="rounded-md border border-input bg-background px-3 py-2 text-sm"
          >
            <option value="ALL">All</option>
            {statusOptions.map((s) => (
              <option key={s} value={s}>
                {s}
              </option>
            ))}
          </select>
          <Button variant="outline" size="sm" onClick={loadReports}>
            Refresh
          </Button>
        </div>
      </CardHeader>
      <CardContent className="space-y-3">
        {error && (
          <div className="bg-destructive/10 border border-destructive text-destructive px-3 py-2 rounded-md text-sm">
            {error}
          </div>
        )}
        {isLoading ? (
          <p className="text-sm text-muted-foreground">Loading reports...</p>
        ) : reports.length === 0 ? (
          <p className="text-sm text-muted-foreground">No reports found.</p>
        ) : (
          <div className="space-y-3 max-h-[400px] overflow-y-auto pr-1">
            {reports.map((report) => (
              <div
                key={report.id}
                className="border border-border rounded-lg p-3 space-y-2"
              >
                <div className="flex items-start justify-between gap-2">
                  <div className="space-y-1">
                    <div className="flex items-center gap-2 flex-wrap">
                      <p className="font-semibold text-foreground">
                        {report.title}
                      </p>
                      <Badge
                        variant="secondary"
                        className={`${statusColors[report.status] ?? ""}`}
                      >
                        {report.status}
                      </Badge>
                    </div>
                    <p className="text-sm text-muted-foreground">
                      {report.description}
                    </p>
                    <div className="text-xs text-muted-foreground flex flex-wrap gap-2">
                      {report.reporterEmail && (
                        <span>Reporter: {report.reporterEmail}</span>
                      )}
                      {report.toolTitle && <span>Tool: {report.toolTitle}</span>}
                      {report.bookingId && (
                        <span>Booking: {report.bookingId.slice(0, 8)}</span>
                      )}
                      <span>
                        Created: {new Date(report.createdAt).toLocaleString()}
                      </span>
                    </div>
                  </div>
                  <div className="flex flex-col gap-2 items-end">
                    <select
                      value={report.status}
                      onChange={(e) =>
                        handleStatusChange(
                          report.id,
                          e.target.value as ReportStatus
                        )
                      }
                      disabled={updatingId === report.id}
                      className="rounded-md border border-input bg-background px-2 py-1 text-xs"
                    >
                      {statusOptions.map((s) => (
                        <option key={s} value={s}>
                          {s}
                        </option>
                      ))}
                    </select>
                    <Button
                      variant="destructive"
                      size="sm"
                      disabled={updatingId === report.id}
                      onClick={() => handleDelete(report.id)}
                    >
                      Delete
                    </Button>
                  </div>
                </div>
              </div>
            ))}
          </div>
        )}
      </CardContent>
    </Card>
  );
};
