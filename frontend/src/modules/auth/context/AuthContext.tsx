import { createContext, useContext, useState, useCallback } from 'react';
import type { ReactNode } from 'react';

// Define the shape of the user data
interface User {
  id: string;
  firstName: string;
  lastName: string;
  email: string;
  role: 'ADMIN' | 'SUPPLIER' | 'RENTER'; // Matching backend enum and frontend expectations
}

// Define the shape of the AuthContext
interface AuthContextType {
  user: User | null;
  isAuthenticated: boolean;
  role: User['role'] | null;
  login: (userData: User, token: string) => void;
  logout: () => void;
  token: string | null;
}

// Create the context with default values
const AuthContext = createContext<AuthContextType | undefined>(undefined);

// AuthProvider component to manage authentication state
interface AuthProviderProps {
  children: ReactNode;
}

export const AuthProvider = ({ children }: AuthProviderProps) => {
  const [user, setUser] = useState<User | null>(null);
  const [token, setToken] = useState<string | null>(null);
  const [isAuthenticated, setIsAuthenticated] = useState<boolean>(false);
  const [role, setRole] = useState<User['role'] | null>(null);

  // Function to handle user login
  const login = useCallback((userData: User, authToken: string) => {
    setUser(userData);
    setToken(authToken);
    setIsAuthenticated(true);
    setRole(userData.role);
    // In a real app, you'd store token/user data in localStorage/sessionStorage
    localStorage.setItem('authToken', authToken);
    localStorage.setItem('user', JSON.stringify(userData));
  }, []);

  // Function to handle user logout
  const logout = useCallback(() => {
    setUser(null);
    setToken(null);
    setIsAuthenticated(false);
    setRole(null);
    localStorage.removeItem('authToken');
    localStorage.removeItem('user');
  }, []);

  // Context value
  const value = {
    user,
    isAuthenticated,
    role,
    login,
    logout,
    token,
  };

  return <AuthContext.Provider value={value}>{children}</AuthContext.Provider>;
};

// Custom hook to use the AuthContext
export const useAuth = () => {
  const context = useContext(AuthContext);
  if (context === undefined) {
    throw new Error('useAuth must be used within an AuthProvider');
  }
  return context;
};
