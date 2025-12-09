import { useAuth } from '@/modules/auth/context/AuthContext';
import { Card, CardContent, CardDescription, CardHeader, CardTitle } from '@/components/ui/card';
import { RenterNavbar } from '../components/RenterNavbar';
import { BackToDashboardButton } from '../components/BackToDashboardButton';

export const RenterProfilePage = () => {
  const { user } = useAuth();

  return (
    <div className="min-h-screen bg-background">
      <RenterNavbar />

      <main className="container mx-auto py-8 px-4 space-y-6">
        <div className="flex items-center justify-between">
          <div>
            <h1 className="text-3xl font-bold">Profile</h1>
            <p className="text-sm text-muted-foreground">Your account</p>
          </div>
          <BackToDashboardButton />
        </div>

        <div className="grid grid-cols-1 lg:grid-cols-3 gap-6">
          <Card className="lg:col-span-1">
            <CardHeader>
              <CardTitle>Renter details</CardTitle>
              <CardDescription>Basic info for your account</CardDescription>
            </CardHeader>
            <CardContent className="space-y-3 text-sm">
              <div>
                <p className="text-muted-foreground">Name</p>
                <p className="font-semibold text-foreground">
                  {user?.firstName} {user?.lastName}
                </p>
              </div>
              <div>
                <p className="text-muted-foreground">Email</p>
                <p className="font-semibold text-foreground">{user?.email}</p>
              </div>
              <div>
                <p className="text-muted-foreground">Role</p>
                <p className="font-semibold text-foreground">{user?.role}</p>
              </div>
            </CardContent>
          </Card>
        </div>
      </main>
    </div>
  );
};
