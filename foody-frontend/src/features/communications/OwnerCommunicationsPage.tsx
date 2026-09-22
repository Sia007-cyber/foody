import { useState, type FormEvent } from "react";
import { useMutation, useQuery, useQueryClient } from "@tanstack/react-query";
import { DashboardShell } from "../../components/DashboardShell";
import { Button } from "../../components/Button";
import { Input, Textarea } from "../../components/Field";
import { EmptyState, ErrorState, PageSpinner, Segmented } from "../../components/Controls";
import { errorMessage, useToast } from "../../components/Feedback";
import { formatDateTime } from "../../lib/format";
import type { BusinessMessage, TicketDetail } from "../../types/api";
import { ownerNavItems } from "../owner/ownerNav";
import { ownerCommunicationApi } from "./communicationApi";
import "./communications.css";

type Tab = "tickets" | "messages";
export function OwnerCommunicationsPage() {
  const [tab,setTab]=useState<Tab>("messages"); const [selectedTicket,setSelectedTicket]=useState<TicketDetail|null>(null); const [selectedMessage,setSelectedMessage]=useState<BusinessMessage|null>(null); const [subject,setSubject]=useState(""); const [body,setBody]=useState(""); const [reply,setReply]=useState("");
  const qc=useQueryClient(); const {notify}=useToast();
  const tickets=useQuery({queryKey:["communications","owner","tickets"],queryFn:ownerCommunicationApi.tickets});
  const messages=useQuery({queryKey:["communications","owner","messages"],queryFn:ownerCommunicationApi.messages});
  const create=useMutation({mutationFn:()=>ownerCommunicationApi.createTicket(subject,body),onSuccess:(data)=>{setSubject("");setBody("");setSelectedTicket(data);qc.invalidateQueries({queryKey:["communications"]});notify("درخواست پشتیبانی ثبت شد","ok");},onError:e=>notify(errorMessage(e),"danger")});
  const replyMutation=useMutation({mutationFn:()=>ownerCommunicationApi.reply(selectedTicket!.ticket.id,reply),onSuccess:data=>{setReply("");setSelectedTicket(data);qc.invalidateQueries({queryKey:["communications"]});notify("پاسخ ثبت شد","ok");},onError:e=>notify(errorMessage(e),"danger")});
  async function openTicket(id:number){try{const data=await ownerCommunicationApi.ticket(id);setSelectedTicket(data);qc.invalidateQueries({queryKey:["communications"]});}catch(e){notify(errorMessage(e),"danger");}}
  async function openMessage(id:number){try{const data=await ownerCommunicationApi.message(id);setSelectedMessage(data);qc.invalidateQueries({queryKey:["communications"]});}catch(e){notify(errorMessage(e),"danger");}}
  function createSubmit(e:FormEvent){e.preventDefault();create.mutate();} function replySubmit(e:FormEvent){e.preventDefault();replyMutation.mutate();}
  return <DashboardShell navItems={ownerNavItems} title="پیام‌ها و پشتیبانی">
    <Segmented value={tab} onChange={setTab} options={[{value:"messages",label:"پیام‌های مدیریت"},{value:"tickets",label:"پشتیبانی"}]}/>
    {tab==="messages"?<div className="communications-grid">
      <section className="communications-panel"><h2>صندوق پیام</h2>{messages.isLoading?<PageSpinner/>:messages.isError?<ErrorState error={messages.error}/>:!messages.data?.items.length?<EmptyState title="پیامی نداری"/>:<div className="communications-list">{messages.data.items.map(m=><button className={`communication-item ${selectedMessage?.id===m.id?"is-active":""}`} key={m.id} onClick={()=>openMessage(m.id)}><span className="communication-item-head"><strong>{m.subject}</strong>{!m.read&&<span className="communication-unread" aria-label="خوانده‌نشده"/>}</span><span className="communication-item-meta">{m.type==="BROADCAST"?"پیام عمومی مدیریت":"پیام مستقیم"} · {formatDateTime(m.createdAt)}</span></button>)}</div>}</section>
      <section className="communications-panel">{selectedMessage?<><h2>{selectedMessage.subject}</h2><p className="communication-item-meta">{selectedMessage.type==="BROADCAST"?"ارسال برای همه کسب‌وکارها":"ارسال مستقیم"} · {formatDateTime(selectedMessage.createdAt)}</p><p className="communication-body">{selectedMessage.body}</p></>:<EmptyState title="یک پیام را انتخاب کن"/>}</section>
    </div>:<div className="communications-grid">
      <div className="communications-stack"><section className="communications-panel"><h2>درخواست جدید</h2><form className="communications-form" onSubmit={createSubmit}><Input label="موضوع" required maxLength={160} value={subject} onChange={e=>setSubject(e.target.value)}/><Textarea label="شرح درخواست" required maxLength={4000} rows={5} value={body} onChange={e=>setBody(e.target.value)}/><Button type="submit" loading={create.isPending}>ثبت درخواست</Button></form></section>
      <section className="communications-panel"><h2>درخواست‌های من</h2>{tickets.isLoading?<PageSpinner/>:tickets.isError?<ErrorState error={tickets.error}/>:!tickets.data?.items.length?<EmptyState title="درخواستی ثبت نشده"/>:<div className="communications-list">{tickets.data.items.map(t=><button className={`communication-item ${selectedTicket?.ticket.id===t.id?"is-active":""}`} key={t.id} onClick={()=>openTicket(t.id)}><span className="communication-item-head"><strong>{t.subject}</strong>{t.unread&&<span className="communication-unread" aria-label="پاسخ جدید"/>}</span><span className="communication-item-meta">{ticketStatus(t.status)} · {formatDateTime(t.updatedAt)}</span></button>)}</div>}</section></div>
      <TicketThread detail={selectedTicket} reply={reply} setReply={setReply} submit={replySubmit} pending={replyMutation.isPending}/>
    </div>}
  </DashboardShell>;
}
function TicketThread({detail,reply,setReply,submit,pending}:{detail:TicketDetail|null;reply:string;setReply:(v:string)=>void;submit:(e:FormEvent)=>void;pending:boolean}){if(!detail)return <section className="communications-panel"><EmptyState title="یک درخواست را انتخاب کن"/></section>;return <section className="communications-panel"><h2>{detail.ticket.subject}</h2><p className="communication-item-meta">{ticketStatus(detail.ticket.status)}</p><div className="ticket-thread">{detail.messages.map(m=><article key={m.id} className={`ticket-message ${m.senderType==="ADMIN"?"admin":""}`}><strong>{m.senderDisplayName}</strong><div>{m.body}</div><small>{formatDateTime(m.createdAt)}</small></article>)}</div>{detail.ticket.status!=="CLOSED"?<form className="communications-form" onSubmit={submit}><Textarea label="پاسخ" required maxLength={4000} value={reply} onChange={e=>setReply(e.target.value)}/><Button type="submit" loading={pending}>ارسال پاسخ</Button></form>:<p className="communication-item-meta">این درخواست بسته شده است.</p>}</section>}
function ticketStatus(status:TicketDetail["ticket"]["status"]){return status==="OPEN"?"باز":status==="ANSWERED"?"پاسخ داده‌شده":"بسته";}
