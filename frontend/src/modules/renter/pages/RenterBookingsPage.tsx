import { useCallback, useEffect, useMemo, useState } from "react";
import type { FormEvent } from "react";
import { useNavigate, useParams } from "react-router-dom";
import { useAuth } from "@/modules/auth/context/AuthContext";
import { Button } from "@/components/ui/button";
import {
  Card,
  CardContent,
  CardDescription,
  CardFooter,
  CardHeader,
  CardTitle,
} from "@/components/ui/card";
import { Separator } from "@/components/ui/separator";
import { Input } from "@/components/ui/input";
import {
  getToolDetails,
  type ToolDetails,
} from "@/modules/supplier/api/tools-api";
import {
  createBooking,
  type BookingResponse,
  getBookingsForTool,
  getBookingsForRenter,
} from "@/modules/renter/api/bookings-api";
import {
  createReview,
  updateReview,
  type ReviewResponse,
} from "@/modules/renter/api/reviews-api";
import { ToolAvailabilityCard } from "../components/ToolAvailabilityCard";
import { BackToDashboardButton } from "../components/BackToDashboardButton";
import { RenterNavbar } from "../components/RenterNavbar";
import StarRating from "@/modules/shared/components/StarRating";
import { getSubscriptionStatus } from "@/api/subscription-api";
import { Crown } from "lucide-react";

