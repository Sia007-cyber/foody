// Mirrors the backend's DTOs exactly (see foody-backend/src/main/java/com/foody/**/dto).
// Kept as one file since the surface is small; split if it grows.

export type UserRole = "CUSTOMER" | "BUSINESS_OWNER" | "ADMIN";
export type UserStatus = "ACTIVE" | "SUSPENDED";

export interface User {
  id: number;
  publicId: string | null;
  email: string;
  phone: string | null;
  address: string | null;
  latitude: number | null;
  longitude: number | null;
  profileImageUrl: string | null;
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

// Admin panel view: same shape as Order plus the business/customer names and
// contact info, since the admin overview spans every business.
export interface AdminOrder {
  id: number;
  businessId: number;
  businessName: string;
  customerUserId: number;
  customerName: string;
  customerEmail: string | null;
  customerPhone: string | null;
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

export type NotificationType =
  | "ORDER_STATUS_CHANGED"
  | "NEW_ORDER"
  | "RESERVATION_STATUS_CHANGED"
  | "NEW_RESERVATION"
  | "BUSINESS_STATUS_CHANGED";

export interface Notification {
  id: number;
  type: NotificationType;
  title: string;
  message: string;
  referenceType: string | null;
  referenceId: number | null;
  read: boolean;
  createdAt: string;
}

export interface UnreadCountResponse {
  unreadCount: number;
}

export type WalletTransactionType =
  | "OWNER_CREDIT"
  | "OWNER_DEBIT"
  | "ADMIN_CREDIT"
  | "ADMIN_DEBIT";

export interface Wallet {
  id: number;
  customerUserId: number;
  businessId: number;
  balance: string;
}

export interface OwnerWallet {
  id: number;
  businessId: number;
  balance: string;
  customerPublicId: string;
  customerDisplayName: string;
}

export interface CustomerLookup {
  publicId: string;
  displayName: string;
}

export interface WalletTransaction {
  id: number;
  walletId: number;
  type: WalletTransactionType;
  amount: string;
  actorUserId: number;
  actorType: "OWNER" | "ADMIN";
  balanceAfter: string;
  debitRequestId: number | null;
  createdAt: string;
}

export type DebitRequestStatus = "PENDING" | "APPROVED" | "REJECTED";

export interface DebitRequest {
  id: number;
  walletId: number;
  customerUserId: number;
  businessId: number;
  requestedByOwnerUserId: number;
  amount: string;
  status: DebitRequestStatus;
  createdAt: string;
  resolvedAt: string | null;
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

export type OfferStatus = "ACTIVE" | "CANCELLED";

export interface Offer {
  id: number;
  businessId: number;
  businessName: string | null;
  title: string;
  description: string | null;
  capacity: number;
  claimCount: number;
  remainingAvailability: number;
  startsAt: string;
  expiresAt: string;
  status: OfferStatus;
  createdAt: string;
  updatedAt: string;
}

export interface CreateOfferRequest {
  title: string;
  description?: string;
  capacity: number;
  startsAt: string;
  expiresAt: string;
}
