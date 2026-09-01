import { useState, type FormEvent } from "react";
import { Link, useNavigate } from "react-router-dom";
import { useAuth } from "./AuthContext";
import type { RegistrableRole } from "./authApi";
import { Input, PasswordInput } from "../../components/Field";
import { Button } from "../../components/Button";
import { errorMessage } from "../../components/Feedback";
import "./auth.css";

const ROLE_OPTIONS: { value: RegistrableRole; title: string; subtitle: string }[] = [
  { value: "CUSTOMER", title: "مشتری", subtitle: "سفارش غذا و رزرو میز" },
  { value: "BUSINESS_OWNER", title: "کافه‌دار / رستوران‌دار", subtitle: "مدیریت کسب‌وکار در فودی" },
];

export function RegisterPage() {
  const { register } = useAuth();
  const navigate = useNavigate();
  const [role, setRole] = useState<RegistrableRole>("CUSTOMER");
  const [fullName, setFullName] = useState("");
  const [email, setEmail] = useState("");
  const [phone, setPhone] = useState("");
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
      await register({ fullName, email, phone: phone || undefined, password, role });
      navigate(role === "BUSINESS_OWNER" ? "/business" : "/", { replace: true });
    } catch (err) {
      setError(errorMessage(err));
    } finally {
      setLoading(false);
    }
  }

  return (
    <div className="auth-page">
      <div className="auth-card">
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
                onClick={() => setRole(opt.value)}
              >
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
            label="ایمیل"
            type="email"
            dir="ltr"
            required
            autoComplete="email"
            value={email}
            onChange={(e) => setEmail(e.target.value)}
          />
          <Input
            label="شماره تماس (اختیاری)"
            type="tel"
            dir="ltr"
            value={phone}
            onChange={(e) => setPhone(e.target.value)}
          />
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
    </div>
  );
}
