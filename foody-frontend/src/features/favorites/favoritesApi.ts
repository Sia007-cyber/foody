import { apiRequest } from "../../lib/api";
import type { Business } from "../../types/api";

export const favoritesApi = {
  list: () => apiRequest<Business[]>("/api/favorites"),
  businessIds: () => apiRequest<number[]>("/api/favorites/business-ids"),
  add: (businessId: number) => apiRequest<void>(`/api/favorites/${businessId}`, { method: "PUT" }),
  remove: (businessId: number) => apiRequest<void>(`/api/favorites/${businessId}`, { method: "DELETE" }),
};
