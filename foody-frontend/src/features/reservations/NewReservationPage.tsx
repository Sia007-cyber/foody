import { useState, type FormEvent } from "react";
import { useParams, useNavigate } from "react-router-dom";
import { useQuery } from "@tanstack/react-query";
import { businessApi } from "../businesses/businessApi";
import { reservationApi } from "./reservationApi";
import { Input } from "../../components/Field";
import { Button } from "../../components/Button";
import { PageSpinner, EmptyState, ErrorState } from "../../components/Controls";
import { useToast, errorMessage } from "../../components/Feedback";

export function NewReservationPage() {
  const { id } = useParams<{ id: string }>();
  const businessId = Number(id);
  const navigate = useNavigate();
  const { notify } = useToast();

  const {
    data: business,
    isLoading,
    isError: isBusinessError,
    error: businessError,
    refetch: refetchBusiness,
  } = useQuery({
    queryKey: ["businesses", businessId],
    queryFn: () => businessApi.getById(businessId),
    enabled: Number.isFinite(businessId),
  });

  const [date, setDate] = useState("");
  const [time, setTime] = useState("");
  const [guestCount, setGuestCount] = useState(2);
  const [submitting, setSubmitting] = useState(false);
  const [error, setError] = useState<string | null>(null);

  if (isLoading) return <PageSpinner />;
  if (isBusinessError)
    return <ErrorState error={businessError} onRetry={() => refetchBusiness()} title="اطلاعات کسب‌وکار لود نشد" />;
  if (!business) return <EmptyState title="کسب‌وکار پیدا نشد" />;

  async function handleSubmit(e: FormEvent) {
    e.preventDefault();
    setError(null);
    setSubmitting(true);
    try {
      const reservation = await reservationApi.create({ businessId, date, time, guestCount });
      notify("رزرو با موفقیت ثبت شد", "ok");
      navigate(`/reservations/${reservation.id}`);
    } catch (err) {
      setError(errorMessage(err));
    } finally {
      setSubmitting(false);
    }
  }

  return (
    <div className="container" style={{ maxWidth: 420, paddingBlock: 48 }}>
      <h1 style={{ fontSize: 24, fontWeight: 800, marginBottom: 4 }}>رزرو میز</h1>
      <p style={{ color: "var(--ink-soft)", marginBottom: 24 }}>{business.name}</p>

      {error && <div className="auth-error" style={{ marginBottom: 16 }}>{error}</div>}

      <form onSubmit={handleSubmit} style={{ display: "flex", flexDirection: "column", gap: 14 }}>
        <Input label="تاریخ" type="date" required value={date} onChange={(e) => setDate(e.target.value)} />
        <Input label="ساعت" type="time" required value={time} onChange={(e) => setTime(e.target.value)} />
        <Input
          label="تعداد نفرات"
          type="number"
          min={1}
          required
          value={guestCount}
          onChange={(e) => setGuestCount(Number(e.target.value))}
        />
        <Button type="submit" block loading={submitting}>
          ثبت رزرو
        </Button>
      </form>
    </div>
  );
}
