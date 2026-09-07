import { apiRequest } from "../../lib/api";
import type { Offer, OfferClaim } from "../../types/api";

export const customerOffersApi = {
  getClaimableOffers: () => apiRequest<Offer[]>("/api/offers", { auth: false }),
  claimOffer: (offerId: number) => apiRequest<OfferClaim>(`/api/offers/${offerId}/claim`, { method: "POST" }),
  getMyClaims: () => apiRequest<OfferClaim[]>("/api/offers/my-claims"),
};
