import { useCallback, useEffect, useMemo, useState } from 'react';
import { useNavigate, useParams } from 'react-router-dom';
import { useAuth } from '@/modules/auth/context/AuthContext';
import { Button } from '@/components/ui/button';
import { Card, CardContent, CardDescription, CardFooter, CardHeader, CardTitle } from '@/components/ui/card';
import { Separator } from '@/components/ui/separator';
import { getToolDetails, type ToolDetails } from '@/modules/supplier/api/tools-api';

export const RenterBookingsPage = () => {
  const { toolId } = useParams<{ toolId: string }>();
  const { user, logout } = useAuth();
  const navigate = useNavigate();

  const [tool, setTool] = useState<ToolDetails | null>(null);
  const [isLoading, setIsLoading] = useState(true);
  const [error, setError] = useState<string | null>(null);

  const fetchTool = useCallback(async () => {
    if (!toolId) {
      setError('Missing tool id');
      setIsLoading(false);
      return;
    }

    try {
      setError(null);
      const data = await getToolDetails(toolId);
      setTool(data);
    } catch (err) {
      setError(err instanceof Error ? err.message : 'Failed to load tool');
    } finally {
      setIsLoading(false);
    }
  }, [toolId]);

  useEffect(() => {
    fetchTool();
  }, [fetchTool]);

  const ratingSummary = useMemo(() => {
    if (!tool || !tool.numRatings) return 'No ratings yet';
    return `${tool.overallRating.toFixed(1)} (${tool.numRatings} reviews)`;
  }, [tool]);

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
          {error || 'Tool not found'}
        </div>
        <Button variant="outline" onClick={() => navigate(-1)}>
          Go back
        </Button>
      </div>
    );
  }

  return (
    <div className="min-h-screen bg-background">
      {/* Header */}
      <header className="border-b">
        <div className="container mx-auto px-4 py-4 flex justify-between items-center">
          <h1 className="text-2xl font-bold">ToolShed - Renter</h1>
          <div className="flex items-center gap-4">
            <span className="text-muted-foreground">
              Welcome, {user?.firstName} {user?.lastName}
            </span>
            <Button variant="outline" onClick={logout}>
              Logout
            </Button>
          </div>
        </div>
      </header>

      {/* Main Content */}
      <main className="container mx-auto py-8 px-4">
        <div className="flex items-center justify-between mb-6">
          <div>
            <p className="text-sm text-muted-foreground mb-1">Booking</p>
            <h2 className="text-3xl font-bold">{tool.title}</h2>
            <p className="text-muted-foreground">{tool.location}</p>
          </div>
          <Button variant="outline" onClick={() => navigate('/renter')}>
            Back to tools
          </Button>
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
                <div className="flex flex-wrap items-center gap-3">
                  <span className="px-3 py-1 rounded-full bg-primary/10 text-primary font-medium">
                    €{tool.pricePerDay.toFixed(2)}/day
                  </span>
                  <span className="px-3 py-1 rounded-full bg-secondary text-secondary-foreground">
                    {ratingSummary}
                  </span>
                  <span className={`px-3 py-1 rounded-full ${tool.active ? 'bg-emerald-100 text-emerald-800' : 'bg-gray-100 text-gray-800'}`}>
                    {tool.active ? 'Available' : 'Unavailable'}
                  </span>
                </div>
                <Separator />
                <p className="text-muted-foreground leading-relaxed">{tool.description}</p>
              </CardContent>
            </Card>

            <Card>
              <CardHeader>
                <CardTitle>Availability</CardTitle>
                <CardDescription>Plan your rental window</CardDescription>
              </CardHeader>
              <CardContent>
                {tool.availabilityCalendar ? (
                  <div className="text-sm text-muted-foreground whitespace-pre-wrap">
                    {tool.availabilityCalendar}
                  </div>
                ) : (
                  <p className="text-muted-foreground">No detailed availability shared. Contact the owner for dates.</p>
                )}
              </CardContent>
            </Card>
          </div>

          {/* Booking + owner card */}
          <div className="space-y-6">
            <Card>
              <CardHeader>
                <CardTitle>Request booking</CardTitle>
                <CardDescription>Confirm dates and send a request</CardDescription>
              </CardHeader>
              <CardContent className="space-y-3 text-sm text-muted-foreground">
                <p>Booking flow coming soon. Share the tool with a friend or contact the owner below.</p>
                <p>
                  Day rate: <span className="font-semibold text-foreground">€{tool.pricePerDay.toFixed(2)}</span>
                </p>
              </CardContent>
              <CardFooter className="flex gap-2">
                <Button className="flex-1" disabled>
                  Request booking
                </Button>
                <Button variant="outline" className="flex-1" onClick={() => navigate(-1)}>
                  Cancel
                </Button>
              </CardFooter>
            </Card>

            <Card>
              <CardHeader>
                <CardTitle>Owner</CardTitle>
                <CardDescription>Reach out for dates or pickup details</CardDescription>
              </CardHeader>
              <CardContent className="space-y-3 text-sm">
                <div>
                  <p className="font-semibold">
                    {tool.owner.firstName} {tool.owner.lastName}
                  </p>
                  <p className="text-muted-foreground">{tool.owner.email}</p>
                </div>
                <div className="text-muted-foreground">
                  Reputation score: <span className="font-semibold text-foreground">{tool.owner.reputationScore.toFixed(1)}</span>
                </div>
              </CardContent>
            </Card>
          </div>
        </div>
      </main>
    </div>
  );
};
