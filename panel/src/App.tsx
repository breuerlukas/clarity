import { Navigate, Outlet, Route, Routes, useLocation } from "react-router";
import { useSession, useSetupStatus } from "@/hooks/use-session";
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

export default function App() {
  return (
    <Routes>
      <Route path="/setup" element={<SetupPage />} />
      <Route path="/login" element={<LoginPage />} />
      <Route element={<RequireOwner />}>
        <Route element={<PanelLayout />}>
          <Route index element={<OverviewPage />} />
          <Route path="transactions" element={<TransactionsPage />} />
          <Route path="statistics" element={<StatisticsPage />} />
          <Route path="subscriptions" element={<SubscriptionsPage />} />
          <Route path="accounts" element={<AccountsPage />} />
          <Route path="business">
            <Route index element={<BusinessOverviewPage />} />
            <Route path="contracts" element={<ContractsPage />} />
            <Route path="accounts" element={<BusinessAccountsPage />} />
          </Route>
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