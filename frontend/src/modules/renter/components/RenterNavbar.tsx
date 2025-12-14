import { Link } from 'react-router-dom';
import { useAuth } from '@/modules/auth/context/AuthContext';
import { Button } from '@/components/ui/button';
import { Crown } from 'lucide-react';
import { useEffect, useState } from 'react';
import { getSubscriptionStatus } from '@/api/subscription-api';

export const RenterNavbar = () => {
  const { logout, user } = useAuth();
  const [isPro, setIsPro] = useState(false);

  useEffect(() => {
    const checkProStatus = async () => {
      if (user?.id) {
        try {
          const status = await getSubscriptionStatus(user.id);
          setIsPro(status.active);
        } catch (error) {
          console.error('Failed to check Pro status:', error);
        }
      }
    };
    checkProStatus();
  }, [user?.id]);

  return (
    <header className="border-b">
      <div className="container mx-auto px-4 py-4 flex justify-between items-center">
        <Link to="/renter" className="text-2xl font-bold">
          ToolShed
        </Link>
        <div className="flex items-center gap-3">
          <Button 
            variant={isPro ? "default" : "outline"} 
            asChild
            className={isPro ? "bg-yellow-500 hover:bg-yellow-600 text-black" : ""}
          >
            <Link to="/renter/subscription" className="flex items-center gap-1">
              <Crown className={`h-4 w-4 ${isPro ? "fill-current" : ""}`} />
              {isPro ? "Pro Member" : "Pro"}
            </Link>
          </Button>
          <Button variant="outline" asChild>
            <Link to="/renter/my-bookings">My bookings</Link>
          </Button>
          <Button variant="outline" asChild>
            <Link to="/renter/profile">Profile</Link>
          </Button>
          <Button variant="outline" onClick={logout}>
            Logout
          </Button>
        </div>
      </div>
    </header>
  );
};

