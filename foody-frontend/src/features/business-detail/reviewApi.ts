import { apiRequest } from "../../lib/api";
import type { Review, ReviewListResponse } from "../../types/api";

export interface ReviewPayload {
  rating: number;
  comment?: string | null;
}

const base = (businessId: number) => `/api/businesses/${businessId}/reviews`;

export const reviewApi = {
  list: (businessId: number) => apiRequest<ReviewListResponse>(base(businessId), { auth: false }),
  mine: (businessId: number) => apiRequest<Review>(`${base(businessId)}/mine`),
  create: (businessId: number, payload: ReviewPayload) =>
    apiRequest<Review>(base(businessId), { method: "POST", body: payload }),
  update: (businessId: number, payload: ReviewPayload) =>
    apiRequest<Review>(`${base(businessId)}/mine`, { method: "PATCH", body: payload }),
  remove: (businessId: number) => apiRequest<void>(`${base(businessId)}/mine`, { method: "DELETE" }),
  adminRemove: (reviewId: number) => apiRequest<void>(`/api/admin/reviews/${reviewId}`, { method: "DELETE" }),
};