export const RenterBookingsPage = () => {
  const { toolId } = useParams<{ toolId: string }>();
  const { user } = useAuth();
  const navigate = useNavigate();

  const [tool, setTool] = useState<ToolDetails | null>(null);
  const [isLoading, setIsLoading] = useState(true);
  const [isSubmitting, setIsSubmitting] = useState(false);
  const [error, setError] = useState<string | null>(null);
  const [success, setSuccess] = useState<string | null>(null);
  const [bookingResponse, setBookingResponse] =
    useState<BookingResponse | null>(null);
  const [toolBookings, setToolBookings] = useState<BookingResponse[]>([]);
  const [isLoadingReviews, setIsLoadingReviews] = useState(false);
  const [reviewSaving, setReviewSaving] = useState(false);
  const [reviewError, setReviewError] = useState<string | null>(null);
  const [reviewSuccess, setReviewSuccess] = useState<string | null>(null);
  const [reviewRating, setReviewRating] = useState(0);
  const [reviewComment, setReviewComment] = useState("");
  const [showReviewForm, setShowReviewForm] = useState(false);
  const [startDate, setStartDate] = useState("");
  const [endDate, setEndDate] = useState("");
  const [isPro, setIsPro] = useState(false);
  const [discountPercentage, setDiscountPercentage] = useState(0);

  const fetchTool = useCallback(async () => {
    if (!toolId) {
      setError("Missing tool id");
      setIsLoading(false);
      return;
    }

    try {
      setError(null);
      const data = await getToolDetails(toolId);
      setTool(data);
    } catch (err) {
      setError(err instanceof Error ? err.message : "Failed to load tool");
    } finally {
      setIsLoading(false);
    }
  }, [toolId]);

  useEffect(() => {
    fetchTool();
  }, [fetchTool]);

  // Fetch bookings for tool (reviews) and renter bookings (eligibility)
  useEffect(() => {
    const loadBookings = async () => {
      if (!toolId || !user?.id) return;
      try {
        setIsLoadingReviews(true);
        const toolData = await getBookingsForTool(toolId);
        setToolBookings(toolData);

        const myCompleted = toolData.find(
          (b) => b.renterId === user.id && b.status === "COMPLETED"
        );
        if (myCompleted?.toolReview) {
          setReviewRating(myCompleted.toolReview.rating);
          setReviewComment(myCompleted.toolReview.comment);
        } else {
          setReviewRating(0);
          setReviewComment("");
        }
      } catch (err) {
        console.error("Failed to load bookings for reviews", err);
      } finally {
        setIsLoadingReviews(false);
      }
    };
    loadBookings();
  }, [toolId, user?.id]);

  // Check Pro status for discount
  useEffect(() => {
    const checkProStatus = async () => {
      if (user?.id) {
        try {
          const status = await getSubscriptionStatus(user.id);
          setIsPro(status.active);
          setDiscountPercentage(status.discountPercentage || 0);
        } catch (error) {
          console.error('Failed to check Pro status:', error);
        }
      }
    };
    checkProStatus();
  }, [user?.id]);

  const rentalDays = useMemo(() => {
    if (!startDate || !endDate) return 0;
    const start = new Date(startDate);
    const end = new Date(endDate);
    const diff =
      Math.ceil((end.getTime() - start.getTime()) / (1000 * 60 * 60 * 24)) + 1;
    return diff > 0 ? diff : 0;
  }, [startDate, endDate]);

  const totalPrice = useMemo(() => {
    if (!tool) return 0;
    return rentalDays * tool.pricePerDay;
  }, [rentalDays, tool]);

  const discountedTotalPrice = useMemo(() => {
    if (!isPro || discountPercentage === 0) return totalPrice;
    return totalPrice * (1 - discountPercentage / 100);
  }, [totalPrice, isPro, discountPercentage]);

  // Security deposit - refundable if tool returned in good condition
  const SECURITY_DEPOSIT = 8.0;
  const grandTotal = useMemo(() => {
    return discountedTotalPrice + SECURITY_DEPOSIT;
  }, [discountedTotalPrice]);

  const datesInvalid = useMemo(() => {
    if (!startDate || !endDate) return true;
    const start = new Date(startDate);
    const end = new Date(endDate);
    return start > end;
  }, [startDate, endDate]);

  const toolReviews: ReviewResponse[] = useMemo(() => {
    return toolBookings
      .map((b) => b.toolReview)
      .filter((r): r is ReviewResponse => Boolean(r));
  }, [toolBookings]);

  const myCompletedBooking = useMemo(
    () =>
      toolBookings.find(
        (b) =>
          b.renterId === user?.id &&
          b.status === "COMPLETED" &&
          b.paymentStatus === "COMPLETED"
      ),
    [toolBookings, user?.id]
  );

  const myExistingReview = useMemo(
    () => myCompletedBooking?.toolReview,
    [myCompletedBooking]
  );

  const supplierRating = useMemo(() => {
    const ratings = toolBookings
      .map((b) => b.review?.rating)
      .filter((r): r is number => typeof r === "number");
    if (ratings.length === 0) return tool?.owner?.reputationScore ?? 0;
    const avg = ratings.reduce((sum, r) => sum + r, 0) / ratings.length;
    return Math.round(avg * 10) / 10;
  }, [toolBookings, tool?.owner?.reputationScore]);

  const handleSubmit = async (event: FormEvent<HTMLFormElement>) => {
    event.preventDefault();
    if (!tool || !user || !toolId || datesInvalid || rentalDays === 0) return;

    try {
      setIsSubmitting(true);
      setError(null);
      setSuccess(null);
      const response = await createBooking({
        toolId,
        renterId: user.id,
        startDate,
        endDate,
      });
      setBookingResponse(response);
      setSuccess(
        "Booking request sent to the owner. We will notify you once it is confirmed."
      );
    } catch (err) {
      setError(err instanceof Error ? err.message : "Failed to submit booking");
    } finally {
      setIsSubmitting(false);
    }
  };

  const handleReviewSubmit = async () => {
    if (!myCompletedBooking) return;
    try {
      setReviewSaving(true);
      setReviewError(null);
      setReviewSuccess(null);

      const payload = {
        bookingId: myCompletedBooking.id,
        rating: reviewRating,
        comment: reviewComment,
        type: "RENTER_TO_TOOL" as const,
      };

      if (myExistingReview) {
        await updateReview(myExistingReview.id, payload);
        setReviewSuccess("Review updated");
      } else {
        await createReview(payload);
        setReviewSuccess("Review submitted");
      }

      const toolData = await getBookingsForTool(toolId!);
      setToolBookings(toolData);
    } catch (err) {
      setReviewError(
        err instanceof Error ? err.message : "Failed to save review"
      );
    } finally {
      setReviewSaving(false);
    }
  };

  if (isLoading) {
    return (
      <div className="min-h-screen flex items-center justify-center">
        <p className="text-muted-foreground">Loading tool details...</p>
      </div>
    );
  }

  if (error || !tool) {
    return (
      <div className="min-h-screen flex flex-col items-center justify-center gap-4">
        <div className="bg-destructive/10 border border-destructive text-destructive px-4 py-3 rounded-lg">
          {error || "Tool not found"}
        </div>
        <Button variant="outline" onClick={() => navigate(-1)}>
          Go back
        </Button>
      </div>
    );
  }

  const todayISO = new Date().toISOString().split("T")[0];

  return (
    <div className="min-h-screen bg-background">
      {/* Header */}
      <RenterNavbar />

      {/* Main Content */}
      <main className="container mx-auto py-8 px-4">
        <div className="flex items-center justify-between mb-6">
          <div>
            <p className="text-sm text-muted-foreground mb-1">Booking</p>
            <h2 className="text-3xl font-bold">{tool.title}</h2>
            <p className="text-muted-foreground">
              {tool.location || tool.district}
            </p>
          </div>
          <BackToDashboardButton />
        </div>

        <div className="grid grid-cols-1 lg:grid-cols-3 gap-6">
          {/* Tool details */}
          <div className="lg:col-span-2 space-y-6">
            <Card>
              <CardHeader>
                <CardTitle>Overview</CardTitle>
                <CardDescription>What you get with this rental</CardDescription>
              </CardHeader>
              <CardContent className="space-y-4">
                {tool.imageUrl && (
                  <div className="w-full h-64 mb-4 rounded-md overflow-hidden bg-gray-100 dark:bg-gray-800">
                    <img
                      src={tool.imageUrl}
                      alt={tool.title}
                      className="w-full h-full object-cover"
                      onError={(e) => {
                        (e.target as HTMLImageElement).src =
                          "https://via.placeholder.com/600x400?text=No+Image+Available";
                      }}
                    />
                  </div>
                )}
                <div className="flex flex-wrap items-center gap-3">
                  <span className="px-3 py-1 rounded-full bg-primary/10 text-primary font-medium">
                    €{tool.pricePerDay.toFixed(2)}/day
                  </span>
                  <div className="flex items-center gap-2 rounded-full bg-secondary text-secondary-foreground px-3 py-1">
                    <StarRating rating={tool.overallRating} size={14} />
                    <span className="text-xs">
                      Tool • {tool.numRatings} review{tool.numRatings === 1 ? "" : "s"}
                    </span>
                  </div>
                  <span
                    className={`px-3 py-1 rounded-full ${tool.active
                      ? "bg-emerald-100 text-emerald-800"
                      : "bg-gray-100 text-gray-800"
                      }`}
                  >
                    {tool.active ? "Available" : "Unavailable"}
                  </span>
                </div>
                <Separator />
                <p className="text-muted-foreground leading-relaxed">
                  {tool.description}
                </p>
              </CardContent>
            </Card>
              
            {toolId && <ToolAvailabilityCard toolId={toolId} />}

            <Card>
              <CardHeader>
                <CardTitle>Reviews</CardTitle>
                <CardDescription>What renters said about this tool</CardDescription>
              </CardHeader>
              <CardContent className="space-y-4">
                {isLoadingReviews ? (
                  <p className="text-sm text-muted-foreground">Loading reviews...</p>
                ) : toolReviews.length === 0 ? (
                  <p className="text-sm text-muted-foreground">
                    No reviews yet. Complete a booking to be the first to review.
                  </p>
                ) : (
                  <div className="grid gap-3">
                    {toolReviews.map((review) => (
                      <div
                        key={review.id}
                        className="rounded-lg border border-border p-3 bg-muted/30"
                      >
                        <div className="flex items-start justify-between gap-2">
                          <div>
                            <p className="font-semibold">{review.reviewerName}</p>
                            <p className="text-xs text-muted-foreground">
                              {new Date(review.date).toLocaleDateString()}
                            </p>
                          </div>
                          <StarRating rating={review.rating} />
                        </div>
                        <p className="text-sm text-foreground mt-2">{review.comment}</p>
                      </div>
                    ))}
                  </div>
                )}

                <Separator />

                <div className="space-y-3">
                  <div className="flex items-center justify-between">
                    <p className="font-semibold">Your review</p>
                    {myExistingReview && (
                      <span className="text-xs text-muted-foreground">
                        You can update your previous review
                      </span>
                    )}
                  </div>
                  {!myCompletedBooking ? (
                    <p className="text-sm text-muted-foreground">
                      You can leave a review after completing and paying for a booking of this tool.
                    </p>
                  ) : (
                    <>
                      <Button
                        variant="outline"
                        onClick={() => setShowReviewForm((v) => !v)}
                      >
                        {showReviewForm
                          ? "Close"
                          : myExistingReview
                          ? "Edit your review"
                          : "Leave a review"}
                      </Button>
                      {showReviewForm && (
                        <div className="space-y-3 border border-border rounded-lg p-3 bg-muted/20">
                          <div className="flex items-center gap-2">
                            <span className="text-sm text-muted-foreground">Rating:</span>
                            <div className="flex gap-1">
                              {[1, 2, 3, 4, 5].map((value) => (
                                <button
                                  key={value}
                                  type="button"
                                  onClick={() => setReviewRating(value)}
                                  className="text-2xl leading-none"
                                  aria-label={`Rate ${value} star${value > 1 ? "s" : ""}`}
                                >
                                  <span
                                    className={
                                      reviewRating >= value
                                        ? "text-yellow-500"
                                        : "text-muted-foreground"
                                    }
                                  >
                                    ★
                                  </span>
                                </button>
                              ))}
                            </div>
                          </div>
                          <div className="space-y-2">
                            <label
                              className="text-sm text-muted-foreground"
                              htmlFor="reviewComment"
                            >
                              Comment
                            </label>
                            <Input
                              id="reviewComment"
                              value={reviewComment}
                              onChange={(e) => setReviewComment(e.target.value)}
                              placeholder="Share your experience with this tool"
                            />
                          </div>
                          {reviewError && (
                            <p className="text-sm text-destructive">{reviewError}</p>
                          )}
                          {reviewSuccess && (
                            <p className="text-sm text-green-600">{reviewSuccess}</p>
                          )}
                          <Button
                            onClick={handleReviewSubmit}
                            disabled={reviewSaving || reviewRating === 0}
                          >
                            {reviewSaving
                              ? "Saving..."
                              : myExistingReview
                              ? "Update Review"
                              : "Submit Review"}
                          </Button>
                        </div>
                      )}
                    </>
                  )}
                </div>
              </CardContent>
            </Card>

          </div>

          {/* Booking + owner card */}
          <div className="space-y-6">
            <Card>
              <CardHeader>
                <CardTitle>Request booking</CardTitle>
                <CardDescription>
                  Confirm dates and send a request
                </CardDescription>
              </CardHeader>
              <CardContent className="space-y-3">
                {success && (
                  <div className="bg-emerald-50 text-emerald-800 border border-emerald-200 px-3 py-2 rounded-md text-sm">
                    {success}
                  </div>
                )}
                {error && (
                  <div className="bg-destructive/10 text-destructive border border-destructive px-3 py-2 rounded-md text-sm">
                    {error}
                  </div>
                )}

                <form className="space-y-4" onSubmit={handleSubmit}>
                  <div className="space-y-2 text-sm text-muted-foreground">
                    <div className="flex items-center justify-between">
                      <span>Day rate</span>
                      <span className="text-foreground font-semibold">
                        €{tool.pricePerDay.toFixed(2)}
                      </span>
                    </div>
                    <div className="flex items-center justify-between">
                      <span>Days</span>
                      <span className="text-foreground font-semibold">
                        {rentalDays || "-"}
                      </span>
                    </div>
                    {isPro && discountPercentage > 0 && (
                      <>
                        <div className="flex items-center justify-between">
                          <span>Subtotal</span>
                          <span className="text-foreground line-through">
                            €{totalPrice.toFixed(2)}
                          </span>
                        </div>
                        <div className="flex items-center justify-between text-yellow-600">
                          <span className="flex items-center gap-1">
                            <Crown className="h-4 w-4" />
                            Pro discount ({discountPercentage}%)
                          </span>
                          <span className="font-semibold">
                            -€{(totalPrice - discountedTotalPrice).toFixed(2)}
                          </span>
                        </div>
                      </>
                    )}
                    <div className="flex items-center justify-between">
                      <span>Rental {isPro && discountPercentage > 0 ? "(after discount)" : "subtotal"}</span>
                      <span className={`text-foreground font-semibold ${isPro && discountPercentage > 0 ? "text-yellow-600" : ""}`}>
                        €{discountedTotalPrice.toFixed(2)}
                      </span>
                    </div>
                    <div className="flex items-center justify-between">
                      <span className="flex items-center gap-1">
                        Security deposit
                        <span className="text-xs text-muted-foreground">(refundable)</span>
                      </span>
                      <span className="text-foreground font-semibold">
                        €{SECURITY_DEPOSIT.toFixed(2)}
                      </span>
                    </div>
                    <Separator className="my-2" />
                    <div className="flex items-center justify-between text-foreground font-semibold text-lg">
                      <span>Total at checkout</span>
                      <span>
                        €{grandTotal.toFixed(2)}
                      </span>
                    </div>
                  </div>

                  <div className="grid grid-cols-1 gap-3">
                    <div className="space-y-1">
                      <label
                        className="text-sm font-medium text-foreground"
                        htmlFor="startDate"
                      >
                        Start date
                      </label>
                      <Input
                        id="startDate"
                        type="date"
                        value={startDate}
                        onChange={(e) => setStartDate(e.target.value)}
                        min={todayISO}
                        disabled={isSubmitting || !tool.active}
                      />
                    </div>
                    <div className="space-y-1">
                      <label
                        className="text-sm font-medium text-foreground"
                        htmlFor="endDate"
                      >
                        End date
                      </label>
                      <Input
                        id="endDate"
                        type="date"
                        value={endDate}
                        onChange={(e) => setEndDate(e.target.value)}
                        min={startDate || todayISO}
                        disabled={isSubmitting || !tool.active}
                      />
                    </div>
                    {datesInvalid && (startDate || endDate) && (
                      <p className="text-xs text-destructive">
                        End date must be after start date.
                      </p>
                    )}
                  </div>

                  {bookingResponse && (
                    <div className="text-xs text-muted-foreground">
                      Booking reference:{" "}
                      <span className="font-semibold text-foreground">
                        {bookingResponse.id}
                      </span>
                    </div>
                  )}

                  <CardFooter className="flex gap-2 px-0">
                    <Button
                      className="flex-1"
                      type="submit"
                      disabled={
                        isSubmitting ||
                        !tool.active ||
                        datesInvalid ||
                        rentalDays === 0
                      }
                    >
                      {tool.active
                        ? isSubmitting
                          ? "Sending..."
                          : "Request booking"
                        : "Unavailable"}
                    </Button>
                    <Button
                      variant="outline"
                      className="flex-1"
                      type="button"
                      onClick={() => navigate(-1)}
                    >
                      Cancel
                    </Button>
                  </CardFooter>
                </form>
              </CardContent>
            </Card>

            <Card>
              <CardHeader>
                <CardTitle>Owner</CardTitle>
                <CardDescription>
                  Reach out for dates or pickup details
                </CardDescription>
              </CardHeader>
              <CardContent className="space-y-3 text-sm">
                <div>
                  <p className="font-semibold">
                    {tool.owner.firstName} {tool.owner.lastName}
                  </p>
                  <p className="text-muted-foreground">{tool.owner.email}</p>
                </div>
                <div className="text-muted-foreground">
                  Supplier rating:{" "}
                  <span className="font-semibold text-foreground mr-2">
                    {(supplierRating || 0).toFixed(1)}
                  </span>
                  <StarRating rating={supplierRating || 0} size={14} />
                </div>
              </CardContent>
            </Card>
          </div>
        </div>
      </main>
    </div>
  );
};
