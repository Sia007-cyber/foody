import { useState } from "react";
import { useMutation, useQuery, useQueryClient } from "@tanstack/react-query";
import { Button } from "../../components/Button";
import { DashboardShell } from "../../components/DashboardShell";
import { EmptyState, ErrorState, PageSpinner } from "../../components/Controls";
import { errorMessage, useToast } from "../../components/Feedback";
import { formatDateTime } from "../../lib/format";
import type { AdminReview } from "../../types/api";
import { ReviewStars } from "../business-detail/ReviewEditor";
import { adminApi } from "./adminApi";
import { adminNavItems } from "./adminNav";
import "./adminReviews.css";

const statusLabels={PENDING:"در انتظار بررسی",APPROVED:"تأییدشده",REJECTED:"ردشده"} as const;

export function AdminReviewsPage(){
  const [status,setStatus]=useState<AdminReview["moderationStatus"]>("PENDING");
  const queryClient=useQueryClient();const {notify}=useToast();
  const query=useQuery({queryKey:["admin","reviews",status],queryFn:()=>adminApi.reviews(status)});
  const decide=useMutation({mutationFn:({review,decision}:{review:AdminReview;decision:"approve"|"reject"})=>decision==="approve"?adminApi.approveReview({type:review.reviewType,id:review.id}):adminApi.rejectReview({type:review.reviewType,id:review.id}),onSuccess:()=>{void queryClient.invalidateQueries({queryKey:["admin","reviews"]});void queryClient.invalidateQueries({queryKey:["reviews"]});void queryClient.invalidateQueries({queryKey:["product-reviews"]});notify("وضعیت نظر به‌روزرسانی شد","ok")},onError:error=>notify(errorMessage(error),"danger")});
  return <DashboardShell navItems={adminNavItems} title="مدیریت نظرات">
    <div className="admin-review-tabs" role="tablist" aria-label="فیلتر وضعیت نظرات">{(["PENDING","APPROVED","REJECTED"] as const).map(value=><button key={value} type="button" role="tab" aria-selected={status===value} className={status===value?"active":""} onClick={()=>setStatus(value)}>{statusLabels[value]}</button>)}</div>
    {query.isLoading?<PageSpinner/>:query.isError?<ErrorState error={query.error} onRetry={()=>query.refetch()} title="نظرات لود نشدند"/>:!query.data?.length?<EmptyState title={`نظر ${statusLabels[status]} وجود ندارد`}/>:<div className="admin-review-list">{query.data.map(review=><article className="admin-review-row" key={`${review.reviewType}-${review.id}`}><div className="admin-review-main"><div className="admin-review-heading"><strong>{review.reviewerDisplayName}</strong><ReviewStars rating={review.rating}/><span className={`admin-review-status admin-review-status-${review.moderationStatus.toLowerCase()}`}>{statusLabels[review.moderationStatus]}</span></div><p className="admin-review-context">{review.businessName}{review.reviewType==="PRODUCT"?` · محصول ${review.targetName}`:" · نظر کسب‌وکار"} · {formatDateTime(review.createdAt)}</p>{review.comment&&<p className="admin-review-comment">{review.comment}</p>}</div>{status==="PENDING"&&<div className="admin-review-actions"><Button size="sm" onClick={()=>decide.mutate({review,decision:"approve"})} disabled={decide.isPending}>تأیید</Button><Button size="sm" variant="danger" onClick={()=>decide.mutate({review,decision:"reject"})} disabled={decide.isPending}>رد</Button></div>}</article>)}</div>}
  </DashboardShell>;
}
