import { Link } from "react-router-dom";
import { Button } from "@/components/ui/button";
import { useAuth } from "@/modules/auth/context/AuthContext";

export const AdminNavbar = () => {
  const { logout } = useAuth();

  return (
    <header className="border-b">
      <div className="container mx-auto px-4 py-4 flex justify-between items-center">
        <Link to="/admin" className="text-2xl font-bold">
          ToolShed Admin
        </Link>
        <div className="flex items-center gap-3">
          <Button variant="outline" asChild>
            <Link to="/admin">Dashboard</Link>
          </Button>
          <Button variant="outline" asChild>
            <Link to="/admin/users">Users</Link>
          </Button>
          <Button variant="outline" onClick={logout}>
            Logout
          </Button>
        </div>
      </div>
    </header>
  );
};
