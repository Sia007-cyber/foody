import { useState, type FormEvent } from "react";
import { Link, useLocation, useNavigate } from "react-router-dom";
import { useAuth } from "./AuthContext";
import { Input, PasswordInput } from "../../components/Field";
import { Button } from "../../components/Button";
import { errorMessage } from "../../components/Feedback";
import "./auth.css";

export function LoginPage() {
  const { login } = useAuth();
  const navigate = useNavigate();
  const location = useLocation();
  const [email, setEmail] = useState("");
  const [password, setPassword] = useState("");
  const [error, setError] = useState<string | null>(null);
  const [loading, setLoading] = useState(false);

  async function handleSubmit(e: FormEvent) {
    e.preventDefault();
    setError(null);
    setLoading(true);
    try {
      const user = await login({ email, password });
      const from = (location.state as { from?: string } | null)?.from;
      if (from) {
        navigate(from, { replace: true });
      } else if (user.role === "BUSINESS_OWNER") {
        navigate("/business", { replace: true });
      } else if (user.role === "ADMIN") {
        navigate("/admin", { replace: true });
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
      <div className="auth-card">
        <div className="auth-heading">
          <h1>خوش اومدی</h1>
          <p>وارد حساب فودی‌ات شو</p>
        </div>

        {error && <div className="auth-error">{error}</div>}

        <form className="auth-form" onSubmit={handleSubmit}>
          <Input
            label="ایمیل"
            type="email"
            dir="ltr"
            required
            autoComplete="email"
            value={email}
            onChange={(e) => setEmail(e.target.value)}
          />
          <PasswordInput
            label="رمز عبور"
            dir="ltr"
            required
            autoComplete="current-password"
            value={password}
            onChange={(e) => setPassword(e.target.value)}
          />
          <Button type="submit" block loading={loading}>
            ورود
          </Button>
        </form>

        <p className="auth-switch">
          حساب نداری؟ <Link to="/register">ثبت‌نام کن</Link>
        </p>
      </div>
    </div>
  );
}
