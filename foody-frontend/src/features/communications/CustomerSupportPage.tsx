import { useState, type FormEvent } from "react";
import { useMutation, useQuery, useQueryClient } from "@tanstack/react-query";
import { Button } from "../../components/Button";
import { Input, Textarea } from "../../components/Field";
import { EmptyState, ErrorState, PageSpinner } from "../../components/Controls";
import { errorMessage, useToast } from "../../components/Feedback";
import { formatDateTime } from "../../lib/format";
import type { TicketDetail } from "../../types/api";
import { customerCommunicationApi } from "./communicationApi";
import "./communications.css";

export function CustomerSupportPage() {
  const [selected, setSelected] = useState<TicketDetail | null>(null);
  const [subject, setSubject] = useState("");
  const [body, setBody] = useState("");
  const [reply, setReply] = useState("");
  const qc = useQueryClient();
  const { notify } = useToast();

  const tickets = useQuery({ queryKey: ["communications", "customer", "tickets"], queryFn: customerCommunicationApi.tickets });

  const create = useMutation({
    mutationFn: () => customerCommunicationApi.createTicket(subject, body),
    onSuccess: (data) => { setSubject(""); setBody(""); setSelected(data); qc.invalidateQueries({ queryKey: ["communications"] }); notify("درخواست پشتیبانی ثبت شد", "ok"); },
    onError: (e) => notify(errorMessage(e), "danger"),
  });

  const replyMutation = useMutation({
    mutationFn: () => customerCommunicationApi.reply(selected!.ticket.id, reply),
    onSuccess: (data) => { setReply(""); setSelected(data); qc.invalidateQueries({ queryKey: ["communications"] }); notify("پاسخ ثبت شد", "ok"); },
    onError: (e) => notify(errorMessage(e), "danger"),
  });

  async function openTicket(id: number) {
    try {
      const data = await customerCommunicationApi.ticket(id);
      setSelected(data);
      qc.invalidateQueries({ queryKey: ["communications"] });
    } catch (e) {
      notify(errorMessage(e), "danger");
    }
  }

  function createSubmit(e: FormEvent) { e.preventDefault(); create.mutate(); }
  function replySubmit(e: FormEvent) { e.preventDefault(); replyMutation.mutate(); }

  return (
    <div className="container profile-page">
      <h1 className="profile-title">پشتیبانی</h1>
      <div className="communications-grid">
        <div className="communications-stack">
          <section className="communications-panel">
            <h2>درخواست جدید</h2>
            <form className="communications-form" onSubmit={createSubmit}>
              <Input label="موضوع" required maxLength={160} value={subject} onChange={(e) => setSubject(e.target.value)} />
              <Textarea label="شرح درخواست" required maxLength={4000} rows={5} value={body} onChange={(e) => setBody(e.target.value)} />
              <Button type="submit" loading={create.isPending}>ثبت درخواست</Button>
            </form>
          </section>
          <section className="communications-panel">
            <h2>درخواست‌های من</h2>
            {tickets.isLoading ? (
              <PageSpinner />
            ) : tickets.isError ? (
              <ErrorState error={tickets.error} />
            ) : !tickets.data?.items.length ? (
              <EmptyState title="درخواستی ثبت نشده" description="اگر سوالی درباره سفارش، رزرو یا حساب کاربری‌ات داری، از همین‌جا با پشتیبانی فودی در تماس باش." />
            ) : (
              <div className="communications-list">
                {tickets.data.items.map((t) => (
                  <button className={`communication-item ${selected?.ticket.id === t.id ? "is-active" : ""}`} key={t.id} onClick={() => openTicket(t.id)}>
                    <span className="communication-item-head">
                      <strong>{t.subject}</strong>
                      {t.unread && <span className="communication-unread" aria-label="پاسخ جدید" />}
                    </span>
                    <span className="communication-item-meta">{ticketStatus(t.status)} · {formatDateTime(t.updatedAt)}</span>
                  </button>
                ))}
              </div>
            )}
          </section>
        </div>
        <TicketThread detail={selected} reply={reply} setReply={setReply} submit={replySubmit} pending={replyMutation.isPending} />
      </div>
    </div>
  );
}

function TicketThread({ detail, reply, setReply, submit, pending }: { detail: TicketDetail | null; reply: string; setReply: (v: string) => void; submit: (e: FormEvent) => void; pending: boolean }) {
  if (!detail) return <section className="communications-panel"><EmptyState title="یک درخواست را انتخاب کن" /></section>;
  return (
    <section className="communications-panel">
      <h2>{detail.ticket.subject}</h2>
      <p className="communication-item-meta">{ticketStatus(detail.ticket.status)}</p>
      <div className="ticket-thread">
        {detail.messages.map((m) => (
          <article key={m.id} className={`ticket-message ${m.senderType === "ADMIN" ? "admin" : ""}`}>
            <strong>{m.senderDisplayName}</strong>
            <div>{m.body}</div>
            <small>{formatDateTime(m.createdAt)}</small>
          </article>
        ))}
      </div>
      {detail.ticket.status !== "CLOSED" ? (
        <form className="communications-form" onSubmit={submit}>
          <Textarea label="پاسخ" required maxLength={4000} value={reply} onChange={(e) => setReply(e.target.value)} />
          <Button type="submit" loading={pending}>ارسال پاسخ</Button>
        </form>
      ) : (
        <p className="communication-item-meta">این درخواست بسته شده است.</p>
      )}
    </section>
  );
}

function ticketStatus(status: TicketDetail["ticket"]["status"]) {
  return status === "OPEN" ? "باز" : status === "ANSWERED" ? "پاسخ داده‌شده" : "بسته";
}
