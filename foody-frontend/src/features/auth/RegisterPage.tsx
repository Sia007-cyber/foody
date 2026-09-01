import { useState, type FormEvent } from "react";
import { Link, useNavigate } from "react-router-dom";
import { useAuth } from "./AuthContext";
import { Input } from "../../components/Field";
import { Button } from "../../components/Button";
import { errorMessage } from "../../components/Feedback";
import "./auth.css";

export function RegisterPage() {
  const { register } = useAuth();
  const navigate = useNavigate();
  const [fullName, setFullName] = useState("");
  const [email, setEmail] = useState("");
  const [phone, setPhone] = useState("");
  const [password, setPassword] = useState("");
  const [error, setError] = useState<string | null>(null);
  const [loading, setLoading] = useState(false);

  async function handleSubmit(e: FormEvent) {
    e.preventDefault();
    setError(null);
    setLoading(true);
    try {
      await register({ fullName, email, phone: phone || undefined, password });
      navigate("/", { replace: true });
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
          <p>یه حساب مشتری فودی بساز</p>
        </div>

        {error && <div className="auth-error">{error}</div>}

        <form className="auth-form" onSubmit={handleSubmit}>
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
          <Input
            label="رمز عبور"
            type="password"
            dir="ltr"
            required
            minLength={8}
            autoComplete="new-password"
            value={password}
            onChange={(e) => setPassword(e.target.value)}
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
