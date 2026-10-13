import type { ReactElement } from "react";
import { Navigate, Outlet, Route, Routes, useLocation, useParams } from "react-router";
import { useSession, useSetupStatus } from "@/hooks/use-session";
import {
  lastSpace,
  rememberSpace,
  spacePath,
  SpaceContext,
  useSpace,
  useSpaces,
} from "@/hooks/use-space";
import { PanelLayout } from "@/layouts/panel";
import { Splash } from "@/components/splash";
import { SetupPage } from "@/pages/setup-page";
import { LoginPage } from "@/pages/login-page";
import { OverviewPage } from "@/pages/overview/overview-page";
import { TransactionsPage } from "@/pages/transactions/transactions-page";
import { StatisticsPage } from "@/pages/statistics/statistics-page";
import { SubscriptionsPage } from "@/pages/subscriptions/subscriptions-page";
import { AccountsPage } from "@/pages/accounts/accounts-page";
import { ConnectCallbackPage } from "@/pages/accounts/connect-callback-page";
import { BusinessOverviewPage } from "@/pages/business/business-overview-page";
import { ContractsPage } from "@/pages/business/contracts-page";
import { BusinessAccountsPage } from "@/pages/business/business-accounts-page";
import { InvoicesPage } from "@/pages/business/invoices-page";

export default function App() {
  return (
    <Routes>
      <Route path="/setup" element={<SetupPage />} />
      <Route path="/login" element={<LoginPage />} />
      <Route element={<RequireOwner />}>
        <Route index element={<OpenLastSpace />} />
        <Route path="s/:space" element={<SpaceRoute />}>
          <Route index element={<ByKind personal={<OverviewPage />} business={<BusinessOverviewPage />} />} />
          <Route path="transactions" element={<ByKind personal={<TransactionsPage />} />} />
          <Route path="statistics" element={<ByKind personal={<StatisticsPage />} />} />
          <Route path="subscriptions" element={<ByKind personal={<SubscriptionsPage />} />} />
          <Route path="contracts" element={<ByKind business={<ContractsPage />} />} />
          <Route path="invoices" element={<ByKind business={<InvoicesPage />} />} />
          <Route path="accounts" element={<ByKind personal={<AccountsPage />} business={<BusinessAccountsPage />} />} />
        </Route>
        <Route path="connect/callback" element={<ConnectCallbackPage />} />
      </Route>
      <Route path="*" element={<Navigate to="/" replace />} />
    </Routes>
  );
}

/**
 * Only a convenience for the screen; the api enforces the session on every
 * request regardless of what the browser shows.
 */
function RequireOwner() {
  const location = useLocation();
  const setup = useSetupStatus();
  const session = useSession();
  if (setup.isPending || session.isPending) {
    return <Splash />;
  }
  if (setup.data?.setup_required) {
    return <Navigate to="/setup" replace />;
  }
  if (!session.data) {
    return <Navigate to="/login" replace state={{ from: location }} />;
  }
  return <Outlet />;
}

/** The root opens the space used last, or the first one. */
function OpenLastSpace() {
  const spaces = useSpaces();
  if (!spaces.data?.length) {
    return <Splash />;
  }
  const last = spaces.data.find((space) => space.slug === lastSpace());
  return <Navigate to={spacePath(last ?? spaces.data[0])} replace />;
}

/** Finds the space the address names and lays out its pages. */
function SpaceRoute() {
  const { space: slug } = useParams();
  const spaces = useSpaces();
  if (!spaces.data) {
    return <Splash />;
  }
  const space = spaces.data.find((entry) => entry.slug === slug);
  if (!space) {
    return <Navigate to="/" replace />;
  }
  rememberSpace(space.slug);
  return (
    <SpaceContext.Provider value={space}>
      <PanelLayout />
    </SpaceContext.Provider>
  );
}

/** A page for one kind of space; the other kind lands on its overview. */
function ByKind({ personal, business }: { personal?: ReactElement; business?: ReactElement }) {
  const space = useSpace();
  const page = space.kind === "BUSINESS" ? business : personal;
  return page ?? <Navigate to={spacePath(space)} replace />;
}
