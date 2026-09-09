import { useEffect, useState, type FormEvent } from "react";
import { Link } from "react-router-dom";
import { useMutation, useQuery, useQueryClient } from "@tanstack/react-query";
import { ApiError } from "../../lib/api";
import type { Review } from "../../types/api";
import { useAuth } from "../auth/AuthContext";
import { reviewApi, type ReviewPayload } from "./reviewApi";
import { Button } from "../../components/Button";
import { ConfirmDialog, errorMessage, useToast } from "../../components/Feedback";
import { EmptyState, ErrorState, Spinner } from "../../components/Controls";

const COMMENT_LIMIT = 2000;
const reviewsKey = (businessId: number) => ["reviews", businessId] as const;
const mineKey = (businessId: number) => ["reviews", "mine", businessId] as const;

function stars(rating: number) {
  return "★★★★★".slice(0, Math.max(0, Math.min(5, rating)));
}

function formatDate(value: string) {
  const date = new Date(value);
  return Number.isNaN(date.valueOf()) ? "" : new Intl.DateTimeFormat("fa-IR", { dateStyle: "medium" }).format(date);
}

function ReviewStars({ rating, label }: { rating: number; label?: string }) {
  return (
    <span className="review-stars" aria-label={label ?? `${rating} از ۵ ستاره`}>
      <span aria-hidden="true">{stars(rating)}<span className="review-stars-muted">{"★★★★★".slice(Math.max(0, Math.min(5, rating)))}</span></span>
      <span className="review-rating-number">{rating}/۵</span>
    </span>
  );
}

function ReviewForm({ businessId, existing, onDone }: { businessId: number; existing: Review | null; onDone: () => void }) {
  const queryClient = useQueryClient();
  const { notify } = useToast();
  const [rating, setRating] = useState(existing?.rating ?? 0);
  const [comment, setComment] = useState(existing?.comment ?? "");
  const [confirmDelete, setConfirmDelete] = useState(false);

  useEffect(() => {
    setRating(existing?.rating ?? 0);
    setComment(existing?.comment ?? "");
  }, [existing?.id, existing?.rating, existing?.comment]);

  const refreshReviews = async () => {
    await Promise.all([
      queryClient.invalidateQueries({ queryKey: reviewsKey(businessId) }),
      queryClient.invalidateQueries({ queryKey: mineKey(businessId) }),
    ]);
    onDone();
  };
  const mutation = useMutation({
    mutationFn: (payload: ReviewPayload) => existing ? reviewApi.update(businessId, payload) : reviewApi.create(businessId, payload),
    onSuccess: async () => {
      notify(existing ? "نظر شما به‌روزرسانی شد" : "نظر شما ثبت شد", "ok");
      await refreshReviews();
    },
    onError: (error) => notify(errorMessage(error), "danger"),
  });
  const deleteMutation = useMutation({
    mutationFn: () => reviewApi.remove(businessId),
    onSuccess: async () => {
      setConfirmDelete(false);
      notify("نظر شما حذف شد", "ok");
      await refreshReviews();
    },
    onError: (error) => notify(errorMessage(error), "danger"),
  });

  function submit(event: FormEvent<HTMLFormElement>) {
    event.preventDefault();
    if (!rating) {
      notify("لطفاً امتیاز خود را انتخاب کنید", "danger");
      return;
    }
    mutation.mutate({ rating, comment: comment.trim() || null });
  }

  return (
    <>
      <form className="review-form" onSubmit={submit}>
        <div className="review-form-heading">
          <h3>{existing ? "ویرایش نظر شما" : "نظر شما درباره این کسب‌وکار"}</h3>
          {existing && <ReviewStars rating={existing.rating} label={`امتیاز فعلی: ${existing.rating} از ۵`} />}
        </div>
        <fieldset className="rating-fieldset">
          <legend>امتیاز شما</legend>
          <div className="rating-options">
            {[1, 2, 3, 4, 5].map((value) => (
              <label className={`rating-option ${rating === value ? "selected" : ""}`} key={value}>
                <input type="radio" name={`review-rating-${businessId}`} value={value} checked={rating === value} onChange={() => setRating(value)} />
                <span aria-hidden="true">★</span>
                <span className="visually-hidden">{value} ستاره</span>
              </label>
            ))}
          </div>
          <span className="rating-selection" aria-live="polite">{rating ? `امتیاز انتخاب‌شده: ${rating} از ۵` : "امتیازی انتخاب نشده"}</span>
        </fieldset>
        <label className="review-comment-label" htmlFor={`review-comment-${businessId}`}>توضیح (اختیاری)</label>
        <textarea id={`review-comment-${businessId}`} className="textarea review-comment" maxLength={COMMENT_LIMIT} value={comment} onChange={(event) => setComment(event.target.value)} placeholder="تجربه‌تان را با دیگران به اشتراک بگذارید" />
        <div className="review-form-footer">
          <span className="review-character-count">{comment.length}/{COMMENT_LIMIT}</span>
          <div className="review-form-actions">
            <Button type="submit" loading={mutation.isPending}>{existing ? "ذخیره تغییرات" : "ثبت نظر"}</Button>
            {existing && <Button type="button" variant="danger" onClick={() => setConfirmDelete(true)} disabled={mutation.isPending || deleteMutation.isPending}>حذف نظر</Button>}
          </div>
        </div>
        {mutation.isError && <p className="review-form-error" role="alert">{errorMessage(mutation.error)}</p>}
      </form>
      {confirmDelete && <ConfirmDialog title="حذف نظر؟" description="این نظر برای همیشه حذف می‌شود." confirmLabel="حذف نظر" danger onCancel={() => setConfirmDelete(false)} onConfirm={() => deleteMutation.mutate()} loading={deleteMutation.isPending} />}
    </>
  );
}

