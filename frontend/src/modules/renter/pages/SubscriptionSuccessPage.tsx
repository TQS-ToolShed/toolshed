import { useEffect, useState } from 'react';
import { useNavigate, useSearchParams } from 'react-router-dom';
import { useAuth } from '@/modules/auth/context/AuthContext';
import {
  Card,
  CardContent,
  CardDescription,
  CardHeader,
  CardTitle,
} from '@/components/ui/card';
import { Button } from '@/components/ui/button';
import { RenterNavbar } from '../components/RenterNavbar';
import { activateSubscription } from '@/api/subscription-api';
import { CheckCircle, Loader2, Crown } from 'lucide-react';

export const SubscriptionSuccessPage = () => {
  const { user } = useAuth();
  const navigate = useNavigate();
  const [searchParams] = useSearchParams();
  const [loading, setLoading] = useState(true);
  const [success, setSuccess] = useState(false);

  useEffect(() => {
    const activateSub = async () => {
      const sessionId = searchParams.get('session_id');
      if (!user?.id || !sessionId) {
        setLoading(false);
        return;
      }

      try {
        // Activate the subscription with a placeholder ID
        // In production, you'd get the actual subscription ID from the session
        await activateSubscription(user.id, sessionId);
        setSuccess(true);
      } catch (error) {
        console.error('Failed to activate subscription:', error);
      } finally {
        setLoading(false);
      }
    };

    activateSub();
  }, [user?.id, searchParams]);

  return (
    <div className="min-h-screen bg-background">
      <RenterNavbar />

      <main className="container mx-auto py-16 px-4">
        <div className="max-w-md mx-auto">
          <Card className="text-center">
            <CardHeader>
              {loading ? (
                <>
                  <div className="flex justify-center mb-4">
                    <Loader2 className="h-16 w-16 animate-spin text-primary" />
                  </div>
                  <CardTitle>Activating Subscription...</CardTitle>
                  <CardDescription>
                    Please wait while we set up your Pro membership
                  </CardDescription>
                </>
              ) : success ? (
                <>
                  <div className="flex justify-center mb-4">
                    <div className="p-4 bg-primary/10 rounded-full">
                      <Crown className="h-12 w-12 text-primary" />
                    </div>
                  </div>
                  <div className="flex justify-center mb-2">
                    <CheckCircle className="h-6 w-6 text-green-500" />
                  </div>
                  <CardTitle className="text-green-600">
                    Welcome to Pro!
                  </CardTitle>
                  <CardDescription>
                    Your Pro Member subscription is now active
                  </CardDescription>
                </>
              ) : (
                <>
                  <CardTitle className="text-yellow-600">
                    Subscription Pending
                  </CardTitle>
                  <CardDescription>
                    Your subscription is being processed
                  </CardDescription>
                </>
              )}
            </CardHeader>
            <CardContent className="space-y-4">
              {success && (
                <>
                  <p className="text-muted-foreground">
                    You now enjoy <strong>5% off</strong> on all tool rentals!
                  </p>
                  <div className="p-4 bg-primary/5 rounded-lg">
                    <p className="font-medium text-primary">Pro Member Benefits:</p>
                    <ul className="text-sm text-muted-foreground mt-2 space-y-1">
                      <li>✓ 5% discount on all rentals</li>
                      <li>✓ Pro Member badge</li>
                      <li>✓ Priority support</li>
                    </ul>
                  </div>
                </>
              )}
              <Button
                onClick={() => navigate('/renter')}
                className="w-full"
              >
                Go to Dashboard
              </Button>
            </CardContent>
          </Card>
        </div>
      </main>
    </div>
  );
};
