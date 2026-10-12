import { StrictMode } from "react";
import { createRoot } from "react-dom/client";
import {
  MutationCache,
  QueryCache,
  QueryClient,
  QueryClientProvider,
} from "@tanstack/react-query";
import { BrowserRouter } from "react-router";
import "@/global.css";
import "@/locales/i18n";
import { ApiError } from "@/lib/api";
import { applyTheme, watchSystemTheme } from "@/lib/theme";
import App from "@/App";

/**
 * Any 401 means the session ended (logout elsewhere, expiry, idle timeout):
 * forget it, and the route guard sends the owner to the login.
 */
const endSessionOnUnauthorized = (error: Error) => {
  if (error instanceof ApiError && error.status === 401) {
    queryClient.setQueryData(["session"], null);
  }
};

const queryClient = new QueryClient({
  queryCache: new QueryCache({ onError: endSessionOnUnauthorized }),
  mutationCache: new MutationCache({ onError: endSessionOnUnauthorized }),
  defaultOptions: {
    queries: {
      retry: (failureCount, error) =>
        !(error instanceof ApiError && error.status < 500) && failureCount < 2,
      refetchOnWindowFocus: true,
      staleTime: 60_000,
    },
  },
});

applyTheme();
watchSystemTheme();

createRoot(document.getElementById("root")!).render(
  <StrictMode>
    <QueryClientProvider client={queryClient}>
      <BrowserRouter basename={import.meta.env.BASE_URL}>
        <App />
      </BrowserRouter>
    </QueryClientProvider>
  </StrictMode>,
);