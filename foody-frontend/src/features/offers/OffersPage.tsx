import { useMemo } from "react";
import { useLocation } from "react-router-dom";
import { useMutation, useQuery, useQueryClient } from "@tanstack/react-query";
import { Button } from "../../components/Button";
import { EmptyState, ErrorState, PageSpinner } from "../../components/Controls";
import { errorMessage, useToast } from "../../components/Feedback";
import { ClockIcon, CheckCircleIcon, MegaphoneIcon } from "../../components/icons";
import { formatDateTime } from "../../lib/format";
import { useAuth } from "../auth/AuthContext";
import type { Offer, OfferClaim } from "../../types/api";
import { customerOffersApi } from "./customerOffersApi";
import { businessApi } from "../businesses/businessApi";
import "./offers.css";

export const CUSTOMER_OFFERS_QUERY_KEY = ["offers", "claimable"] as const;
export const CUSTOMER_CLAIMS_QUERY_KEY = ["offers", "my-claims"] as const;

function claimableState(offer: Offer, now = new Date()) {
  if (offer.status === "CANCELLED") return "لغو شده";
  if (new Date(offer.startsAt) > now) return "به‌زودی";
  if (new Date(offer.expiresAt) <= now) return "منقضی شده";
  if (offer.remainingAvailability <= 0) return "تکمیل ظرفیت";
  return "قابل دریافت";
}

export function OffersPage() {
  const location = useLocation();
  const { user } = useAuth();
  const historyMode = location.pathname.endsWith("/my-claims");
  const queryClient = useQueryClient();
  const { notify } = useToast();
  const isCustomerActor = user?.role === "CUSTOMER" || user?.role === "BUSINESS_OWNER";
  const ownerBusinessQuery = useQuery({
    queryKey: ["business", "profile"],
    queryFn: businessApi.myProfile,
    enabled: user?.role === "BUSINESS_OWNER",
    retry: false,
  });
  const offersQuery = useQuery({ queryKey: CUSTOMER_OFFERS_QUERY_KEY, queryFn: customerOffersApi.getClaimableOffers });
  const claimsQuery = useQuery({ queryKey: CUSTOMER_CLAIMS_QUERY_KEY, queryFn: customerOffersApi.getMyClaims, enabled: isCustomerActor });
  const claimMutation = useMutation({
    mutationFn: customerOffersApi.claimOffer,
    onSuccess: () => {
      void queryClient.invalidateQueries({ queryKey: CUSTOMER_OFFERS_QUERY_KEY });
      void queryClient.invalidateQueries({ queryKey: CUSTOMER_CLAIMS_QUERY_KEY });
      notify("پیشنهاد برایت ثبت شد", "ok");
    },
    onError: (error) => notify(errorMessage(error), "danger"),
  });
  const claimedOfferIds = useMemo(() => new Set((claimsQuery.data ?? []).map((claim) => claim.offerId)), [claimsQuery.data]);

  return (
    <main className="offers-page container">
      <header className="offers-page-hero">
        <div className="offers-page-hero-icon"><MegaphoneIcon size={26} /></div>
        <div><span className="section-eyebrow">بازار فودی</span><h1>{historyMode ? "سابقه پیشنهادهای من" : "پیشنهادهای ویژه"}</h1><p>{historyMode ? "پیشنهادهایی که قبلاً دریافت کرده‌ای اینجا هستند." : "پیشنهادهای عمومی و فعال کسب‌وکارها را ببین و اگر مناسب توست، دریافتش کن."}</p></div>
      </header>
      {historyMode ? <ClaimHistory claims={claimsQuery.data} isLoading={claimsQuery.isLoading} isError={claimsQuery.isError} error={claimsQuery.error} onRetry={() => claimsQuery.refetch()} offers={offersQuery.data ?? []} /> : (
        offersQuery.isLoading ? <PageSpinner /> : offersQuery.isError ? <ErrorState error={offersQuery.error} onRetry={() => offersQuery.refetch()} title="پیشنهادها لود نشدند" /> : !offersQuery.data?.length ? (
          <EmptyState title="فعلاً پیشنهاد قابل دریافتی نیست" description="پیشنهادهای فعال کافه‌ها در اینجا نمایش داده می‌شوند." />
        ) : (
          <div className="customer-offer-list" aria-label="پیشنهادهای قابل دریافت">
            {offersQuery.data.map((offer) => <CustomerOfferCard key={offer.id} offer={offer} claimed={claimedOfferIds.has(offer.id)} canAct={user?.role === "CUSTOMER" || (user?.role === "BUSINESS_OWNER" && ownerBusinessQuery.data?.id != null && ownerBusinessQuery.data.id !== offer.businessId)} claiming={claimMutation.isPending && claimMutation.variables === offer.id} onClaim={() => claimMutation.mutate(offer.id)} />)}
          </div>
        )
      )}
    </main>
  );
}

