import { apiRequest } from "../../lib/api";
import type { FulfillmentType, Order, OrderStatus } from "../../types/api";

export interface CreateOrderPayload {
  businessId: number;
  fulfillmentType: FulfillmentType;
  items: { productId: number; quantity: number }[];
  deliveryAddress?: string;
}

export const orderApi = {
  create: (payload: CreateOrderPayload) =>
    apiRequest<Order>("/api/orders", { method: "POST", body: payload }),

  myOrders: () => apiRequest<Order[]>("/api/orders/my"),

  getById: (id: number) => apiRequest<Order>(`/api/orders/${id}`),

  cancel: (id: number) => apiRequest<Order>(`/api/orders/${id}/cancel`, { method: "PATCH" }),

  // Business owner panel
  businessOrders: (status?: OrderStatus) =>
    apiRequest<Order[]>("/api/business/orders", { query: { status } }),

  updateStatus: (id: number, status: OrderStatus) =>
    apiRequest<Order>(`/api/business/orders/${id}/status`, { method: "PATCH", body: { status } }),
};
