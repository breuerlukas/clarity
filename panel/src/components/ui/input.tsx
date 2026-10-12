import * as React from "react";

import { cn } from "@/lib/utils";

function Input({ className, type, ...props }: React.ComponentProps<"input">) {
  return (
    <input
      type={type}
      data-slot="input"
      className={cn(
        "h-12 w-full min-w-0 rounded-2xl border-[1.5px] border-transparent bg-ground px-4 text-sm text-foreground placeholder:text-placeholder outline-none transition-colors focus-visible:border-primary aria-invalid:border-destructive disabled:cursor-not-allowed disabled:opacity-50 file:inline-flex file:h-7 file:border-0 file:bg-transparent file:text-sm file:font-medium file:text-foreground selection:bg-primary selection:text-primary-foreground",
        className,
      )}
      {...props}
    />
  );
}

export { Input };
