// Mirrors the backend's DTOs exactly (see foody-backend/src/main/java/com/foody/**/dto).
// Kept as one file since the surface is small; split if it grows.

export type UserRole = "CUSTOMER" | "BUSINESS_OWNER" | "ADMIN";
export type UserStatus = "ACTIVE" | "SUSPENDED";

export interface User {
  id: number;
  email: string;
  phone: string | null;
  fullName: string;
  role: UserRole;
  status: UserStatus;
  createdAt: string;
  updatedAt: string;
}

export interface TokenResponse {
  accessToken: string;
  refreshToken: string;
  tokenType: string;
  expiresInSeconds: number;
}

export type BusinessStatus = "PENDING" | "APPROVED" | "REJECTED" | "SUSPENDED";
export type BusinessType = "CAFE" | "FAST_FOOD";

export interface Business {
  id: number;
  ownerUserId: number;
  name: string;
  description: string | null;
  businessType: string;
  address: string | null;
  latitude: number | null;
  longitude: number | null;
  phone: string | null;
  status: BusinessStatus;
  coverImageUrl: string | null;
  createdAt: string;
  updatedAt: string;
}

export interface Menu {
  id: number;
  businessId: number;
  name: string;
  displayOrder: number | null;
}

export interface Product {
  id: number;
  menuId: number;
  name: string;
  description: string | null;
  price: string; // BigDecimal serializes as a numeric JSON string-safe value; treat as string, parse for math.
  imageUrl: string | null;
  isAvailable: boolean;
  displayOrder: number | null;
}

export type FulfillmentType = "PICKUP" | "DELIVERY" | "DINE_IN";
export type OrderStatus =
  | "PENDING"
  | "ACCEPTED"
  | "PREPARING"
  | "READY"
  | "COMPLETED"
  | "REJECTED"
  | "CANCELLED";

export interface OrderItem {
  productId: number;
  productName: string;
  unitPrice: string;
  quantity: number;
  subtotal: string;
}

export interface Order {
  id: number;
  businessId: number;
  customerUserId: number;
  fulfillmentType: FulfillmentType;
  status: OrderStatus;
  deliveryAddress: string | null;
  totalAmount: string;
  items: OrderItem[];
  createdAt: string;
  updatedAt: string;
}

export type ReservationStatus = "PENDING" | "CONFIRMED" | "COMPLETED" | "REJECTED" | "CANCELLED";

export interface Reservation {
  id: number;
  businessId: number;
  customerUserId: number;
  date: string; // ISO date, e.g. 2026-09-01
  time: string; // ISO time, e.g. 20:30:00
  guestCount: number;
  status: ReservationStatus;
  createdAt: string;
  updatedAt: string;
}

export interface DashboardSummary {
  totalUsers: number;
  activeBusinesses: number;
  totalOrders: number;
  totalReservations: number;
}

export interface ApiErrorBody {
  timestamp: string;
  status: number;
  error: string;
  code: string;
  message: string;
  path: string;
  details: string[] | null;
}
