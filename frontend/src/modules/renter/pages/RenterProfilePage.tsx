import { useAuth } from "@/modules/auth/context/AuthContext";
import {
  Card,
  CardContent,
  CardDescription,
  CardHeader,
  CardTitle,
} from "@/components/ui/card";
import StarRating from "@/modules/shared/components/StarRating";
import { RenterNavbar } from "../components/RenterNavbar";
import { BackToDashboardButton } from "../components/BackToDashboardButton";
import { useEffect, useMemo, useState } from "react";
import { getBookingsForRenter, type BookingResponse } from "../api/bookings-api";

export const RenterProfilePage = () => {
  const { user } = useAuth();
  const [bookings, setBookings] = useState<BookingResponse[]>([]);
  const [isLoadingReviews, setIsLoadingReviews] = useState(false);
  const [reviewsError, setReviewsError] = useState<string | null>(null);

  useEffect(() => {
    const load = async () => {
      if (!user?.id) return;
      try {
        setIsLoadingReviews(true);
        setReviewsError(null);
        const data = await getBookingsForRenter(user.id);
        setBookings(data);
      } catch (err) {
        setReviewsError(
          err instanceof Error ? err.message : "Failed to load your reviews"
        );
      } finally {
        setIsLoadingReviews(false);
      }
    };
    load();
  }, [user?.id]);

  const ownerReviews = useMemo(
    () =>
      bookings
        .map((b) => b.ownerReview)
        .filter((r): r is NonNullable<BookingResponse["ownerReview"]> => Boolean(r)),
    [bookings]
  );

  return (
    <div className="min-h-screen bg-background">
      <RenterNavbar />

      <main className="container mx-auto py-8 px-4 space-y-6">
        <div className="flex items-center justify-between">
          <div>
            <h1 className="text-3xl font-bold">Profile</h1>
            <p className="text-sm text-muted-foreground">Your account</p>
          </div>
          <BackToDashboardButton />
        </div>

        <div className="grid grid-cols-1 lg:grid-cols-3 gap-6">
          <Card className="lg:col-span-1">
            <CardHeader>
              <CardTitle>Renter details</CardTitle>
              <CardDescription>Basic info for your account</CardDescription>
            </CardHeader>
            <CardContent className="space-y-3 text-sm">
              <div>
                <p className="text-muted-foreground">Name</p>
                <p className="font-semibold text-foreground">
                  {user?.firstName} {user?.lastName}
                </p>
              </div>
              <div>
                <p className="text-muted-foreground">Email</p>
                <p className="font-semibold text-foreground">{user?.email}</p>
              </div>
              <div>
                <p className="text-muted-foreground">Role</p>
                <p className="font-semibold text-foreground">{user?.role}</p>
              </div>
              <div>
                <p className="text-muted-foreground">Reputation Score</p>
                <div className="flex items-center gap-2">
                  <span className="font-semibold text-foreground">
                    {user?.reputationScore
                      ? user.reputationScore.toFixed(1)
                      : "0.0"}
                  </span>
                  <StarRating rating={user?.reputationScore || 0} size={16} />
                </div>
              </div>
            </CardContent>
          </Card>

          <Card className="lg:col-span-2">
            <CardHeader>
              <CardTitle>Your reviews</CardTitle>
              <CardDescription>
                Feedback left by owners after your rentals.
              </CardDescription>
            </CardHeader>
            <CardContent className="space-y-3">
              {reviewsError && (
                <div className="text-sm text-destructive">{reviewsError}</div>
              )}
              {isLoadingReviews ? (
                <p className="text-sm text-muted-foreground">Loading reviews...</p>
              ) : ownerReviews.length === 0 ? (
                <p className="text-sm text-muted-foreground">
                  You have no reviews yet. Complete bookings to see feedback here.
                </p>
              ) : (
                <div className="grid gap-3">
                  {ownerReviews.map((review) => (
                    <div
                      key={review.id}
                      className="border border-border rounded-lg p-3 bg-muted/30"
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
                      <p className="text-sm text-foreground mt-2">
                        {review.comment}
                      </p>
                    </div>
                  ))}
                </div>
              )}
            </CardContent>
          </Card>
        </div>
      </main>
    </div>
  );
};
