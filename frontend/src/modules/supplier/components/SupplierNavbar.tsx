import { Link, useLocation } from 'react-router-dom';
import { useAuth } from '@/modules/auth/context/AuthContext';
import { Button } from '@/components/ui/button';

const navLinks = [
  { to: '/supplier', label: 'Dashboard' },
  { to: '/supplier/tools', label: 'My Tools' },
  { to: '/supplier/rentals', label: 'Rentals' },
];

export const SupplierNavbar = () => {
  const { user, logout } = useAuth();
  const location = useLocation();

  return (
    <header className="border-b bg-background/70 backdrop-blur">
      <div className="container mx-auto px-4 py-4 flex flex-wrap items-center justify-between gap-3">
        <div className="flex items-center gap-6">
          <Link to="/supplier" className="text-2xl font-bold hover:underline">
            ToolShed - Supplier
          </Link>
        </div>
        <div className="flex items-center gap-4">
          <span className="text-sm text-muted-foreground">
            {user ? `Welcome, ${user.firstName} ${user.lastName}` : 'Welcome'}
          </span>
          <Button variant="outline" size="sm" onClick={logout}>
            Logout
          </Button>
        </div>
      </div>
    </header>
  );
};
