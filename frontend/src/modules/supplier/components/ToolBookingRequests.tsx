import { useCallback, useEffect, useMemo, useState } from 'react';
import { useAuth } from '@/modules/auth/context/AuthContext';
import { Button } from '@/components/ui/button';
import { Card, CardContent, CardDescription, CardHeader, CardTitle } from '@/components/ui/card';
import {
  getBookingsForOwner,
  updateBookingStatus,
  type SupplierBookingRequest,
} from '../api/booking-requests-api';

const statusStyles: Record<string, string> = {
  PENDING: 'bg-amber-100 text-amber-800',
  APPROVED: 'bg-emerald-100 text-emerald-800',
  REJECTED: 'bg-rose-100 text-rose-800',
  CANCELLED: 'bg-gray-100 text-gray-800',
  COMPLETED: 'bg-blue-100 text-blue-800',
};

const formatDate = (date: string) => new Date(date).toLocaleDateString();

export const ToolBookingRequests = () => {
  const { user } = useAuth();
  const ownerId = user?.id;

  const [requests, setRequests] = useState<SupplierBookingRequest[]>([]);
  const [isLoading, setIsLoading] = useState(true);
  const [error, setError] = useState<string | null>(null);
  const [selectedRequest, setSelectedRequest] = useState<SupplierBookingRequest | null>(null);
  const [isUpdating, setIsUpdating] = useState(false);
  const [actionError, setActionError] = useState<string | null>(null);
  const [infoMessage, setInfoMessage] = useState<string | null>(null);
  const [showHistory, setShowHistory] = useState(false);
  const [openedFromHistory, setOpenedFromHistory] = useState(false);

  const pendingRequests = useMemo(
    () => requests.filter((req) => req.status === 'PENDING'),
    [requests]
  );

  const loadRequests = useCallback(async () => {
    if (!ownerId) {
      setError('You need to be logged in to view booking requests.');
      setIsLoading(false);
      return;
    }

    try {
      setIsLoading(true);
      setError(null);
      setInfoMessage(null);
      const data = await getBookingsForOwner(ownerId);
      setRequests(data);
    } catch (err) {
      setError(err instanceof Error ? err.message : 'Failed to load booking requests');
      setRequests([]);
    } finally {
      setIsLoading(false);
    }
  }, [ownerId]);

  useEffect(() => {
    loadRequests();
  }, [loadRequests]);

  const handleDecision = async (decision: 'APPROVED' | 'REJECTED') => {
    if (!selectedRequest) return;

    try {
      setIsUpdating(true);
      setActionError(null);
      await updateBookingStatus(selectedRequest.id, decision);
      setInfoMessage(`Booking ${decision === 'APPROVED' ? 'approved' : 'rejected'} successfully.`);
    } catch (err) {
      setActionError(
        err instanceof Error
          ? `${err.message} (updated locally only)`
          : 'Could not update on the server, updated locally only.'
      );
    } finally {
      setRequests((prev) =>
        prev.map((req) => (req.id === selectedRequest.id ? { ...req, status: decision } : req))
      );
      setSelectedRequest(null);
      setIsUpdating(false);
    }
  };

  return (
    <Card>
      <CardHeader>
        <div className="flex items-start justify-between gap-3">
          <div>
            <CardTitle>Booking requests</CardTitle>
            <CardDescription>Review incoming rental requests for your tools</CardDescription>
          </div>
          <Button variant="outline" size="sm" onClick={() => setShowHistory(true)}>
            Requests history
          </Button>
        </div>
      </CardHeader>
      <CardContent className="space-y-4">
        {error && (
          <div className="bg-destructive/10 border border-destructive text-destructive px-3 py-2 rounded-md text-sm">
            {error}
          </div>
        )}
        {infoMessage && (
          <div className="bg-emerald-50 border border-emerald-200 text-emerald-800 px-3 py-2 rounded-md text-sm">
            {infoMessage}
          </div>
        )}

        {isLoading ? (
          <p className="text-muted-foreground text-sm">Loading booking requests...</p>
        ) : pendingRequests.length === 0 ? (
          <p className="text-muted-foreground text-sm">
            No pending booking requests right now.
          </p>
        ) : (
          <div className="space-y-3">
            {pendingRequests.map((request) => {
              const badgeStyle = statusStyles[request.status] || 'bg-gray-100 text-gray-800';

              return (
                <div
                  key={request.id}
                  className="border border-border rounded-lg p-3 flex items-center justify-between gap-3"
                >
                  <div className="space-y-1">
                    <div className="flex items-center gap-2">
                      <p className="font-semibold text-foreground">
                        {request.toolTitle || 'Tool booking'}
                      </p>
                      <span
                        className={`px-2 py-1 rounded-full text-xs font-semibold ${badgeStyle}`}
                      >
                        {request.status}
                      </span>
                    </div>
                    <p className="text-sm text-muted-foreground">
                      {request.renterName || 'Renter'} • {formatDate(request.startDate)} -{' '}
                      {formatDate(request.endDate)}
                    </p>
                    {request.totalPrice && (
                      <p className="text-xs text-muted-foreground">
                        Total: <span className="text-foreground font-semibold">€{request.totalPrice.toFixed(2)}</span>
                      </p>
                    )}
                  </div>
                  <Button
                    variant={request.status === 'PENDING' ? 'default' : 'outline'}
                    onClick={() => {
                      setSelectedRequest(request);
                      setOpenedFromHistory(false);
                    }}
                    disabled={isUpdating}
                  >
                    {request.status === 'PENDING' ? 'Review' : 'View'}
                  </Button>
                </div>
              );
            })}
          </div>
        )}
      </CardContent>

      {selectedRequest && (
        <div className="fixed inset-0 bg-black/40 backdrop-blur-sm flex items-center justify-center z-50 px-4">
          <div className="bg-background border border-border rounded-lg shadow-xl w-full max-w-lg p-6 space-y-4">
            <div className="flex items-center gap-3 mb-2">
              <button
                type="button"
                className="inline-flex items-center justify-center rounded-md p-2 hover:bg-muted transition"
                aria-label="Back"
                onClick={() => {
                  if (openedFromHistory) {
                    setSelectedRequest(null);
                    setOpenedFromHistory(false);
                    setShowHistory(true);
                  } else {
                    setSelectedRequest(null);
                  }
                }}
              >
                ←
              </button>
              <div>
                <p className="text-sm text-muted-foreground mb-1">Booking request</p>
                <h3 className="text-xl font-semibold">{selectedRequest.toolTitle || 'Tool booking'}</h3>
              </div>
            </div>

            <div className="space-y-2 text-sm ml-9">
              <div className="flex items-center gap-2">
                <span className="font-semibold">Renter:</span>
                <span className="text-foreground">{selectedRequest.renterName || selectedRequest.renterId}</span>
              </div>
              <div className="flex items-center gap-2">
                <span className="font-semibold">Dates:</span>
                <span className="text-foreground">
                  {formatDate(selectedRequest.startDate)} - {formatDate(selectedRequest.endDate)}
                </span>
              </div>
              {selectedRequest.totalPrice && (
                <div className="flex items-center gap-2">
                  <span className="font-semibold">Total:</span>
                  <span className="text-foreground">€{selectedRequest.totalPrice.toFixed(2)}</span>
                </div>
              )}
              <div className="flex items-center gap-2">
                <span className="font-semibold">Status:</span>
                <span
                  className={`px-2 py-1 rounded-full text-xs font-semibold ${
                    statusStyles[selectedRequest.status] || 'bg-gray-100 text-gray-800'
                  }`}
                >
                  {selectedRequest.status}
                </span>
              </div>
            </div>

            {actionError && (
              <div className="bg-destructive/10 border border-destructive text-destructive px-3 py-2 rounded-md text-sm ml-9">
                {actionError}
              </div>
            )}

            {selectedRequest.status === 'PENDING' ? (
              <div className="flex items-center gap-3 justify-end ml-9">
                <Button
                  variant="outline"
                  onClick={() => {
                    if (openedFromHistory) {
                      setSelectedRequest(null);
                      setOpenedFromHistory(false);
                      setShowHistory(true);
                    } else {
                      setSelectedRequest(null);
                    }
                  }}
                  disabled={isUpdating}
                >
                  {openedFromHistory ? 'Back to history' : 'Cancel'}
                </Button>
                <Button
                  variant="destructive"
                  onClick={() => handleDecision('REJECTED')}
                  disabled={isUpdating}
                >
                  {isUpdating ? 'Updating...' : 'Reject'}
                </Button>
                <Button onClick={() => handleDecision('APPROVED')} disabled={isUpdating}>
                  {isUpdating ? 'Updating...' : 'Accept'}
                </Button>
              </div>
            ) : (
              <div className="flex items-center justify-end ml-9">
                <Button variant="outline" onClick={() => setSelectedRequest(null)}>
                  Close
                </Button>
              </div>
            )}
          </div>
        </div>
      )}

      {showHistory && (
        <div className="fixed inset-0 bg-black/40 backdrop-blur-sm flex items-center justify-center z-50 px-4">
          <div className="bg-background border border-border rounded-lg shadow-xl w-full max-w-3xl p-6 space-y-4">
            <div className="flex items-center justify-between">
              <div>
                <p className="text-sm text-muted-foreground mb-1">Requests history</p>
                <h3 className="text-xl font-semibold">All booking requests</h3>
              </div>
              <Button variant="ghost" onClick={() => setShowHistory(false)}>
                Close
              </Button>
            </div>

            {requests.length === 0 ? (
              <p className="text-sm text-muted-foreground">No booking requests yet.</p>
            ) : (
              <div className="space-y-3 max-h-[60vh] overflow-y-auto pr-1">
                {requests.map((request) => {
                  const badgeStyle = statusStyles[request.status] || 'bg-gray-100 text-gray-800';
                  return (
                    <div
                      key={request.id}
                      className="border border-border rounded-lg p-3 flex items-center justify-between gap-3"
                    >
                      <div className="space-y-1">
                        <div className="flex items-center gap-2">
                          <p className="font-semibold text-foreground">
                            {request.toolTitle || 'Tool booking'}
                          </p>
                          <span
                            className={`px-2 py-1 rounded-full text-xs font-semibold ${badgeStyle}`}
                          >
                            {request.status}
                          </span>
                        </div>
                        <p className="text-sm text-muted-foreground">
                          {request.renterName || 'Renter'} • {formatDate(request.startDate)} -{' '}
                          {formatDate(request.endDate)}
                        </p>
                        {request.totalPrice && (
                          <p className="text-xs text-muted-foreground">
                            Total: <span className="text-foreground font-semibold">€{request.totalPrice.toFixed(2)}</span>
                          </p>
                        )}
                      </div>
                      <Button
                        variant="outline"
                        size="sm"
                        onClick={() => {
                          setSelectedRequest(request);
                          setShowHistory(false);
                          setOpenedFromHistory(true);
                        }}
                      >
                        View
                      </Button>
                    </div>
                  );
                })}
              </div>
            )}
          </div>
        </div>
      )}
    </Card>
  );
};
