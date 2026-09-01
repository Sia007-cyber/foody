import { useState } from "react";
import { useMutation, useQuery, useQueryClient } from "@tanstack/react-query";
import { reservationApi } from "../reservations/reservationApi";
import { DashboardShell } from "../../components/DashboardShell";
import { Input } from "../../components/Field";
import { Button } from "../../components/Button";
import { ReservationStatusBadge } from "../../components/Badge";
import { PageSpinner, EmptyState } from "../../components/Controls";
import { useToast, errorMessage } from "../../components/Feedback";
import { formatDate, formatTime } from "../../lib/format";
import { ownerNavItems } from "./ownerNav";
import type { ReservationStatus } from "../../types/api";

// Mirrors ReservationServiceImpl.VALID_BUSINESS_TRANSITIONS exactly.
const nextActions: Partial<Record<ReservationStatus, { status: ReservationStatus; label: string; danger?: boolean }[]>> = {
  PENDING: [
    { status: "CONFIRMED", label: "تایید" },
    { status: "REJECTED", label: "رد کردن", danger: true },
  ],
  CONFIRMED: [{ status: "COMPLETED", label: "تکمیل شد" }],
};

export function OwnerReservationsPage() {
  const [date, setDate] = useState("");
  const queryClient = useQueryClient();
  const { notify } = useToast();

  const { data: reservations, isLoading } = useQuery({
    queryKey: ["business", "reservations", date],
    queryFn: () => reservationApi.businessReservations(date || undefined),
  });

  const updateStatus = useMutation({
    mutationFn: ({ id, newStatus }: { id: number; newStatus: ReservationStatus }) =>
      reservationApi.updateStatus(id, newStatus),
    onSuccess: () => {
      queryClient.invalidateQueries({ queryKey: ["business", "reservations"] });
      notify("وضعیت رزرو به‌روزرسانی شد", "ok");
    },
    onError: (err) => notify(errorMessage(err), "danger"),
  });

  return (
    <DashboardShell
      navItems={ownerNavItems}
      title="رزروها"
      actions={<Input type="date" value={date} onChange={(e) => setDate(e.target.value)} />}
    >
      {isLoading ? (
        <PageSpinner />
      ) : !reservations || reservations.length === 0 ? (
        <EmptyState title="رزروی پیدا نشد" />
      ) : (
        <div className="list-group">
          {reservations.map((r) => (
            <div key={r.id} className="list-row">
              <div className="list-row-main">
                <span className="list-row-title">
                  {formatDate(r.date)} — {formatTime(r.time)}
                </span>
                <span className="list-row-sub">{r.guestCount} نفر</span>
              </div>
              <div className="list-row-actions">
                <ReservationStatusBadge status={r.status} />
                {(nextActions[r.status] ?? []).map((action) => (
                  <Button
                    key={action.status}
                    size="sm"
                    variant={action.danger ? "danger" : "secondary"}
                    loading={updateStatus.isPending}
                    onClick={() => updateStatus.mutate({ id: r.id, newStatus: action.status })}
                  >
                    {action.label}
                  </Button>
                ))}
              </div>
            </div>
          ))}
        </div>
      )}
    </DashboardShell>
  );
}
