import { SignupForm } from "@/modules/auth/components/signup-form"
import { BrandLogo } from "@/modules/shared/components/BrandLogo"

export function RegisterPage() {
  return (
    <div className="flex min-h-svh w-full flex-col items-center justify-center p-6 md:p-10">
      <BrandLogo />
      <div className="w-full max-w-sm">
        <SignupForm />
      </div>
    </div>
  )
}
