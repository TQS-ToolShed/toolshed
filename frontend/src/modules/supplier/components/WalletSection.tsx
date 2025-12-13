import { useState, useEffect } from 'react';
import { Card, CardContent, CardHeader, CardTitle, CardDescription } from '@/components/ui/card';
import { Button } from '@/components/ui/button';
import { Input } from '@/components/ui/input';
import { useAuth } from '@/modules/auth/context/AuthContext';
import { getOwnerWallet, requestPayout } from '../api/payoutApi';
import type { WalletResponse, PayoutResponse } from '../api/payoutApi';
import { Wallet, ArrowDownCircle, CheckCircle, Clock, AlertCircle } from 'lucide-react';

export const WalletSection = () => {
  const { user } = useAuth();
  const [walletData, setWalletData] = useState<WalletResponse | null>(null);
  const [loading, setLoading] = useState(true);
  const [payoutAmount, setPayoutAmount] = useState<string>('');
  const [isRequestingPayout, setIsRequestingPayout] = useState(false);
  const [errorMessage, setErrorMessage] = useState<string | null>(null);
  const [successMessage, setSuccessMessage] = useState<string | null>(null);

  const fetchWallet = async () => {
    if (!user?.id) return;
    try {
      setLoading(true);
      const data = await getOwnerWallet(user.id);
      setWalletData(data);
      setErrorMessage(null);
    } catch (error) {
      console.error('Error fetching wallet:', error);
      setErrorMessage('Failed to load wallet data');
    } finally {
      setLoading(false);
    }
  };

  useEffect(() => {
    fetchWallet();
  }, [user?.id]);

  const handleRequestPayout = async () => {
    if (!user?.id || !payoutAmount) return;
    
    const amount = parseFloat(payoutAmount);
    if (isNaN(amount) || amount <= 0) {
      setErrorMessage('Please enter a valid amount');
      return;
    }

    if (walletData && amount > walletData.balance) {
      setErrorMessage('Insufficient balance');
      return;
    }

    try {
      setIsRequestingPayout(true);
      setErrorMessage(null);
      await requestPayout(user.id, amount);
      setSuccessMessage(`Payout of €${amount.toFixed(2)} processed successfully!`);
      setPayoutAmount('');
      fetchWallet(); // Refresh wallet data
      setTimeout(() => setSuccessMessage(null), 3000);
    } catch (error: unknown) {
      const message = error instanceof Error ? error.message : 'Failed to process payout';
      setErrorMessage(message);
    } finally {
      setIsRequestingPayout(false);
    }
  };

  const getStatusIcon = (status: PayoutResponse['status']) => {
    switch (status) {
      case 'COMPLETED':
        return <CheckCircle className="h-4 w-4 text-green-500" />;
      case 'PENDING':
        return <Clock className="h-4 w-4 text-yellow-500" />;
      case 'FAILED':
        return <AlertCircle className="h-4 w-4 text-red-500" />;
    }
  };

  const formatDate = (dateString: string) => {
    return new Date(dateString).toLocaleDateString('pt-PT', {
      day: '2-digit',
      month: 'short',
      year: 'numeric',
      hour: '2-digit',
      minute: '2-digit'
    });
  };

  if (loading) {
    return (
      <Card>
        <CardHeader>
          <CardTitle className="flex items-center gap-2">
            <Wallet className="h-5 w-5" />
            My Wallet
          </CardTitle>
        </CardHeader>
        <CardContent>
          <div className="animate-pulse space-y-4">
            <div className="h-12 bg-muted rounded"></div>
            <div className="h-8 bg-muted rounded w-1/2"></div>
          </div>
        </CardContent>
      </Card>
    );
  }

  return (
    <Card>
      <CardHeader>
        <CardTitle className="flex items-center gap-2">
          <Wallet className="h-5 w-5" />
          My Wallet
        </CardTitle>
        <CardDescription>
          View your balance and request payouts
        </CardDescription>
      </CardHeader>
      <CardContent className="space-y-6">
        {/* Balance Display */}
        <div className="bg-gradient-to-r from-primary/10 to-primary/5 rounded-lg p-4">
          <p className="text-sm text-muted-foreground mb-1">Available Balance</p>
          <p className="text-3xl font-bold text-primary">
            €{walletData?.balance?.toFixed(2) ?? '0.00'}
          </p>
        </div>

        {/* Status Messages */}
        {errorMessage && (
          <div className="text-sm text-red-600 bg-red-50 p-2 rounded">
            {errorMessage}
          </div>
        )}
        {successMessage && (
          <div className="text-sm text-green-600 bg-green-50 p-2 rounded">
            {successMessage}
          </div>
        )}

        {/* Payout Request */}
        <div className="space-y-3">
          <label className="text-sm font-medium">Request Payout</label>
          <div className="flex gap-2">
            <Input
              type="number"
              placeholder="Amount (€)"
              value={payoutAmount}
              onChange={(e) => setPayoutAmount(e.target.value)}
              min="0"
              step="0.01"
              className="flex-1"
            />
            <Button 
              onClick={handleRequestPayout}
              disabled={isRequestingPayout || !payoutAmount}
            >
              <ArrowDownCircle className="h-4 w-4 mr-2" />
              {isRequestingPayout ? 'Processing...' : 'Withdraw'}
            </Button>
          </div>
        </div>

        {/* Recent Payouts */}
        {walletData?.recentPayouts && walletData.recentPayouts.length > 0 && (
          <div className="space-y-3">
            <h4 className="text-sm font-medium">Recent Payouts</h4>
            <div className="space-y-2">
              {walletData.recentPayouts.slice(0, 5).map((payout) => (
                <div 
                  key={payout.id}
                  className="flex items-center justify-between p-3 bg-muted/50 rounded-lg"
                >
                  <div className="flex items-center gap-3">
                    {getStatusIcon(payout.status)}
                    <div>
                      <p className="font-medium">€{payout.amount.toFixed(2)}</p>
                      <p className="text-xs text-muted-foreground">
                        {formatDate(payout.requestedAt)}
                      </p>
                    </div>
                  </div>
                  <span className={`text-xs px-2 py-1 rounded-full ${
                    payout.status === 'COMPLETED' ? 'bg-green-100 text-green-700' :
                    payout.status === 'PENDING' ? 'bg-yellow-100 text-yellow-700' :
                    'bg-red-100 text-red-700'
                  }`}>
                    {payout.status}
                  </span>
                </div>
              ))}
            </div>
          </div>
        )}
      </CardContent>
    </Card>
  );
};
