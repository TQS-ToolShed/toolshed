import { useState, useEffect } from 'react';
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
import { BackToDashboardButton } from '../components/BackToDashboardButton';
import {
  getSubscriptionStatus,
  createProSubscription,
  cancelSubscription,
  type SubscriptionStatus,
} from '@/api/subscription-api';
import { Crown, Sparkles, Check, X, Loader2 } from 'lucide-react';

export const SubscriptionPage = () => {
  const { user } = useAuth();
  const [subscription, setSubscription] = useState<SubscriptionStatus | null>(null);
  const [loading, setLoading] = useState(true);
  const [actionLoading, setActionLoading] = useState(false);

  useEffect(() => {
    if (user?.id) {
      loadSubscription();
    }
  }, [user?.id]);

  const loadSubscription = async () => {
    if (!user?.id) return;
    try {
      setLoading(true);
      const status = await getSubscriptionStatus(user.id);
      setSubscription(status);
    } catch (error) {
      console.error('Failed to load subscription:', error);
      // Default to FREE if not found
      setSubscription({
        tier: 'FREE',
        active: false,
        subscriptionStart: null,
        subscriptionEnd: null,
        discountPercentage: 0,
      });
    } finally {
      setLoading(false);
    }
  };

  const handleSubscribe = async () => {
    if (!user?.id) return;
    try {
      setActionLoading(true);
      const response = await createProSubscription(user.id);
      // Redirect to Stripe checkout
      window.location.href = response.checkoutUrl;
    } catch (error) {
      console.error('Failed to create subscription:', error);
      alert('Failed to start subscription. Please try again.');
    } finally {
      setActionLoading(false);
    }
  };

  const handleCancel = async () => {
    if (!user?.id) return;
    if (!confirm('Are you sure you want to cancel your Pro subscription?')) return;
    
    try {
      setActionLoading(true);
      await cancelSubscription(user.id);
      await loadSubscription();
    } catch (error) {
      console.error('Failed to cancel subscription:', error);
      alert('Failed to cancel subscription. Please try again.');
    } finally {
      setActionLoading(false);
    }
  };

  const isPro = subscription?.tier === 'PRO' && subscription?.active;

  return (
    <div className="min-h-screen bg-background">
      <RenterNavbar />

      <main className="container mx-auto py-8 px-4 space-y-6">
        <div className="flex items-center justify-between">
          <div>
            <h1 className="text-3xl font-bold">Subscription</h1>
            <p className="text-sm text-muted-foreground">
              Manage your Pro Member subscription
            </p>
          </div>
          <BackToDashboardButton />
        </div>

        {loading ? (
          <div className="flex items-center justify-center py-12">
            <Loader2 className="h-8 w-8 animate-spin text-primary" />
          </div>
        ) : (
          <div className="grid grid-cols-1 lg:grid-cols-2 gap-6">
            {/* Current Status Card */}
            <Card className={isPro ? 'border-primary/50 bg-primary/5' : ''}>
              <CardHeader>
                <div className="flex items-center gap-2">
                  <CardTitle>Current Plan</CardTitle>
                  {isPro && (
                    <span className="inline-flex items-center px-2 py-1 rounded-full text-xs font-medium bg-primary text-primary-foreground">
                      <Crown className="h-3 w-3 mr-1" />
                      Active
                    </span>
                  )}
                </div>
                <CardDescription>
                  {isPro
                    ? "You're enjoying Pro Member benefits!"
                    : 'Upgrade to Pro for exclusive discounts'}
                </CardDescription>
              </CardHeader>
              <CardContent className="space-y-4">
                <div className="flex items-center gap-4">
                  <div
                    className={`p-4 rounded-full ${
                      isPro ? 'bg-primary text-primary-foreground' : 'bg-muted'
                    }`}
                  >
                    <Crown className="h-8 w-8" />
                  </div>
                  <div>
                    <p className="text-2xl font-bold">
                      {isPro ? 'Pro Member' : 'Free Plan'}
                    </p>
                    {isPro && subscription?.discountPercentage && (
                      <p className="text-sm text-primary font-medium">
                        {subscription.discountPercentage}% off all rentals
                      </p>
                    )}
                  </div>
                </div>

                {isPro && subscription?.subscriptionEnd && (
                  <p className="text-sm text-muted-foreground">
                    Next billing date:{' '}
                    {new Date(subscription.subscriptionEnd).toLocaleDateString()}
                  </p>
                )}

                {isPro ? (
                  <Button
                    variant="outline"
                    onClick={handleCancel}
                    disabled={actionLoading}
                    className="w-full"
                  >
                    {actionLoading ? (
                      <Loader2 className="h-4 w-4 animate-spin mr-2" />
                    ) : (
                      <X className="h-4 w-4 mr-2" />
                    )}
                    Cancel Subscription
                  </Button>
                ) : (
                  <Button
                    onClick={handleSubscribe}
                    disabled={actionLoading}
                    className="w-full"
                    size="lg"
                  >
                    {actionLoading ? (
                      <Loader2 className="h-4 w-4 animate-spin mr-2" />
                    ) : (
                      <Sparkles className="h-4 w-4 mr-2" />
                    )}
                    Upgrade to Pro - €25/month
                  </Button>
                )}
              </CardContent>
            </Card>

            {/* Benefits Card */}
            <Card>
              <CardHeader>
                <CardTitle className="flex items-center gap-2">
                  <Sparkles className="h-5 w-5 text-primary" />
                  Pro Member Benefits
                </CardTitle>
                <CardDescription>
                  What you get with Pro subscription
                </CardDescription>
              </CardHeader>
              <CardContent>
                <ul className="space-y-3">
                  <li className="flex items-start gap-3">
                    <Check className="h-5 w-5 text-primary mt-0.5" />
                    <div>
                      <p className="font-medium">5% Discount on All Rentals</p>
                      <p className="text-sm text-muted-foreground">
                        Save money on every tool you rent
                      </p>
                    </div>
                  </li>
                  <li className="flex items-start gap-3">
                    <Check className="h-5 w-5 text-primary mt-0.5" />
                    <div>
                      <p className="font-medium">Pro Member Badge</p>
                      <p className="text-sm text-muted-foreground">
                        Stand out as a trusted renter
                      </p>
                    </div>
                  </li>
                  <li className="flex items-start gap-3">
                    <Check className="h-5 w-5 text-primary mt-0.5" />
                    <div>
                      <p className="font-medium">Priority Support</p>
                      <p className="text-sm text-muted-foreground">
                        Get faster responses from our team
                      </p>
                    </div>
                  </li>
                </ul>

                <div className="mt-6 p-4 bg-muted rounded-lg">
                  <p className="text-center">
                    <span className="text-3xl font-bold">€25</span>
                    <span className="text-muted-foreground">/month</span>
                  </p>
                  <p className="text-center text-sm text-muted-foreground mt-1">
                    Cancel anytime
                  </p>
                </div>
              </CardContent>
            </Card>
          </div>
        )}
      </main>
    </div>
  );
};
