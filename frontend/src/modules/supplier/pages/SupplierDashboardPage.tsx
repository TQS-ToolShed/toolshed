import { Link } from 'react-router-dom';
import { useAuth } from '@/modules/auth/context/AuthContext';
import { Button } from '@/components/ui/button';
import { Card, CardContent, CardHeader, CardTitle, CardDescription } from '@/components/ui/card';

export const SupplierDashboardPage = () => {
  const { user, logout } = useAuth();

  return (
    <div className="min-h-screen bg-background">
      {/* Header */}
      <header className="border-b">
        <div className="container mx-auto px-4 py-4 flex justify-between items-center">
          <h1 className="text-2xl font-bold">ToolShed - Supplier</h1>
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
        <div className="mb-8">
          <h2 className="text-3xl font-bold mb-2">Supplier Dashboard</h2>
          <p className="text-muted-foreground">Manage your tool listings and rentals</p>
        </div>

        <div className="grid grid-cols-1 md:grid-cols-2 lg:grid-cols-3 gap-6">
          {/* Tools Management Card */}
          <Card>
            <CardHeader>
              <CardTitle>My Tools</CardTitle>
              <CardDescription>
                Manage your tool listings - add, edit, or remove tools
              </CardDescription>
            </CardHeader>
            <CardContent>
              <Link to="/supplier/tools">
                <Button className="w-full">Manage Tools</Button>
              </Link>
            </CardContent>
          </Card>

          {/* Stats Card - Placeholder */}
          <Card>
            <CardHeader>
              <CardTitle>Statistics</CardTitle>
              <CardDescription>
                View your rental statistics and earnings
              </CardDescription>
            </CardHeader>
            <CardContent>
              <p className="text-muted-foreground text-sm">Coming soon...</p>
            </CardContent>
          </Card>

          {/* Rentals Card - Placeholder */}
          <Card>
            <CardHeader>
              <CardTitle>Active Rentals</CardTitle>
              <CardDescription>
                View and manage your current tool rentals
              </CardDescription>
            </CardHeader>
            <CardContent>
              <p className="text-muted-foreground text-sm">Coming soon...</p>
            </CardContent>
          </Card>
        </div>
      </main>
    </div>
  );
};
