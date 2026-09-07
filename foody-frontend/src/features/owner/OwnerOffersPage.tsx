import { useState, type FormEvent } from "react";
import { useMutation, useQuery, useQueryClient } from "@tanstack/react-query";
import { Button } from "../../components/Button";
import { EmptyState, ErrorState, PageSpinner } from "../../components/Controls";
import { DashboardShell } from "../../components/DashboardShell";
import { Input, Textarea } from "../../components/Field";
import { ConfirmDialog, errorMessage, useToast } from "../../components/Feedback";
import { ClockIcon, MegaphoneIcon, PlusIcon } from "../../components/icons";
import { formatDateTime } from "../../lib/format";
import type { CreateOfferRequest, Offer } from "../../types/api";
import { ownerNavItems } from "./ownerNav";
import { ownerOffersApi } from "./ownerOffersApi";
import "./ownerOffers.css";

export const OWNER_OFFERS_QUERY_KEY = ["business", "offers"] as const;

type OfferPresentationState = "active" | "upcoming" | "expired" | "full" | "cancelled";

export function offerPresentationState(offer: Offer, now = new Date()): OfferPresentationState {
  if (offer.status === "CANCELLED") return "cancelled";
  if (new Date(offer.startsAt) > now) return "upcoming";
  if (new Date(offer.expiresAt) <= now) return "expired";
  if (offer.remainingAvailability <= 0 || offer.claimCount >= offer.capacity) return "full";
  return "active";
}

const stateLabels: Record<OfferPresentationState, string> = {
  active: "فعال", upcoming: "در انتظار شروع", expired: "منقضی شده", full: "تکمیل ظرفیت", cancelled: "لغو شده",
};

function localDateTimeNow() {
  const now = new Date();
  now.setMinutes(now.getMinutes() - now.getTimezoneOffset());
  return now.toISOString().slice(0, 16);
}

export function OwnerOffersPage() {
  const [showForm, setShowForm] = useState(false);
  const [offerToCancel, setOfferToCancel] = useState<Offer | null>(null);
  const offersQuery = useQuery({ queryKey: OWNER_OFFERS_QUERY_KEY, queryFn: ownerOffersApi.getOffers });
  const queryClient = useQueryClient();
  const { notify } = useToast();
  const createMutation = useMutation({
    mutationFn: ownerOffersApi.createOffer,
    onSuccess: () => {
      void queryClient.invalidateQueries({ queryKey: OWNER_OFFERS_QUERY_KEY });
      setShowForm(false);
      notify("پیشنهاد جدید ثبت شد", "ok");
    },
    onError: (error) => notify(errorMessage(error), "danger"),
  });
  const cancelMutation = useMutation({
    mutationFn: ownerOffersApi.cancelOffer,
    onSuccess: () => {
      void queryClient.invalidateQueries({ queryKey: OWNER_OFFERS_QUERY_KEY });
      setOfferToCancel(null);
      notify("پیشنهاد لغو شد", "ok");
    },
    onError: (error) => notify(errorMessage(error), "danger"),
  });

  return (
    <DashboardShell
      navItems={ownerNavItems}
      title="پیشنهادهای محدود"
      actions={<Button size="sm" onClick={() => setShowForm((visible) => !visible)}><PlusIcon size={16} />{showForm ? "بستن فرم" : "پیشنهاد جدید"}</Button>}
    >
      {showForm && <OfferForm onSubmit={createMutation.mutate} loading={createMutation.isPending} />}
      {offersQuery.isLoading ? <PageSpinner /> : offersQuery.isError ? (
        <ErrorState error={offersQuery.error} onRetry={() => offersQuery.refetch()} title="پیشنهادها لود نشدند" />
      ) : !offersQuery.data?.length ? (
        <EmptyState title="هنوز پیشنهادی نساخته‌ای" description="یک پیشنهاد محدود بساز تا مشتری‌ها بتوانند ظرفیت آن را دریافت کنند." />
      ) : (
        <div className="owner-offer-list" aria-label="پیشنهادهای محدود">
          {offersQuery.data.map((offer) => <OfferCard key={offer.id} offer={offer} onCancel={() => setOfferToCancel(offer)} />)}
        </div>
      )}
      {offerToCancel && (
        <ConfirmDialog
          title="لغو پیشنهاد"
          description={`«${offerToCancel.title}» لغو می‌شود. دریافت‌های ثبت‌شده حفظ خواهند شد.`}
          confirmLabel="لغو پیشنهاد"
          danger
          loading={cancelMutation.isPending}
          onCancel={() => !cancelMutation.isPending && setOfferToCancel(null)}
          onConfirm={() => cancelMutation.mutate(offerToCancel.id)}
        />
      )}
    </DashboardShell>
  );
}

