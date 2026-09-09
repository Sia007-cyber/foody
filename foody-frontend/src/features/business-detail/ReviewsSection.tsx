import { useState } from "react";
import { Link } from "react-router-dom";
import { useMutation, useQuery, useQueryClient } from "@tanstack/react-query";
import { ApiError } from "../../lib/api";
import { useAuth } from "../auth/AuthContext";
import { reviewApi } from "./reviewApi";
import { EmptyState, ErrorState, Spinner } from "../../components/Controls";
import { errorMessage, useToast } from "../../components/Feedback";
import { ReviewCard, ReviewEditor, ReviewStars } from "./ReviewEditor";

const reviewsKey=(id:number)=>["reviews",id] as const;
const mineKey=(id:number)=>["reviews","mine",id] as const;

export function ReviewsSection({businessId,ownerUserId}:{businessId:number;ownerUserId:number}) {
  const {user,isLoading:authLoading}=useAuth();
  const queryClient=useQueryClient();
  const {notify}=useToast();
  const [editing,setEditing]=useState(false);
  const reviews=useQuery({queryKey:reviewsKey(businessId),queryFn:()=>reviewApi.list(businessId)});
  const canWrite=user?.role==="CUSTOMER"||(user?.role==="BUSINESS_OWNER"&&user.id!==ownerUserId);
  const mine=useQuery({queryKey:mineKey(businessId),queryFn:async()=>{try{return await reviewApi.mine(businessId)}catch(error){if(error instanceof ApiError&&error.status===404)return null;throw error}},enabled:!authLoading&&canWrite});
  const refresh=async()=>{await Promise.all([queryClient.invalidateQueries({queryKey:reviewsKey(businessId)}),queryClient.invalidateQueries({queryKey:mineKey(businessId)})]);setEditing(false)};
  const save=useMutation({mutationFn:({rating,comment}:{rating:number;comment:string|null})=>mine.data?reviewApi.update(businessId,{rating,comment}):reviewApi.create(businessId,{rating,comment}),onSuccess:refresh,onError:error=>notify(errorMessage(error),"danger")});
  const remove=useMutation({mutationFn:()=>reviewApi.remove(businessId),onSuccess:refresh,onError:error=>notify(errorMessage(error),"danger")});
  const adminDelete=useMutation({mutationFn:reviewApi.adminRemove,onSuccess:()=>queryClient.invalidateQueries({queryKey:reviewsKey(businessId)})});
  return <section className="reviews-section container" aria-labelledby="reviews-title">
    <div className="reviews-section-heading"><div><h2 id="reviews-title">نظر مشتری‌ها</h2><p>تجربه واقعی مشتری‌ها را بخوانید</p></div>{reviews.data&&<div className="review-summary"><ReviewStars rating={Number(reviews.data.averageRating)||0}/><span>{reviews.data.reviewCount} نظر</span></div>}</div>
    {reviews.isLoading&&<div className="reviews-loading"><Spinner/></div>}
    {reviews.isError&&<ErrorState error={reviews.error} onRetry={()=>reviews.refetch()} title="نظرات لود نشد"/>}
    {reviews.data?.reviews.length===0&&<EmptyState title="هنوز نظری ثبت نشده"/>}
    <div className="reviews-list">{mine.data&&<ReviewCard review={mine.data} mine onEdit={()=>setEditing(true)} onDelete={()=>remove.mutate()} deleting={remove.isPending}/>} {reviews.data?.reviews.filter(review=>review.id!==mine.data?.id).map(review=><div key={review.id}><ReviewCard review={review}/>{user?.role==="ADMIN"&&<button type="button" className="btn btn-danger btn-sm" onClick={()=>adminDelete.mutate(review.id)}>حذف توسط مدیر</button>}</div>)}</div>
    {canWrite&&!mine.isLoading&&!mine.data&&!editing&&<button type="button" className="btn btn-secondary btn-sm review-write-button" onClick={()=>setEditing(true)}>ثبت نظر</button>}
    {canWrite&&editing&&<ReviewEditor initial={mine.data} title={mine.data?"ویرایش نظر شما":"نظر شما درباره این کسب‌وکار"} saving={save.isPending} onSave={(rating,comment)=>save.mutate({rating,comment})} onCancel={()=>setEditing(false)}/>}
    {!authLoading&&!user&&<div className="review-login-prompt"><p>برای ثبت نظر وارد شوید.</p><Link to="/login" className="btn btn-secondary btn-sm">ورود</Link></div>}
  </section>;
}
