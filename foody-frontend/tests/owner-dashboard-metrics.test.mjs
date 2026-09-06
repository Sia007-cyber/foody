import test from "node:test";
import assert from "node:assert/strict";

const { getLast30DayMetrics, localDateKey } = await import("../src/features/owner/dashboardMetrics.ts");

const now = new Date(2026, 8, 6, 12);
const order = (id, createdAt, status = "COMPLETED", totalAmount = "100") => ({ id, createdAt, status, totalAmount });
const reservation = (id, date) => ({ id, date });

test("uses the inclusive last 30 local calendar days for owner metrics", () => {
  const metrics = getLast30DayMetrics(
    [
      order(1, new Date(2026, 7, 8, 10).toISOString(), "COMPLETED", "120"),
      order(2, new Date(2026, 7, 7, 10).toISOString(), "COMPLETED", "500"),
      order(3, new Date(2026, 8, 6, 20).toISOString(), "PENDING", "80"),
    ],
    [reservation(1, "2026-08-08"), reservation(2, "2026-08-07"), reservation(3, "2026-09-06")],
    now,
  );

  assert.deepEqual(metrics, {
    orderCount: 2,
    reservationCount: 2,
    completedRevenue: 120,
    averageOrderValue: 120,
  });
});

test("formats today's key from local date parts", () => {
  assert.equal(localDateKey(new Date(2026, 0, 2, 0, 30)), "2026-01-02");
});
