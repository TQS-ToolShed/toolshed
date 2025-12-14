import { Link } from 'react-router-dom';
import { Button } from '@/components/ui/button';
import { Card, CardContent, CardHeader, CardTitle, CardDescription } from '@/components/ui/card';
import { ToolBookingRequests } from '../components/ToolBookingRequests';
import { ActiveRentals } from '../components/ActiveRentals';
import { SupplierNavbar } from '../components/SupplierNavbar';
import { WalletSection } from '../components/WalletSection';
import { OwnerEarnings } from '../components/OwnerEarnings';

export const SupplierDashboardPage = () => {

  return (
    <div className="min-h-screen bg-background">

      <SupplierNavbar />
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

          {/* Wallet Section */}
          <WalletSection />

          {/* Rentals Card */}
          <ActiveRentals />
        </div>

        {/* Earnings Section */}
        <div className="mt-10">
          <OwnerEarnings />
        </div>

        <div className="mt-10">
          <ToolBookingRequests />
        </div>
      </main>
    </div>
  );
};
