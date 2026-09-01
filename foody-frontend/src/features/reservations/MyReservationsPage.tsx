import { useState } from "react";
import { useMutation, useQuery, useQueryClient } from "@tanstack/react-query";
import { reservationApi } from "./reservationApi";
import { ReservationStatusBadge } from "../../components/Badge";
import { PageSpinner, EmptyState, ErrorState } from "../../components/Controls";
import { Button } from "../../components/Button";
import { ConfirmDialog, useToast, errorMessage } from "../../components/Feedback";
import { formatDate, formatTime } from "../../lib/format";

export function MyReservationsPage() {
  const queryClient = useQueryClient();
  const { notify } = useToast();
  const [cancelTargetId, setCancelTargetId] = useState<number | null>(null);

  const {
    data: reservations,
    isLoading,
    isError,
    error,
    refetch,
  } = useQuery({
    queryKey: ["reservations", "my"],
    queryFn: reservationApi.myReservations,
  });

  const cancelMutation = useMutation({
    mutationFn: (id: number) => reservationApi.cancel(id),
    onSuccess: () => {
      queryClient.invalidateQueries({ queryKey: ["reservations", "my"] });
      notify("رزرو لغو شد", "ok");
      setCancelTargetId(null);
    },
    onError: (err) => {
      notify(errorMessage(err), "danger");
      setCancelTargetId(null);
    },
  });

  if (isLoading) return <PageSpinner />;

  return (
    <div className="container" style={{ paddingBlock: 40 }}>
      <h1 style={{ fontSize: 24, fontWeight: 800, marginBottom: 24 }}>رزروهای من</h1>

      {isError ? (
        <ErrorState error={error} onRetry={() => refetch()} title="رزروها لود نشدن" />
      ) : !reservations || reservations.length === 0 ? (
        <EmptyState title="هنوز رزروی ثبت نکردی" />
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
                {(r.status === "PENDING" || r.status === "CONFIRMED") && (
                  <Button variant="danger" size="sm" onClick={() => setCancelTargetId(r.id)}>
                    لغو
                  </Button>
                )}
              </div>
            </div>
          ))}
        </div>
      )}

      {cancelTargetId !== null && (
        <ConfirmDialog
          title="رزرو لغو بشه؟"
          confirmLabel="لغو رزرو"
          danger
          loading={cancelMutation.isPending}
          onConfirm={() => cancelMutation.mutate(cancelTargetId)}
          onCancel={() => setCancelTargetId(null)}
        />
      )}
    </div>
  );
}
