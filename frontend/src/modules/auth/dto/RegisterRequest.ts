export type UserRole = 'SUPPLIER' | 'RENTER' | 'ADMIN';

export interface RegisterRequest {
  firstName: string;
  lastName: string;
  email: string;
  password: string;
  role: UserRole;
}
