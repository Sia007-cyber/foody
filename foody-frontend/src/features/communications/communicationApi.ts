import { apiRequest } from "../../lib/api";
import type { BusinessMessage, PageResponse, TicketDetail, TicketRequesterType, TicketStatus, TicketSummary, UnreadCountResponse } from "../../types/api";

export const ownerCommunicationApi = {
  tickets: () => apiRequest<PageResponse<TicketSummary>>("/api/business/communications/tickets"),
  ticket: (id: number) => apiRequest<TicketDetail>(`/api/business/communications/tickets/${id}`),
  createTicket: (subject: string, message: string) => apiRequest<TicketDetail>("/api/business/communications/tickets", { method: "POST", body: { subject, message } }),
  reply: (id: number, message: string) => apiRequest<TicketDetail>(`/api/business/communications/tickets/${id}/replies`, { method: "POST", body: { message } }),
  messages: () => apiRequest<PageResponse<BusinessMessage>>("/api/business/communications/messages"),
  message: (id: number) => apiRequest<BusinessMessage>(`/api/business/communications/messages/${id}`),
  unreadCount: () => apiRequest<UnreadCountResponse>("/api/business/communications/unread-count"),
};

export const customerCommunicationApi = {
  tickets: () => apiRequest<PageResponse<TicketSummary>>("/api/customer/communications/tickets"),
  ticket: (id: number) => apiRequest<TicketDetail>(`/api/customer/communications/tickets/${id}`),
  createTicket: (subject: string, message: string) => apiRequest<TicketDetail>("/api/customer/communications/tickets", { method: "POST", body: { subject, message } }),
  reply: (id: number, message: string) => apiRequest<TicketDetail>(`/api/customer/communications/tickets/${id}/replies`, { method: "POST", body: { message } }),
  unreadCount: () => apiRequest<UnreadCountResponse>("/api/customer/communications/unread-count"),
};

export const adminCommunicationApi = {
  tickets: (status?: TicketStatus, requesterType?: TicketRequesterType) => apiRequest<PageResponse<TicketSummary>>("/api/admin/communications/tickets", { query: { status, requesterType } }),
  ticket: (id: number) => apiRequest<TicketDetail>(`/api/admin/communications/tickets/${id}`),
  reply: (id: number, message: string) => apiRequest<TicketDetail>(`/api/admin/communications/tickets/${id}/replies`, { method: "POST", body: { message } }),
  close: (id: number) => apiRequest<TicketDetail>(`/api/admin/communications/tickets/${id}/close`, { method: "PATCH" }),
  sendDirect: (businessId: number, subject: string, message: string) => apiRequest<BusinessMessage>("/api/admin/communications/messages/direct", { method: "POST", body: { businessId, subject, message } }),
  sendBroadcast: (subject: string, message: string) => apiRequest<BusinessMessage>("/api/admin/communications/messages/broadcast", { method: "POST", body: { subject, message } }),
  sent: () => apiRequest<PageResponse<BusinessMessage>>("/api/admin/communications/messages/sent"),
};
