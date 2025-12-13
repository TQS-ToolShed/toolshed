import { useEffect, useMemo, useState } from "react";
import {
  getBookingsForRenter,
  type BookingResponse,
} from "../api/bookings-api";
import { handlePayDeposit } from "../api/payment-api";
import { useAuth } from "@/modules/auth/context/AuthContext";
import { ReviewOwnerModal } from "./ReviewOwnerModal";
import { ReviewToolModal } from "./ReviewToolModal";
import { ConditionReportModal } from "./ConditionReportModal";

interface RentalHistoryModalProps {
  open: boolean;
  onClose: () => void;
}

export const RentalHistoryModal = ({
  open,
  onClose,
}: RentalHistoryModalProps) => {
  const { user } = useAuth();
  const [bookings, setBookings] = useState<BookingResponse[]>([]);
  const [isLoading, setIsLoading] = useState(false);
  const [error, setError] = useState<string | null>(null);
  const [selectedBooking, setSelectedBooking] =
    useState<BookingResponse | null>(null);
  const [isReviewModalOpen, setIsReviewModalOpen] = useState(false);
  const [isToolReviewModalOpen, setIsToolReviewModalOpen] = useState(false);
  const [isConditionModalOpen, setIsConditionModalOpen] = useState(false);
  const [isPayingDeposit, setIsPayingDeposit] = useState<string | null>(null);

  const loadBookings = async () => {
    if (!user?.id) return;
    try {
      setIsLoading(true);
      setError(null);
      const data = await getBookingsForRenter(user.id);
      setBookings(data);
    } catch (err) {
      setError(err instanceof Error ? err.message : "Failed to load history");
    } finally {
      setIsLoading(false);
    }
  };

  useEffect(() => {
    if (open) {
      loadBookings();
    }
  }, [open, user?.id]);

  const pastBookings = useMemo(
    () =>
      bookings
        .filter(
          (booking) =>
            booking.status === "COMPLETED" || booking.status === "CANCELLED"
        )
        .sort(
          (a, b) =>
            new Date(b.startDate).getTime() - new Date(a.startDate).getTime()
        ),
    [bookings]
  );

  const statusStyles: Record<string, string> = {
    COMPLETED: "bg-emerald-100 text-emerald-800",
    CANCELLED: "bg-amber-100 text-amber-800",
    REJECTED: "bg-red-100 text-red-800",
    APPROVED: "bg-blue-100 text-blue-800",
    PENDING: "bg-gray-100 text-gray-800",
  };

  const conditionStyles: Record<string, string> = {
    OK: "bg-emerald-100 text-emerald-800",
    USED: "bg-blue-100 text-blue-800",
    MINOR_DAMAGE: "bg-amber-100 text-amber-800",
    BROKEN: "bg-red-100 text-red-800",
    MISSING_PARTS: "bg-orange-100 text-orange-800",
  };

  const depositStyles: Record<string, string> = {
    NOT_REQUIRED: "bg-gray-100 text-gray-600",
    REQUIRED: "bg-amber-100 text-amber-800",
    PAID: "bg-emerald-100 text-emerald-800",
  };

  const onPayDepositClick = async (bookingId: string) => {
    if (!user?.id) return;
    try {
      setIsPayingDeposit(bookingId);
      // Redirects to Stripe checkout
      await handlePayDeposit(bookingId);
      // Note: Page will redirect, but in case it doesn't:
      await loadBookings();
    } catch (err) {
      setError(err instanceof Error ? err.message : 'Failed to initiate deposit payment');
    } finally {
      setIsPayingDeposit(null);
    }
  };

  if (!open) return null;

  return (
    <>
      <div className="fixed inset-0 z-50 flex items-center justify-center bg-black/40 backdrop-blur-sm px-4">
        <div className="bg-background border border-border rounded-xl shadow-xl w-full max-w-3xl max-h-[80vh] overflow-hidden flex flex-col">
          <div className="flex items-center justify-between px-5 py-4 border-b border-border">
            <div>
              <p className="text-sm text-muted-foreground">Rental history</p>
              <h2 className="text-xl font-semibold">Past bookings</h2>
            </div>
            <button
              type="button"
              onClick={onClose}
              className="text-sm text-muted-foreground hover:text-foreground"
            >
              Close
            </button>
          </div>
          <div className="p-5 flex-1 overflow-y-auto space-y-3">
            {error && (
              <div className="bg-destructive/10 border border-destructive text-destructive px-3 py-2 rounded-md text-sm">
                {error}
              </div>
            )}
            {isLoading ? (
              <p className="text-sm text-muted-foreground">
                Loading history...
              </p>
            ) : pastBookings.length === 0 ? (
              <p className="text-sm text-muted-foreground">
                No past bookings yet.
              </p>
            ) : (
              <div className="space-y-3 max-h-[60vh] overflow-y-auto pr-1">
                {pastBookings.map((booking) => {
                  const badgeStyle =
                    statusStyles[booking.status] || "bg-gray-100 text-gray-800";
                  return (
                    <div
                      key={booking.id}
                      className="border border-border rounded-lg p-3 flex items-center justify-between gap-3"
                    >
                      <div className="space-y-1 flex-1">
                        <div className="flex items-center gap-2">
                          <p className="font-semibold text-foreground">
                            {booking.toolTitle ||
                              `Tool ${booking.toolId.slice(0, 6)}`}
                          </p>
                          <span
                            className={`px-2 py-1 rounded-full text-xs font-semibold ${badgeStyle}`}
                          >
                            {booking.status}
                          </span>
                        </div>
                        <p className="text-sm text-muted-foreground">
                          {new Date(booking.startDate).toLocaleDateString()} -{" "}
                          {new Date(booking.endDate).toLocaleDateString()}
                        </p>
                        <div className="text-xs text-muted-foreground flex flex-wrap gap-3">
                          <span className="text-foreground font-semibold">
                            €{booking.totalPrice.toFixed(2)}
                          </span>
                          <span>Payment: {booking.paymentStatus || "N/A"}</span>
                          <span>ID: {booking.id.slice(0, 8)}</span>
                          {booking.ownerName && (
                            <span>Owner: {booking.ownerName}</span>
                          )}
                        </div>

                        {/* Condition Report Status */}
                        {booking.conditionStatus && (
                          <div className="mt-2 flex flex-wrap items-center gap-2 text-xs">
                            <span
                              className={`px-2 py-1 rounded-full font-medium ${conditionStyles[booking.conditionStatus] || 'bg-gray-100 text-gray-800'}`}
                            >
                              Condition: {booking.conditionStatus.replace('_', ' ')}
                            </span>
                            {booking.depositStatus && (
                              <span
                                className={`px-2 py-1 rounded-full font-medium ${depositStyles[booking.depositStatus] || 'bg-gray-100 text-gray-800'}`}
                              >
                                Deposit: {booking.depositStatus.replace('_', ' ')}
                                {booking.depositAmount && booking.depositStatus !== 'NOT_REQUIRED' && ` (€${booking.depositAmount.toFixed(2)})`}
                              </span>
                            )}
                          </div>
                        )}
                        {booking.conditionDescription && (
                          <p className="mt-1 text-xs text-muted-foreground italic">
                            "{booking.conditionDescription}"
                          </p>
                        )}

                        {booking.review && (
                          <div className="mt-2 p-2 bg-muted/50 rounded-md text-sm">
                            <div className="flex items-center gap-1 mb-1">
                              <span className="font-medium text-xs uppercase tracking-wider text-muted-foreground">
                                Your Review:
                              </span>
                              <span className="text-yellow-500 text-xs">
                                {"★".repeat(booking.review.rating)}
                                <span className="text-gray-300">
                                  {"★".repeat(5 - booking.review.rating)}
                                </span>
                              </span>
                            </div>
                            <p className="text-muted-foreground italic text-xs">
                              "{booking.review.comment}"
                            </p>
                          </div>
                        )}

                        {booking.ownerReview && (
                          <div className="mt-2 p-2 bg-blue-50/50 rounded-md text-sm border border-blue-100">
                            <div className="flex items-center gap-1 mb-1">
                              <span className="font-medium text-xs uppercase tracking-wider text-blue-800">
                                Owner Review:
                              </span>
                              <span className="text-yellow-500 text-xs">
                                {"★".repeat(booking.ownerReview.rating)}
                                <span className="text-gray-300">
                                  {"★".repeat(5 - booking.ownerReview.rating)}
                                </span>
                              </span>
                            </div>
                            <p className="text-blue-900/80 italic text-xs">
                              "{booking.ownerReview.comment}"
                            </p>
                          </div>
                        )}

                        {booking.toolReview && (
                          <div className="mt-2 p-2 bg-green-50/50 rounded-md text-sm border border-green-100">
                            <div className="flex items-center gap-1 mb-1">
                              <span className="font-medium text-xs uppercase tracking-wider text-green-800">
                                Tool Review:
                              </span>
                              <span className="text-yellow-500 text-xs">
                                {"★".repeat(booking.toolReview.rating)}
                                <span className="text-gray-300">
                                  {"★".repeat(5 - booking.toolReview.rating)}
                                </span>
                              </span>
                            </div>
                            <p className="text-green-900/80 italic text-xs">
                              "{booking.toolReview.comment}"
                            </p>
                          </div>
                        )}
                      </div>
                      {booking.status === "COMPLETED" && (
                        <div className="flex flex-col gap-2">
                          {/* Condition Report Button - only show if no condition report yet */}
                          {!booking.conditionStatus && (
                            <button
                              onClick={() => {
                                setSelectedBooking(booking);
                                setIsConditionModalOpen(true);
                              }}
                              className="px-3 py-1.5 rounded-md text-sm font-medium whitespace-nowrap transition-colors bg-purple-600 text-white hover:bg-purple-700"
                            >
                              Report Condition
                            </button>
                          )}
                          {/* Pay Deposit Button - only show if deposit is required */}
                          {booking.depositStatus === 'REQUIRED' && (
                            <button
                              onClick={() => onPayDepositClick(booking.id)}
                              disabled={isPayingDeposit === booking.id}
                              className="px-3 py-1.5 rounded-md text-sm font-medium whitespace-nowrap transition-colors bg-amber-600 text-white hover:bg-amber-700 disabled:opacity-50"
                            >
                              {isPayingDeposit === booking.id ? 'Paying...' : `Pay Deposit €${booking.depositAmount?.toFixed(2)}`}
                            </button>
                          )}
                          <button
                            onClick={() => {
                              setSelectedBooking(booking);
                              setIsReviewModalOpen(true);
                            }}
                            className={`px-3 py-1.5 rounded-md text-sm font-medium whitespace-nowrap transition-colors ${
                              booking.review
                                ? "border border-input bg-background hover:bg-accent text-foreground"
                                : "bg-primary text-primary-foreground hover:bg-primary/90"
                            }`}
                          >
                            {booking.review ? "Edit Review" : "Review Owner"}
                          </button>
                          <button
                            onClick={() => {
                              setSelectedBooking(booking);
                              setIsToolReviewModalOpen(true);
                            }}
                            className={`px-3 py-1.5 rounded-md text-sm font-medium whitespace-nowrap transition-colors ${
                              booking.toolReview
                                ? "border border-input bg-background hover:bg-accent text-foreground"
                                : "bg-green-600 text-white hover:bg-green-700"
                            }`}
                          >
                            {booking.toolReview
                              ? "Edit Tool Review"
                              : "Review Tool"}
                          </button>
                        </div>
                      )}
                    </div>
                  );
                })}
              </div>
            )}
          </div>
        </div>
      </div>

      {selectedBooking && (
        <ReviewOwnerModal
          open={isReviewModalOpen}
          onClose={() => {
            setIsReviewModalOpen(false);
            setSelectedBooking(null);
          }}
          bookingId={selectedBooking.id}
          ownerName={selectedBooking.ownerName || "the owner"}
          existingReview={selectedBooking.review}
          onReviewSubmitted={() => {
            loadBookings();
          }}
        />
      )}

      {selectedBooking && (
        <ReviewToolModal
          open={isToolReviewModalOpen}
          onClose={() => {
            setIsToolReviewModalOpen(false);
            setSelectedBooking(null);
          }}
          bookingId={selectedBooking.id}
          toolName={selectedBooking.toolTitle || "the tool"}
          existingReview={selectedBooking.toolReview}
          onReviewSubmitted={() => {
            loadBookings();
          }}
        />
      )}

      {selectedBooking && (
        <ConditionReportModal
          open={isConditionModalOpen}
          onClose={() => {
            setIsConditionModalOpen(false);
            setSelectedBooking(null);
          }}
          bookingId={selectedBooking.id}
          toolName={selectedBooking.toolTitle || "the tool"}
          onSubmitted={() => {
            loadBookings();
          }}
        />
      )}
    </>
  );
};
