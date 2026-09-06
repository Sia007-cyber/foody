import type { Order, Reservation } from "../../types/api";

export function localDateKey(value: Date): string {
  const year = value.getFullYear();
  const month = String(value.getMonth() + 1).padStart(2, "0");
  const day = String(value.getDate()).padStart(2, "0");
  return `${year}-${month}-${day}`;
}

function startOfLocalDay(value: Date): Date {
  return new Date(value.getFullYear(), value.getMonth(), value.getDate());
}

/** Includes today and the preceding 29 local calendar days. */
export function isInLast30LocalCalendarDays(value: string, now = new Date()): boolean {
  const recordDate = new Date(value);
  if (Number.isNaN(recordDate.getTime())) return false;

  const today = startOfLocalDay(now);
  const firstDay = new Date(today.getFullYear(), today.getMonth(), today.getDate() - 29);
  const localRecordDay = startOfLocalDay(recordDate);
  return localRecordDay >= firstDay && localRecordDay <= today;
}

function isLocalDateKeyInLast30CalendarDays(dateKey: string, now: Date): boolean {
  const today = startOfLocalDay(now);
  const firstDay = new Date(today.getFullYear(), today.getMonth(), today.getDate() - 29);
  return dateKey >= localDateKey(firstDay) && dateKey <= localDateKey(today);
}

export function getLast30DayMetrics(orders: Order[], reservations: Reservation[], now = new Date()) {
  const recentOrders = orders.filter((order) => isInLast30LocalCalendarDays(order.createdAt, now));
  const recentReservations = reservations.filter((reservation) => isLocalDateKeyInLast30CalendarDays(reservation.date, now));
  const completedOrders = recentOrders.filter((order) => order.status === "COMPLETED");
  const completedRevenue = completedOrders.reduce((sum, order) => sum + Number(order.totalAmount), 0);

  return {
    orderCount: recentOrders.length,
    reservationCount: recentReservations.length,
    completedRevenue,
    averageOrderValue: completedOrders.length > 0 ? completedRevenue / completedOrders.length : 0,
  };
}