function CustomerOfferCard({ offer, claimed, canAct, claiming, onClaim }: { offer: Offer; claimed: boolean; canAct: boolean; claiming: boolean; onClaim: () => void }) {
  const state = claimableState(offer);
  const canClaim = canAct && !claimed && state === "قابل دریافت" && offer.remainingAvailability > 0;
  const claimedCount = Math.max(0, Math.min(offer.capacity, offer.claimCount));
  const progress = offer.capacity > 0 ? Math.min(100, (claimedCount / offer.capacity) * 100) : 0;
  return (
    <article className="customer-offer-card">
      <div className="customer-offer-card-header"><div>{offer.businessName && <span className="customer-offer-business">{offer.businessName}</span>}<h2>{offer.title}</h2>{offer.description && <p>{offer.description}</p>}</div><span className={`customer-offer-state customer-offer-state-${claimed ? "claimed" : state === "قابل دریافت" ? "available" : "unavailable"}`}>{claimed ? "دریافت‌شده" : state}</span></div>
      <div className="customer-offer-capacity"><div className="customer-offer-capacity-label"><span>ظرفیت باقی‌مانده</span><strong>{new Intl.NumberFormat("fa-IR").format(Math.max(0, offer.remainingAvailability))} نفر</strong></div><div className="customer-offer-progress" aria-hidden="true"><span style={{ width: `${progress}%` }} /></div></div>
      <div className="customer-offer-times"><span><ClockIcon size={16} />شروع: {formatDateTime(offer.startsAt)}</span><span><ClockIcon size={16} />پایان: {formatDateTime(offer.expiresAt)}</span></div>
      <div className="customer-offer-action">{claimed ? <span className="customer-offer-claimed-note"><CheckCircleIcon size={17} />این پیشنهاد را قبلاً دریافت کرده‌ای</span> : canAct ? <Button size="sm" disabled={!canClaim || claiming} loading={claiming} onClick={onClaim}>دریافت پیشنهاد</Button> : <span className="customer-offer-claimed-note">برای دریافت، وارد حساب مشتری شوید</span>}</div>
    </article>
  );
}

function ClaimHistory({ claims, isLoading, isError, error, onRetry, offers }: { claims?: OfferClaim[]; isLoading: boolean; isError: boolean; error: unknown; onRetry: () => void; offers: Offer[] }) {
  if (isLoading) return <PageSpinner />;
  if (isError) return <ErrorState error={error} onRetry={onRetry} title="دریافت‌های شما لود نشدند" />;
  if (!claims?.length) return <EmptyState title="هنوز پیشنهادی دریافت نکرده‌ای" description="وقتی یک پیشنهاد محدود را دریافت کنی، سابقه‌اش اینجا می‌ماند." />;
  const offersById = new Map(offers.map((offer) => [offer.id, offer]));
  return <div className="customer-claims-list" aria-label="دریافت‌های من">{claims.map((claim) => { const offer = offersById.get(claim.offerId); return <article className="customer-claim-card" key={claim.id}><div>{offer?.businessName && <span className="customer-offer-business">{offer.businessName}</span>}<h2>{offer?.title ?? `پیشنهاد شماره ${claim.offerId}`}</h2><p>زمان دریافت: {formatDateTime(claim.claimedAt)} · ظرفیت باقی‌مانده: {new Intl.NumberFormat("fa-IR").format(Math.max(0, claim.remainingAvailability))}</p></div><span className="customer-offer-state customer-offer-state-claimed"><CheckCircleIcon size={15} />دریافت‌شده</span></article>; })}</div>;
}
