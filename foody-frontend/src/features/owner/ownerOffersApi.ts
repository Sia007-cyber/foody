import { apiRequest } from "../../lib/api";
import type { CreateOfferRequest, Offer, OwnerOfferClaim } from "../../types/api";

/** Offer endpoints derive the business solely from the authenticated owner. */
export const ownerOffersApi = {
  getOffers: () => apiRequest<Offer[]>("/api/business/offers"),
  createOffer: (offer: CreateOfferRequest) =>
    apiRequest<Offer>("/api/business/offers", { method: "POST", body: offer }),
  cancelOffer: (id: number) =>
    apiRequest<Offer>(`/api/business/offers/${id}/cancel`, { method: "PATCH" }),
  getClaims: (id: number) => apiRequest<OwnerOfferClaim[]>(`/api/business/offers/${id}/claims`),
};
