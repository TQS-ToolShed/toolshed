import { Link } from 'react-router-dom';
import { useAuth } from '@/modules/auth/context/AuthContext';
import { Button } from '@/components/ui/button';

export const RenterNavbar = () => {
  const { logout } = useAuth();

  return (
    <header className="border-b">
      <div className="container mx-auto px-4 py-4 flex justify-between items-center">
        <Link to="/renter" className="text-2xl font-bold">
          ToolShed
        </Link>
        <div className="flex items-center gap-3">
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
