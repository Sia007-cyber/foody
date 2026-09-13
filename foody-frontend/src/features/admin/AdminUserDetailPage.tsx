import { useState } from "react";
import { useMutation, useQuery } from "@tanstack/react-query";
import { useNavigate, useParams } from "react-router-dom";
import { DashboardShell } from "../../components/DashboardShell";
import { Button } from "../../components/Button";
import { PasswordInput } from "../../components/Field";
import { ConfirmDialog, errorMessage, useToast } from "../../components/Feedback";
import { ErrorState, PageSpinner } from "../../components/Controls";
import { UserStatusBadge } from "../../components/Badge";
import { useAuth } from "../auth/AuthContext";
import { impersonate } from "../auth/authSession";
import { adminApi } from "./adminApi";
import { adminNavItems } from "./adminNav";

const roleLabels = { CUSTOMER: "مشتری", BUSINESS_OWNER: "صاحب کسب‌وکار", ADMIN: "مدیر" } as const;
const value = (input: string | number | null | undefined) => input === null || input === undefined || input === "" ? "—" : String(input);
const date = (input: string) => new Intl.DateTimeFormat("fa-IR", { dateStyle: "medium", timeStyle: "short" }).format(new Date(input));

export function AdminUserDetailPage() {
  const id = Number(useParams().id);
  const navigate = useNavigate();
  const { user: admin } = useAuth();
  const { notify } = useToast();
  const [confirmImpersonation, setConfirmImpersonation] = useState(false);
  const [newPassword, setNewPassword] = useState("");
  const [confirmReset, setConfirmReset] = useState(false);
  const query = useQuery({ queryKey: ["admin", "users", id], queryFn: () => adminApi.user(id), enabled: Number.isFinite(id) });
  const detail = query.data;
  const target = detail?.user;

  const impersonationMutation = useMutation({
    mutationFn: async () => impersonate(await adminApi.impersonateUser(id), admin!),
    onSuccess: (user) => navigate(user.role === "BUSINESS_OWNER" ? "/business" : "/"),
    onError: (error) => notify(errorMessage(error), "danger"),
  });
  const resetMutation = useMutation({
    mutationFn: () => adminApi.resetPassword({ id, newPassword }),
    onSuccess: () => { setNewPassword(""); setConfirmReset(false); notify("رمز عبور جایگزین شد و نشست‌های فعال کاربر لغو شدند", "ok"); },
    onError: (error) => notify(errorMessage(error), "danger"),
  });

  if (query.isLoading) return <DashboardShell navItems={adminNavItems} title="جزئیات کاربر"><PageSpinner /></DashboardShell>;
  if (query.isError || !target) return <DashboardShell navItems={adminNavItems} title="جزئیات کاربر"><ErrorState error={query.error} onRetry={() => query.refetch()} /></DashboardShell>;
  const eligible = target.role === "CUSTOMER" || target.role === "BUSINESS_OWNER";

  return (
    <DashboardShell navItems={adminNavItems} title="جزئیات و پشتیبانی کاربر" actions={<Button variant="secondary" onClick={() => navigate("/admin/users")}>بازگشت</Button>}>
      <div className="admin-user-detail-grid">
        <section className="support-card admin-user-profile-card">
          <div className="support-card-heading">
            <div><h2>{target.fullName}</h2><p>{roleLabels[target.role]}</p></div>
            <UserStatusBadge status={target.status} />
          </div>
          <dl className="account-detail-list">
            <div><dt>شناسه داخلی</dt><dd>{target.id}</dd></div>
            <div><dt>شناسه عمومی</dt><dd dir="ltr">{value(target.publicId)}</dd></div>
            <div><dt>ایمیل</dt><dd dir="ltr">{value(target.email)}</dd></div>
            <div><dt>تلفن</dt><dd dir="ltr">{value(target.phone)}</dd></div>
            <div><dt>نشانی</dt><dd>{value(target.address)}</dd></div>
            <div><dt>مختصات</dt><dd dir="ltr">{target.latitude !== null ? `${target.latitude}, ${target.longitude}` : "—"}</dd></div>
            <div><dt>تاریخ ایجاد</dt><dd>{date(target.createdAt)}</dd></div>
            <div><dt>آخرین به‌روزرسانی</dt><dd>{date(target.updatedAt)}</dd></div>
          </dl>
          {detail.ownedBusiness && <div className="owned-business-panel">
            <h3>کسب‌وکار تحت مالکیت</h3>
            <p>{detail.ownedBusiness.name} · {detail.ownedBusiness.status}</p>
            <p>شناسه ملی مدیر: <span dir="ltr">{detail.ownedBusiness.managerNationalId}</span></p>
          </div>}
        </section>

        <div className="support-actions-stack">
          <section className="support-card">
            <h2>ورود پشتیبانی</h2>
            <p>بدون دانستن رمز عبور، یک نشست محدود و قابل ممیزی برای این حساب باز کنید.</p>
            {eligible ? <Button disabled={target.status !== "ACTIVE"} onClick={() => setConfirmImpersonation(true)}>ورود به‌جای کاربر</Button>
              : <p className="security-note">حساب مدیر قابل جعل هویت نیست.</p>}
          </section>
          {eligible && <section className="support-card">
            <h2>بازنشانی رمز عبور</h2>
            <p>رمز جایگزین باید حداقل ۸ نویسه باشد. همه نشست‌های تمدید فعال کاربر لغو می‌شوند.</p>
            <PasswordInput id="admin-reset-password" label="رمز عبور جدید" value={newPassword} minLength={8} maxLength={128} autoComplete="new-password" onChange={(event) => setNewPassword(event.target.value)} />
            <Button variant="danger" disabled={newPassword.length < 8} onClick={() => setConfirmReset(true)}>بازنشانی رمز عبور</Button>
          </section>}
          <aside className="security-note">رمزهای عبور فعلی قابل مشاهده نیستند. مدیران می‌توانند رمز را بازنشانی کنند یا برای پشتیبانی وارد حساب شوند. هیچ رمز یا هش رمزی نمایش داده نمی‌شود.</aside>
        </div>
      </div>
      {confirmImpersonation && <ConfirmDialog title="ورود به‌جای کاربر؟" description={`یک نشست پشتیبانی قابل ممیزی برای ${target.fullName} ایجاد می‌شود.`} confirmLabel="شروع ورود پشتیبانی" loading={impersonationMutation.isPending} onCancel={() => setConfirmImpersonation(false)} onConfirm={() => impersonationMutation.mutate()} />}
      {confirmReset && <ConfirmDialog title="بازنشانی رمز عبور؟" description="رمز قبلی دیگر کار نخواهد کرد و همه نشست‌های تمدید فعال این کاربر لغو می‌شوند." confirmLabel="تایید بازنشانی" danger loading={resetMutation.isPending} onCancel={() => setConfirmReset(false)} onConfirm={() => resetMutation.mutate()} />}
    </DashboardShell>
  );
}
