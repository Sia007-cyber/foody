import type {
  BusinessStatus,
  OrderStatus,
  ReservationStatus,
  UserStatus,
} from "../types/api";

type Tone = "ok" | "danger" | "pending" | "suspended" | "ember";

export function Badge({ tone, children }: { tone: Tone; children: string }) {
  return (
    <span className={`badge badge-${tone}`}>
      <span className="badge-dot" />
      {children}
    </span>
  );
}

const businessStatusMap: Record<BusinessStatus, { label: string; tone: Tone }> = {
  PENDING: { label: "در انتظار تایید", tone: "pending" },
  APPROVED: { label: "تایید‌شده", tone: "ok" },
  REJECTED: { label: "رد‌شده", tone: "danger" },
  SUSPENDED: { label: "معلق", tone: "suspended" },
};

export function BusinessStatusBadge({ status }: { status: BusinessStatus }) {
  const info = businessStatusMap[status];
  return <Badge tone={info.tone}>{info.label}</Badge>;
}

const orderStatusMap: Record<OrderStatus, { label: string; tone: Tone }> = {
  PENDING: { label: "در انتظار", tone: "pending" },
  ACCEPTED: { label: "پذیرفته‌شده", tone: "ember" },
  PREPARING: { label: "در حال آماده‌سازی", tone: "ember" },
  READY: { label: "آماده", tone: "ok" },
  COMPLETED: { label: "تکمیل‌شده", tone: "ok" },
  REJECTED: { label: "رد‌شده", tone: "danger" },
  CANCELLED: { label: "لغو‌شده", tone: "danger" },
};

export function OrderStatusBadge({ status }: { status: OrderStatus }) {
  const info = orderStatusMap[status];
  return <Badge tone={info.tone}>{info.label}</Badge>;
}

const reservationStatusMap: Record<ReservationStatus, { label: string; tone: Tone }> = {
  PENDING: { label: "در انتظار", tone: "pending" },
  CONFIRMED: { label: "تایید‌شده", tone: "ok" },
  COMPLETED: { label: "تکمیل‌شده", tone: "ok" },
  REJECTED: { label: "رد‌شده", tone: "danger" },
  CANCELLED: { label: "لغو‌شده", tone: "danger" },
};

export function ReservationStatusBadge({ status }: { status: ReservationStatus }) {
  const info = reservationStatusMap[status];
  return <Badge tone={info.tone}>{info.label}</Badge>;
}

const userStatusMap: Record<UserStatus, { label: string; tone: Tone }> = {
  ACTIVE: { label: "فعال", tone: "ok" },
  SUSPENDED: { label: "معلق", tone: "suspended" },
};

export function UserStatusBadge({ status }: { status: UserStatus }) {
  const info = userStatusMap[status];
  return <Badge tone={info.tone}>{info.label}</Badge>;
}
