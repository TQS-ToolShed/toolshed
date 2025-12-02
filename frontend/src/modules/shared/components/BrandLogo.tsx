import { Hammer } from "lucide-react";

export function BrandLogo({ className = "" }: { className?: string }) {
  return (
    <div className={`flex flex-col items-center gap-2 mb-8 ${className}`}>
      <div className="flex h-12 w-12 items-center justify-center rounded-xl bg-primary text-primary-foreground shadow-lg">
        <Hammer className="h-8 w-8" />
      </div>
      <h1 className="text-3xl font-bold tracking-tight text-foreground">ToolShed</h1>
      <p className="text-muted-foreground text-sm">Your local tool sharing community</p>
    </div>
  );
}
