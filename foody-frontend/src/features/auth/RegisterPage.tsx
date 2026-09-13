import { useState, type FormEvent, type ReactNode } from "react";
import { Link, useNavigate } from "react-router-dom";
import { useAuth } from "./AuthContext";
import type { RegistrableRole } from "./authApi";
import { AuthVisual } from "./AuthVisual";
import { Input, PasswordInput } from "../../components/Field";
import { Button } from "../../components/Button";
import { errorMessage } from "../../components/Feedback";
import { UserIcon, StoreIcon } from "../../components/icons";
import "./auth.css";

const ROLE_OPTIONS: { value: RegistrableRole; title: string; subtitle: string; icon: ReactNode }[] = [
  { value: "CUSTOMER", title: "مشتری", subtitle: "سفارش غذا و رزرو میز", icon: <UserIcon size={18} /> },
  {
    value: "BUSINESS_OWNER",
    title: "کافه‌دار / رستوران‌دار",
    subtitle: "مدیریت کسب‌وکار در فودی",
    icon: <StoreIcon size={18} />,
  },
];

export const OWNER_NATIONAL_ID_STORAGE_KEY = "foody.owner-registration-national-id";

export function RegisterPage() {
  const { register } = useAuth();
  const navigate = useNavigate();
  const [role, setRole] = useState<RegistrableRole>("CUSTOMER");
  const [fullName, setFullName] = useState("");
  const [email, setEmail] = useState("");
  const [phone, setPhone] = useState("");
  const [managerNationalId, setManagerNationalId] = useState("");
  const [password, setPassword] = useState("");
  const [confirmPassword, setConfirmPassword] = useState("");
  const [error, setError] = useState<string | null>(null);
  const [loading, setLoading] = useState(false);

  async function handleSubmit(e: FormEvent) {
    e.preventDefault();
    setError(null);

    if (password !== confirmPassword) {
      setError("رمز عبور و تکرار آن یکسان نیستند");
      return;
    }

    setLoading(true);
    try {
      const trimmedNationalId = managerNationalId.trim();
      await register({
        fullName,
        email: email.trim() || undefined,
        phone: phone.trim(),
        password,
        role,
        managerNationalId: role === "BUSINESS_OWNER" ? trimmedNationalId : undefined,
      });
      if (role === "BUSINESS_OWNER") {
        sessionStorage.setItem(OWNER_NATIONAL_ID_STORAGE_KEY, trimmedNationalId);
        navigate("/business/register", { replace: true });
      } else {
        navigate("/", { replace: true });
      }
    } catch (err) {
      setError(errorMessage(err));
    } finally {
      setLoading(false);
    }
  }

  return (
    <div className="auth-page">
      <AuthVisual
        kicker="بیا شروع کنیم 🚀"
        title="فودی‌ات رو همین امروز بساز"
        subtitle="چه مشتری باشی چه صاحب کسب‌وکار، فودی برات آماده‌ست."
        showPromotionalContent={false}
      >
        <div className="auth-card auth-card-register">
          <div className="auth-heading">
            <h1>بیا شروع کنیم</h1>
            <p>یه حساب فودی بساز</p>
          </div>

          {error && <div className="auth-error">{error}</div>}

          <form className="auth-form" onSubmit={handleSubmit}>
            <div className="role-toggle" role="radiogroup" aria-label="نوع حساب">
              {ROLE_OPTIONS.map((opt) => (
                <button
                  key={opt.value}
                  type="button"
                  role="radio"
                  aria-checked={role === opt.value}
                  className={`role-option ${role === opt.value ? "is-selected" : ""}`}
                  onClick={() => {
                    setRole(opt.value);
                    if (opt.value === "CUSTOMER") setManagerNationalId("");
                  }}
                >
                  <span className="role-option-icon">{opt.icon}</span>
                  <span className="role-option-title">{opt.title}</span>
                  <span className="role-option-subtitle">{opt.subtitle}</span>
                </button>
              ))}
            </div>

            <Input
              label="نام کامل"
              required
              value={fullName}
              onChange={(e) => setFullName(e.target.value)}
            />
            <Input
              label="ایمیل (اختیاری)"
              type="email"
              dir="ltr"
              autoComplete="email"
              value={email}
              onChange={(e) => setEmail(e.target.value)}
            />
            <Input
              id="registration-phone"
              label="شماره موبایل"
              type="tel"
              dir="ltr"
              required
              pattern="09[0-9]{9}"
              maxLength={11}
              helper="مثال: 09123456789"
              helperClassName="registration-helper"
              value={phone}
              onChange={(e) => setPhone(e.target.value.trim())}
            />
            {role === "BUSINESS_OWNER" && (
              <Input
                label="کد ملی مدیر / مالک"
                dir="ltr"
                required
                pattern="[0-9]{10}"
                inputMode="numeric"
                maxLength={10}
                helper="۱۰ رقم، بدون فاصله"
                helperClassName="registration-helper"
                value={managerNationalId}
                onChange={(e) => setManagerNationalId(e.target.value.trim())}
              />
            )}
            <PasswordInput
              label="رمز عبور"
              dir="ltr"
              required
              minLength={8}
              autoComplete="new-password"
              value={password}
              onChange={(e) => setPassword(e.target.value)}
            />
            <PasswordInput
              label="تکرار رمز عبور"
              dir="ltr"
              required
              minLength={8}
              autoComplete="new-password"
              value={confirmPassword}
              onChange={(e) => setConfirmPassword(e.target.value)}
              error={
                confirmPassword.length > 0 && confirmPassword !== password
                  ? "با رمز عبور یکسان نیست"
                  : undefined
              }
            />
            <Button type="submit" block loading={loading}>
              ثبت‌نام
            </Button>
          </form>

          <p className="auth-switch">
            قبلاً ثبت‌نام کردی؟ <Link to="/login">وارد شو</Link>
          </p>
        </div>
      </AuthVisual>
    </div>
  );
}
