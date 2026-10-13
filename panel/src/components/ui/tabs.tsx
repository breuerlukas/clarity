import * as React from "react";
import { Tabs as TabsPrimitive } from "radix-ui";
import { PILL_CLASS, useSlidingPill } from "@/hooks/use-sliding-pill";
import { cn } from "@/lib/utils";

function Tabs(props: React.ComponentProps<typeof TabsPrimitive.Root>) {
  return <TabsPrimitive.Root data-slot="tabs" {...props} />;
}

function TabsList({ className, children, ...props }: React.ComponentProps<typeof TabsPrimitive.List>) {
  const { container, pill } = useSlidingPill<HTMLDivElement>('[data-state="active"]');
  return (
    <TabsPrimitive.List
      ref={container}
      data-slot="tabs-list"
      className={cn(
        "relative inline-flex max-w-full items-center gap-1 overflow-x-auto rounded-full bg-card p-1 ring-1 ring-divider",
        className,
      )}
      {...props}
    >
      <span ref={pill} aria-hidden="true" className={cn(PILL_CLASS, "bg-brand/12")} />
      {children}
    </TabsPrimitive.List>
  );
}

function TabsTrigger({ className, ...props }: React.ComponentProps<typeof TabsPrimitive.Trigger>) {
  return (
    <TabsPrimitive.Trigger
      data-slot="tabs-trigger"
      className={cn(
        "relative inline-flex h-9 shrink-0 items-center gap-2 rounded-full px-4 text-sm font-bold whitespace-nowrap text-muted-foreground transition-colors duration-300 hover:text-foreground data-[state=active]:text-nav-active-text [&_svg]:size-4",
        className,
      )}
      {...props}
    />
  );
}

function TabsContent({ className, ...props }: React.ComponentProps<typeof TabsPrimitive.Content>) {
  return (
    <TabsPrimitive.Content
      data-slot="tabs-content"
      className={cn(
        "mt-6 outline-none data-[state=active]:animate-in data-[state=active]:fade-in-0 data-[state=active]:slide-in-from-bottom-2 data-[state=active]:duration-300 data-[state=active]:ease-out",
        className,
      )}
      {...props}
    />
  );
}

export { Tabs, TabsList, TabsTrigger, TabsContent };
