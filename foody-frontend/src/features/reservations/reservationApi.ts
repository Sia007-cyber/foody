import { apiRequest } from "../../lib/api";
import type { Reservation, ReservationStatus } from "../../types/api";

export interface CreateReservationPayload {
  businessId: number;
  date: string; // YYYY-MM-DD
  time: string; // HH:mm
  guestCount: number;
}

export const reservationApi = {
  create: (payload: CreateReservationPayload) =>
    apiRequest<Reservation>("/api/reservations", { method: "POST", body: payload }),

  myReservations: () => apiRequest<Reservation[]>("/api/reservations/my"),

  getById: (id: number) => apiRequest<Reservation>(`/api/reservations/${id}`),

  cancel: (id: number) => apiRequest<Reservation>(`/api/reservations/${id}/cancel`, { method: "PATCH" }),

  availability: (businessId: number, date: string) =>
    apiRequest<Reservation[]>(`/api/businesses/${businessId}/reservation-availability`, {
      auth: false,
      query: { date },
    }),

  // Business owner panel
  businessReservations: (date?: string) =>
    apiRequest<Reservation[]>("/api/business/reservations", { query: { date } }),

  updateStatus: (id: number, status: ReservationStatus) =>
    apiRequest<Reservation>(`/api/business/reservations/${id}/status`, {
      method: "PATCH",
      body: { status },
    }),
};
