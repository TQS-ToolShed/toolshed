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
  const [emailSuccess, setEmailSuccess] = useState("");
  const [passwordError, setPasswordError] = useState("");
  const [generalError, setGeneralError] = useState("");
  const [successMessage, setSuccessMessage] = useState("");
  const [isSubmitting, setIsSubmitting] = useState(false);
  const [isCheckingEmail, setIsCheckingEmail] = useState(false);
  const [emailTouched, setEmailTouched] = useState(false);

  // Email format validation
  const isValidEmailFormat = (emailValue: string) => {
    const emailRegex = /^[^\s@]+@[^\s@]+\.[^\s@]+$/;
    return emailRegex.test(emailValue);
  };

  // Handle email blur for required validation
  const handleEmailBlur = () => {
    setEmailTouched(true);
    if (email.length === 0) {
      setEmailError("Email is required");
      setEmailSuccess("");
    }
  };

  // Debounce email check
  useEffect(() => {
    if (email.length === 0) {
      // Only show error if field has been touched
      if (emailTouched) {
        setEmailError("Email is required");
      } else {
        setEmailError("");
      }
      setEmailSuccess("");
      setIsCheckingEmail(false);
      return;
    }

    // Check format first
    if (!isValidEmailFormat(email)) {
      setEmailError("Please enter a valid email address");
      setEmailSuccess("");
      setIsCheckingEmail(false);
      return;
    }

    setIsCheckingEmail(true);
    const handler = setTimeout(async () => {
      try {
        const isTaken = await checkEmailTaken(email);
        if (isTaken) {
          setEmailError("Email is already registered");
          setEmailSuccess("");
        } else {
          setEmailError("");
          setEmailSuccess("Email is available");
        }
      } catch (err) {
        console.error("Email check failed:", err);
        setEmailError("Could not verify email. Try again.");
        setEmailSuccess("");
      } finally {
        setIsCheckingEmail(false);
      }
    }, 500); // 500ms debounce

    return () => {
      clearTimeout(handler);
    };
  }, [email, emailTouched]);

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

      // Manual validation to match E2E test expectations
      if (!firstName) {
        setGeneralError("First name is required");
        setIsSubmitting(false);
        return;
      }
      if (!lastName) {
        setGeneralError("Last name is required");
        setIsSubmitting(false);
        return;
      }
      if (!email) {
        setGeneralError("Email is required");
        setIsSubmitting(false);
        return;
      }
      if (!password) {
        setGeneralError("Password is required");
        setIsSubmitting(false);
        return;
      }
      if (password.length < 8) {
        setGeneralError("Password must be at least 8 characters long");
        setIsSubmitting(false);
        return;
      }
      if (!confirmPassword) {
        setGeneralError("Confirm Password is required");
        setIsSubmitting(false);
        return;
      }
      if (!role) {
        setGeneralError("Please select a role");
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
        // Set success message for E2E tests to detect
        setSuccessMessage("Registration successful");
        // Delay to allow tests to see the success message before redirect
        setTimeout(() => {
          navigate("/login");
        }, 1500);
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
        <form onSubmit={handleSubmit} noValidate>
          <FieldGroup>
            <div className="grid grid-cols-2 gap-4">
              <Field>
                <FieldLabel htmlFor="firstName">First Name</FieldLabel>
                <Input
                  id="firstName"
                  name="firstName"
                  data-testid="first-name"
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
                  name="lastName"
                  data-testid="last-name"
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
                name="email"
                data-testid="email"
                type="email"
                placeholder="m@example.com"
                required
                value={email}
                onChange={(e) => setEmail(e.target.value)}
                onBlur={handleEmailBlur}
                aria-describedby="email-error"
              />
              {emailError && (
                <p
                  id="email-error"
                  data-testid="email-error"
                  className="email-error error-message validation-error text-sm text-destructive mt-1 flex items-center gap-1"
                >
                  <span className="inline-block">⚠</span>
                  {emailError}
                </p>
              )}
              {emailSuccess && !emailError && (
                <p
                  id="email-success"
                  data-testid="email-success"
                  className="email-success success-icon text-sm text-green-500 mt-1 flex items-center gap-1"
                >
                  <span className="check-icon inline-block">✓</span>
                  {emailSuccess}
                </p>
              )}
            </Field>
            <Field>
              <FieldLabel htmlFor="role">I want to...</FieldLabel>
              <select
                id="role"
                name="role"
                data-testid="role"
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
                name="password"
                data-testid="password"
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
                {successMessage && (
                  <p
                    data-testid="success-message"
                    className="success-message text-sm text-green-500 text-center mb-2"
                  >
                    {successMessage}
                  </p>
                )}
                {generalError && (
                  <p
                    data-testid="error-message"
                    className="error-message text-sm text-destructive text-center mb-2"
                  >
                    {generalError}
                  </p>
                )}
                <Button
                  type="submit"
                  data-testid="register-button"
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
