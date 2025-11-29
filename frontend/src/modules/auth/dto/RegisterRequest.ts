export type UserRole = 'SUPPLIER' | 'RENTER';

export interface RegisterRequest {
  firstName: string;
  lastName: string;
  email: string;
  password: string;
  role: UserRole;
}