export function ReviewsSection({ businessId, ownerUserId }: { businessId: number; ownerUserId: number }) {
  const { user, isLoading: authLoading } = useAuth();
  const queryClient = useQueryClient();
  const { notify } = useToast();
  const reviews = useQuery({ queryKey: reviewsKey(businessId), queryFn: () => reviewApi.list(businessId), enabled: Number.isFinite(businessId) });
  const mine = useQuery({
    queryKey: mineKey(businessId),
    queryFn: async () => {
      try { return await reviewApi.mine(businessId); }
      catch (error) { if (error instanceof ApiError && error.status === 404) return null; throw error; }
    },
    enabled: Number.isFinite(businessId) && !authLoading && (user?.role === "CUSTOMER" || (user?.role === "BUSINESS_OWNER" && user.id !== ownerUserId)),
  });
  const canWrite = user?.role === "CUSTOMER" || (user?.role === "BUSINESS_OWNER" && user.id !== ownerUserId);
  const adminDelete = useMutation({
    mutationFn: reviewApi.adminRemove,
    onSuccess: () => { void queryClient.invalidateQueries({ queryKey: reviewsKey(businessId) }); notify("نظر حذف شد", "ok"); },
    onError: (error) => notify(errorMessage(error), "danger"),
  });

  return (
    <section className="reviews-section container" aria-labelledby="reviews-title">
      <div className="reviews-section-heading">
        <div>
          <h2 id="reviews-title">نظر مشتری‌ها</h2>
          <p>تجربه واقعی مشتری‌ها را بخوانید</p>
        </div>
        {reviews.data && <div className="review-summary" aria-label={`میانگین ${reviews.data.averageRating} از ۵، ${reviews.data.reviewCount} نظر`}><ReviewStars rating={Number(reviews.data.averageRating) || 0} /><span>{reviews.data.reviewCount} نظر</span></div>}
      </div>
      {reviews.isLoading && <div className="reviews-loading"><Spinner /></div>}
      {reviews.isError && <ErrorState error={reviews.error} onRetry={() => reviews.refetch()} title="نظرات لود نشد" />}
      {reviews.data && reviews.data.reviews.length === 0 && <EmptyState title="هنوز نظری ثبت نشده" description="اولین نفری باشید که تجربه‌اش را ثبت می‌کند." />}
      {reviews.data && reviews.data.reviews.length > 0 && <div className="reviews-list">{reviews.data.reviews.map((review) => <article className="review-card" key={review.id}><div className="review-card-header"><div><h3>{review.reviewerDisplayName}</h3><time dateTime={review.createdAt}>{formatDate(review.createdAt)}</time></div><ReviewStars rating={review.rating} /></div>{review.comment && <p className="review-card-comment">{review.comment}</p>}{user?.role === "ADMIN" && <Button type="button" size="sm" variant="danger" loading={adminDelete.isPending && adminDelete.variables === review.id} onClick={() => adminDelete.mutate(review.id)}>حذف توسط مدیر</Button>}</article>)}</div>}
      {!authLoading && canWrite && mine.isError && <ErrorState error={mine.error} onRetry={() => mine.refetch()} title="نظر شما لود نشد" />}
      {!authLoading && canWrite && !mine.isLoading && !mine.isError && <ReviewForm businessId={businessId} existing={mine.data ?? null} onDone={() => mine.refetch()} />}
      {!authLoading && !user && <div className="review-login-prompt"><p>برای ثبت نظر وارد حساب مشتری خود شوید.</p><Link to="/login" className="btn btn-secondary btn-sm">ورود به حساب</Link></div>}
    </section>
  );
}
