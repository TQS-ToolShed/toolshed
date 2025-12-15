import { useEffect, useMemo, useState } from "react";
import { useAuth } from "@/modules/auth/context/AuthContext";
import { SupplierNavbar } from "../components/SupplierNavbar";
import { BackToDashboardButton } from "@/modules/renter/components/BackToDashboardButton";
import { Card, CardContent, CardDescription, CardHeader, CardTitle } from "@/components/ui/card";
import StarRating from "@/modules/shared/components/StarRating";
import { getToolsBySupplier, type Tool } from "../api/tools-api";
import { getBookingsForTool, type BookingResponse } from "@/modules/renter/api/bookings-api";

export const SupplierProfilePage = () => {
  const { user } = useAuth();
  const [tools, setTools] = useState<Tool[]>([]);
  const [toolBookings, setToolBookings] = useState<Record<string, BookingResponse[]>>({});
  const [isLoading, setIsLoading] = useState(false);
  const [error, setError] = useState<string | null>(null);

  useEffect(() => {
    const load = async () => {
      if (!user?.id) return;
      try {
        setIsLoading(true);
        setError(null);
        const myTools = await getToolsBySupplier(user.id);
        setTools(myTools);

        // fetch bookings per tool to extract renter reviews (RENTER_TO_OWNER is stored as review on booking)
        const bookingEntries: Record<string, BookingResponse[]> = {};
        for (const t of myTools) {
          try {
            bookingEntries[t.id] = await getBookingsForTool(t.id);
          } catch (err) {
            // keep going, but record error once
            if (!error) setError("Some reviews could not be loaded");
          }
        }
        setToolBookings(bookingEntries);
      } catch (err) {
        setError(err instanceof Error ? err.message : "Failed to load profile data");
      } finally {
        setIsLoading(false);
      }
    };
    load();
  }, [user?.id]);

  const renterReviews = useMemo(() => {
    return Object.values(toolBookings)
      .flat()
      .map((b) => b.review) // renter -> owner review on the booking
      .filter((r): r is NonNullable<BookingResponse["review"]> => Boolean(r));
  }, [toolBookings]);

  const averageRating = useMemo(() => {
    if (renterReviews.length === 0) return 0;
    const sum = renterReviews.reduce((acc, r) => acc + r.rating, 0);
    return Math.round((sum / renterReviews.length) * 10) / 10;
  }, [renterReviews]);

  return (
    <div className="min-h-screen bg-background">
      <SupplierNavbar />

      <main className="container mx-auto py-8 px-4 space-y-6">
        <div className="flex items-center justify-between">
          <div>
            <h1 className="text-3xl font-bold">Supplier Profile</h1>
            <p className="text-sm text-muted-foreground">Your account and feedback</p>
          </div>
          <BackToDashboardButton />
        </div>

        <div className="grid grid-cols-1 lg:grid-cols-3 gap-6">
          <Card className="lg:col-span-1">
            <CardHeader>
              <CardTitle>Supplier details</CardTitle>
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
                    {averageRating.toFixed(1)}
                  </span>
                  <StarRating rating={averageRating} size={16} />
                  <span className="text-xs text-muted-foreground">
                    ({renterReviews.length} review{renterReviews.length === 1 ? "" : "s"})
                  </span>
                </div>
              </div>
              <div>
                <p className="text-muted-foreground">Tools listed</p>
                <p className="font-semibold text-foreground">{tools.length}</p>
              </div>
            </CardContent>
          </Card>

          <Card className="lg:col-span-2">
            <CardHeader>
              <CardTitle>Reviews from renters</CardTitle>
              <CardDescription>
                Feedback renters left after using your tools.
              </CardDescription>
            </CardHeader>
            <CardContent className="space-y-3">
              {error && <div className="text-sm text-destructive">{error}</div>}
              {isLoading ? (
                <p className="text-sm text-muted-foreground">Loading reviews...</p>
              ) : renterReviews.length === 0 ? (
                <p className="text-sm text-muted-foreground">
                  No reviews yet. Once renters complete bookings and leave feedback, it will appear here.
                </p>
              ) : (
                <>
                  <div className="flex items-center gap-2 text-sm">
                    <span className="font-semibold text-foreground">Average rating:</span>
                    <StarRating rating={averageRating} />
                    <span className="text-muted-foreground">
                      {averageRating.toFixed(1)} ({renterReviews.length} review{renterReviews.length > 1 ? "s" : ""})
                    </span>
                  </div>
                  <div className="grid gap-3">
                    {renterReviews.map((review) => (
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
                </>
              )}
            </CardContent>
          </Card>
        </div>
      </main>
    </div>
  );
};
