import { useState, useEffect, useCallback } from "react";
import { Button } from "@/components/ui/button";
import { Link, useNavigate } from "react-router-dom";
import {
  Card,
  CardContent,
  CardDescription,
  CardHeader,
  CardTitle,
} from "@/components/ui/card";
import {
  Field,
  FieldDescription,
  FieldGroup,
  FieldLabel,
} from "@/components/ui/field";
import { Input } from "@/components/ui/input";
import { checkEmailTaken, registerUser } from "@/modules/auth/api/auth.service";
import type { UserRole } from "@/modules/auth/dto/RegisterRequest";

export function SignupForm({ ...props }: React.ComponentProps<typeof Card>) {
  const navigate = useNavigate();

  const [firstName, setFirstName] = useState("");
  const [lastName, setLastName] = useState("");
  const [email, setEmail] = useState("");
  const [password, setPassword] = useState("");
  const [confirmPassword, setConfirmPassword] = useState("");
  const [role, setRole] = useState<UserRole>("RENTER");

  const [emailError, setEmailError] = useState("");
  const [passwordError, setPasswordError] = useState("");
  const [generalError, setGeneralError] = useState("");
  const [isSubmitting, setIsSubmitting] = useState(false);

  // Debounce email check
  useEffect(() => {
    if (email.length === 0) {
      setEmailError("");
      return;
    }

    const handler = setTimeout(async () => {
      try {
        const isTaken = await checkEmailTaken(email);
        if (isTaken) {
          setEmailError("Email already registered.");
        } else {
          setEmailError("");
        }
      } catch (err) {
        console.error("Email check failed:", err);
        setEmailError("Could not verify email. Try again.");
      }
    }, 500); // 500ms debounce

    return () => {
      clearTimeout(handler);
    };
  }, [email]);

  // Password match validation
  useEffect(() => {
    if (password && confirmPassword && password !== confirmPassword) {
      setPasswordError("Passwords do not match.");
    } else {
      setPasswordError("");
    }
  }, [password, confirmPassword]);

  const handleSubmit = useCallback(
    async (e: React.FormEvent) => {
      e.preventDefault();
      setGeneralError("");
      setIsSubmitting(true);

      if (emailError || passwordError) {
        setIsSubmitting(false);
        return;
      }

      if (
        !firstName ||
        !lastName ||
        !email ||
        !password ||
        !confirmPassword ||
        !role
      ) {
        setGeneralError("Please fill in all required fields.");
        setIsSubmitting(false);
        return;
      }

      if (password !== confirmPassword) {
        setGeneralError("Passwords do not match.");
        setIsSubmitting(false);
        return;
      }

      try {
        await registerUser({ firstName, lastName, email, password, role });
        alert("Registration successful! Please log in.");
        navigate("/login");
      } catch (error: any) {
        console.error("Registration failed:", error);
        setGeneralError(
          error.message || "Registration failed. Please try again."
        );
      } finally {
        setIsSubmitting(false);
      }
    },
    [
      firstName,
      lastName,
      email,
      password,
      confirmPassword,
      role,
      emailError,
      passwordError,
      navigate,
    ]
  );

  return (
    <Card {...props}>
      <CardHeader>
        <CardTitle>Create an account</CardTitle>
        <CardDescription>
          Enter your information below to create your account
        </CardDescription>
      </CardHeader>
      <CardContent>
        <form onSubmit={handleSubmit}>
          <FieldGroup>
            <div className="grid grid-cols-2 gap-4">
              <Field>
                <FieldLabel htmlFor="firstName">First Name</FieldLabel>
                <Input
                  id="firstName"
                  type="text"
                  placeholder="John"
                  required
                  value={firstName}
                  onChange={(e) => setFirstName(e.target.value)}
                />
              </Field>
              <Field>
                <FieldLabel htmlFor="lastName">Last Name</FieldLabel>
                <Input
                  id="lastName"
                  type="text"
                  placeholder="Doe"
                  required
                  value={lastName}
                  onChange={(e) => setLastName(e.target.value)}
                />
              </Field>
            </div>
            <Field>
              <FieldLabel htmlFor="email">Email</FieldLabel>
              <Input
                id="email"
                type="email"
                placeholder="m@example.com"
                required
                value={email}
                onChange={(e) => setEmail(e.target.value)}
                aria-describedby="email-error"
              />
              {emailError && (
                <p id="email-error" className="text-sm text-destructive mt-1">
                  {emailError}
                </p>
              )}
            </Field>
            <Field>
              <FieldLabel htmlFor="role">I want to...</FieldLabel>
              <select
                id="role"
                className="flex h-10 w-full rounded-md border border-input bg-background px-3 py-2 text-sm ring-offset-background focus-visible:outline-none focus-visible:ring-2 focus-visible:ring-ring focus-visible:ring-offset-2 disabled:cursor-not-allowed disabled:opacity-50"
                required
                value={role}
                onChange={(e) => setRole(e.target.value as UserRole)}
              >
                <option value="RENTER">Rent Tools</option>
                <option value="SUPPLIER">Lend Tools (Supplier)</option>
                <option value="ADMIN">Administrator</option>
              </select>
            </Field>
            <Field>
              <FieldLabel htmlFor="password">Password</FieldLabel>
              <Input
                id="password"
                type="password"
                required
                value={password}
                onChange={(e) => setPassword(e.target.value)}
                aria-describedby="password-error"
              />
            </Field>
            <Field>
              <FieldLabel htmlFor="confirm-password">
                Confirm Password
              </FieldLabel>
              <Input
                id="confirm-password"
                type="password"
                required
                value={confirmPassword}
                onChange={(e) => setConfirmPassword(e.target.value)}
                aria-describedby="password-error"
              />
              {passwordError && (
                <p
                  id="password-error"
                  className="text-sm text-destructive mt-1"
                >
                  {passwordError}
                </p>
              )}
            </Field>
            <FieldGroup>
              <Field>
                {generalError && (
                  <p className="text-sm text-destructive text-center mb-2">
                    {generalError}
                  </p>
                )}
                <Button
                  type="submit"
                  className="w-full"
                  disabled={isSubmitting || !!emailError || !!passwordError}
                >
                  {isSubmitting ? "Registering..." : "Create Account"}
                </Button>
                <FieldDescription className="text-center">
                  Already have an account?{" "}
                  <Link to="/login" className="underline hover:text-primary">
                    Sign in
                  </Link>
                </FieldDescription>
              </Field>
            </FieldGroup>
          </FieldGroup>
        </form>
      </CardContent>
    </Card>
  );
}
