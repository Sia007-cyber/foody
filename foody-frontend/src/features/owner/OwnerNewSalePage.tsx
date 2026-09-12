import { useMemo, useState, type FormEvent } from "react";
import { useMutation, useQueries, useQuery } from "@tanstack/react-query";
import { DashboardShell } from "../../components/DashboardShell";
import { Button } from "../../components/Button";
import { Input } from "../../components/Field";
import { ErrorState, PageSpinner } from "../../components/Controls";
import { errorMessage, useToast } from "../../components/Feedback";
import { menuApi, productApi } from "../catalog/catalogApi";
import { formatToman } from "../../lib/format";
import { ownerNavItems } from "./ownerNav";
import { ownerWalletApi } from "./ownerWalletApi";

export function OwnerNewSalePage(){
 const [publicId,setPublicId]=useState(""); const [quantities,setQuantities]=useState<Record<number,number>>({}); const {notify}=useToast();
 const menus=useQuery({queryKey:["business","menus"],queryFn:menuApi.listMine});
 const productQueries=useQueries({queries:(menus.data??[]).map(menu=>({queryKey:["business","products",menu.id],queryFn:()=>productApi.listMine(menu.id)}))});
 const products=useMemo(()=>productQueries.flatMap(query=>query.data??[]).filter(product=>product.isAvailable),[productQueries]);
 const mutation=useMutation({mutationFn:()=>ownerWalletApi.createPurchase(publicId.trim().toUpperCase(),Object.entries(quantities).filter(([,quantity])=>quantity>0).map(([productId,quantity])=>({productId:Number(productId),quantity}))),onSuccess:()=>{setPublicId("");setQuantities({});notify("درخواست خرید برای تایید مشتری ثبت شد","ok");},onError:error=>notify(errorMessage(error),"danger")});
 const selected=products.filter(product=>(quantities[product.id]??0)>0); const total=selected.reduce((sum,product)=>sum+Number(product.price)*(quantities[product.id]??0),0);
 function submit(event:FormEvent){event.preventDefault();if(!selected.length){notify("حداقل یک محصول را انتخاب کنید","danger");return;}mutation.mutate();}
 return <DashboardShell navItems={ownerNavItems} title="ثبت خرید حضوری"><form className="owner-customer-lookup" onSubmit={submit}><Input label="شناسه فودی مشتری" dir="ltr" required value={publicId} onChange={event=>setPublicId(event.target.value)}/>
  {menus.isLoading?<PageSpinner/>:menus.isError?<ErrorState title="محصولات لود نشد" error={menus.error} onRetry={()=>menus.refetch()}/>:<div className="owner-wallet-list">{products.map(product=><div className="owner-wallet-card" key={product.id}><div><strong>{product.name}</strong><p>{formatToman(product.price)}</p></div><Input aria-label={`تعداد ${product.name}`} label="تعداد" type="number" min="0" value={quantities[product.id]??0} onChange={event=>setQuantities(current=>({...current,[product.id]:Number(event.target.value)}))}/></div>)}</div>}
  <p className="wallet-request-amount">مجموع محاسبه‌شده: {formatToman(total)}</p><Button type="submit" loading={mutation.isPending}>ارسال برای تایید مشتری</Button><p>مبلغ نهایی در سرور از قیمت فعلی محصولات محاسبه می‌شود.</p></form></DashboardShell>;
}