function OfferForm({ onSubmit, loading }: { onSubmit: (request: CreateOfferRequest) => void; loading: boolean }) {
  const [title, setTitle] = useState("");
  const [description, setDescription] = useState("");
  const [capacity, setCapacity] = useState("1");
  const [startsAt, setStartsAt] = useState(localDateTimeNow);
  const [expiresAt, setExpiresAt] = useState("");
  const [error, setError] = useState("");

  const submit = (event: FormEvent<HTMLFormElement>) => {
    event.preventDefault();
    const parsedCapacity = Number(capacity);
    if (!title.trim()) return setError("عنوان پیشنهاد را وارد کن.");
    if (!Number.isInteger(parsedCapacity) || parsedCapacity <= 0) return setError("ظرفیت باید بیشتر از صفر باشد.");
    if (!startsAt || !expiresAt) return setError("زمان شروع و پایان را وارد کن.");
    const start = new Date(startsAt); const expiry = new Date(expiresAt);
    if (!Number.isFinite(start.getTime()) || !Number.isFinite(expiry.getTime()) || expiry <= start) return setError("زمان پایان باید بعد از زمان شروع باشد.");
    setError("");
    onSubmit({ title: title.trim(), description: description.trim() || undefined, capacity: parsedCapacity, startsAt: start.toISOString(), expiresAt: expiry.toISOString() });
  };
  return (
    <section className="owner-offer-form card" aria-labelledby="create-offer-title">
      <div className="owner-offer-form-heading"><span className="owner-offer-form-icon"><MegaphoneIcon size={20} /></span><div><h2 id="create-offer-title">پیشنهاد محدود جدید</h2><p>ظرفیت، زمان شروع و پایان را مشخص کن.</p></div></div>
      <form onSubmit={submit} className="owner-offer-form-grid" noValidate>
        <Input id="offer-title" label="عنوان پیشنهاد" required maxLength={255} value={title} onChange={(event) => setTitle(event.target.value)} />
        <Input id="offer-capacity" label="ظرفیت" type="number" inputMode="numeric" min="1" step="1" required value={capacity} onChange={(event) => setCapacity(event.target.value)} />
        <Textarea id="offer-description" label="توضیحات (اختیاری)" maxLength={2000} value={description} onChange={(event) => setDescription(event.target.value)} className="owner-offer-form-description" />
        <div className="owner-offer-time-fields"><Input id="offer-starts-at" label="زمان شروع" type="datetime-local" required value={startsAt} onChange={(event) => setStartsAt(event.target.value)} /><Input id="offer-expires-at" label="زمان پایان" type="datetime-local" required value={expiresAt} onChange={(event) => setExpiresAt(event.target.value)} /></div>
        {error && <p className="owner-offer-form-error" role="alert">{error}</p>}
        <div className="owner-offer-form-actions"><Button type="submit" loading={loading}>ثبت پیشنهاد</Button></div>
      </form>
    </section>
  );
}

function OfferCard({ offer, onCancel }: { offer: Offer; onCancel: () => void }) {
  const state = offerPresentationState(offer);
  const remaining = Math.max(0, offer.remainingAvailability);
  const claimed = Math.min(offer.capacity, Math.max(0, offer.claimCount));
  const progress = offer.capacity ? Math.min(100, (claimed / offer.capacity) * 100) : 0;
  const mayCancel = state === "active" || state === "upcoming" || state === "full";
  return (
    <article className="owner-offer-card">
      <div className="owner-offer-card-header"><div><h2>{offer.title}</h2>{offer.description && <p>{offer.description}</p>}</div><span className={`owner-offer-status owner-offer-status-${state}`}>{stateLabels[state]}</span></div>
      <div className="owner-offer-capacity"><div className="owner-offer-capacity-label"><span>دریافت‌شده: <strong>{new Intl.NumberFormat("fa-IR").format(claimed)} از {new Intl.NumberFormat("fa-IR").format(offer.capacity)}</strong></span><span>باقی‌مانده: <strong>{new Intl.NumberFormat("fa-IR").format(remaining)}</strong></span></div><div className="owner-offer-progress" aria-label={`${claimed} از ${offer.capacity} ظرفیت دریافت شده`}><span style={{ width: `${progress}%` }} /></div></div>
      <div className="owner-offer-times"><span><ClockIcon size={16} />شروع: {formatDateTime(offer.startsAt)}</span><span><ClockIcon size={16} />پایان: {formatDateTime(offer.expiresAt)}</span></div>
      {mayCancel && <div className="owner-offer-card-actions"><Button size="sm" variant="danger" onClick={onCancel}>لغو پیشنهاد</Button></div>}
    </article>
  );
}
