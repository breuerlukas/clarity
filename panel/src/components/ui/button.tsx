import * as React from "react";
import { Slot as SlotPrimitive } from "radix-ui";
import { cva, type VariantProps } from "class-variance-authority";
import { LoaderCircle } from "lucide-react";

import { cn } from "@/lib/utils";

const Slot = SlotPrimitive.Slot;


const buttonVariants = cva(
  "inline-flex items-center justify-center gap-2 whitespace-nowrap rounded-full text-sm font-bold transition-all disabled:pointer-events-none disabled:opacity-50 [&_svg]:pointer-events-none [&_svg:not([class*='size-'])]:size-4 shrink-0 [&_svg]:shrink-0 aria-invalid:border-destructive",
  {
    variants: {
      variant: {
        default: "bg-primary text-primary-foreground hover:brightness-110",
        destructive: "bg-destructive text-on-brand hover:brightness-110",
        outline: "bg-secondary text-foreground hover:bg-accent",
        secondary: "bg-secondary text-secondary-foreground hover:bg-accent",
        ghost: "hover:bg-secondary hover:text-foreground",
        link: "text-brand-text underline-offset-4 hover:underline",
      },
      size: {
        default: "h-11 px-5 has-[>svg]:px-4",
        sm: "h-8 gap-1.5 px-3 text-[13px] has-[>svg]:px-3",
        lg: "h-14 px-7 text-base has-[>svg]:px-6",
        icon: "size-10",
        "icon-sm": "size-8",
        "icon-lg": "size-12",
      },
    },
    defaultVariants: {
      variant: "default",
      size: "default",
    },
  },
);

/**
 * `loading` disables the button and shows a spinner in front of its label
 * while an action runs; it has no effect together with `asChild`.
 */
function Button({
  className,
  variant,
  size,
  asChild = false,
  loading = false,
  disabled,
  children,
  ...props
}: React.ComponentProps<"button"> &
  VariantProps<typeof buttonVariants> & {
    asChild?: boolean;
    loading?: boolean;
  }) {
  const Comp = asChild ? Slot : "button";

  return (
    <Comp
      data-slot="button"
      className={cn(buttonVariants({ variant, size, className }))}
      disabled={disabled || loading}
      aria-busy={loading || undefined}
      {...props}
    >
      {asChild ? children : <>{loading && <LoaderCircle className="animate-spin" aria-hidden />}{children}</>}
    </Comp>
  );
}

export { Button, buttonVariants };
