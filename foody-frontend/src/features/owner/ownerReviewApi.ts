import { apiRequest } from "../../lib/api"; import type { OwnerReviews } from "../../types/api";
export const ownerReviewApi={list:()=>apiRequest<OwnerReviews>("/api/business/reviews")};
