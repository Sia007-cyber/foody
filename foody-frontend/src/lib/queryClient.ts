import { QueryClient } from "@tanstack/react-query";

import { ApiError } from "./api.ts";
import { onSessionBoundary, SessionChangedError } from "./session.ts";

export const queryClient = new QueryClient({
  defaultOptions: {
    queries: {
      staleTime: 30_000,
      retry: (failureCount, error) =>
        !(error instanceof SessionChangedError) &&
        !(error instanceof ApiError && error.status === 401) && failureCount < 1,
      refetchOnWindowFocus: false,
    },
  },
});

// Some owner/public catalog keys overlap. Clearing everything is the small,
// complete boundary: it also removes mutations and cancels scheduled retries.
onSessionBoundary(() => {
  void queryClient.cancelQueries();
  queryClient.clear();
});
