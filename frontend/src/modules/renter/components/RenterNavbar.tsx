import { Link } from 'react-router-dom';
import { useAuth } from '@/modules/auth/context/AuthContext';
import { Button } from '@/components/ui/button';

export const RenterNavbar = () => {
  const { user, logout } = useAuth();

  return (
    <header className="border-b">
      <div className="container mx-auto px-4 py-4 flex justify-between items-center">
        <h1 className="text-2xl font-bold">ToolShed - Renter</h1>
        <div className="flex items-center gap-3">
          <Button variant="ghost" asChild>
            <Link to="/renter/profile">Profile</Link>
          </Button>
          <span className="text-muted-foreground">
            Welcome, {user?.firstName} {user?.lastName}
          </span>
          <Button variant="outline" onClick={logout}>
            Logout
          </Button>
        </div>
      </div>
    </header>
  );
};
