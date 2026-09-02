import { useState, type FormEvent } from "react";
import { Navigate, useNavigate } from "react-router-dom";
import { useQuery } from "@tanstack/react-query";
import { businessApi, type CreateBusinessPayload } from "../businesses/businessApi";
import { Input, Select, Textarea } from "../../components/Field";
import { Button } from "../../components/Button";
import { errorMessage } from "../../components/Feedback";
import { PageSpinner } from "../../components/Controls";
import "../auth/auth.css";

const BUSINESS_TYPE_OPTIONS: { value: string; label: string }[] = [
  { value: "CAFE", label: "کافه" },
  { value: "FAST_FOOD", label: "فست‌فود" },
];

/**
 * Owner-onboarding step shown right after a BUSINESS_OWNER's first login, before the
 * dashboard is reachable (see App.tsx / RequireOwnerBusiness). Creates the one business
 * row this owner will manage via POST /api/business, then sends them to /business.
 */
export function OwnerRegisterBusinessPage() {
  const navigate = useNavigate();
  const [name, setName] = useState("");
  const [businessType, setBusinessType] = useState("CAFE");
  const [address, setAddress] = useState("");
  const [phone, setPhone] = useState("");
  const [description, setDescription] = useState("");
  const [error, setError] = useState<string | null>(null);
  const [loading, setLoading] = useState(false);

  // Already-registered owners shouldn't land back on this form (e.g. via direct URL
  // or browser back) — bounce them straight to the dashboard.
  const { data: existingBusiness, isLoading: checkingExisting } = useQuery({
    queryKey: ["business", "profile"],
    queryFn: businessApi.myProfile,
    retry: false,
  });

  if (checkingExisting) return <PageSpinner />;
  if (existingBusiness) return <Navigate to="/business" replace />;

  async function handleSubmit(e: FormEvent) {
    e.preventDefault();
    setError(null);
    setLoading(true);
    try {
      const payload: CreateBusinessPayload = {
        name,
        businessType,
        address: address || undefined,
        phone: phone || undefined,
        description: description || undefined,
      };
      await businessApi.createMyBusiness(payload);
      navigate("/business", { replace: true });
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
          <h1>کافه یا فست‌فودت رو ثبت کن</h1>
          <p>یه قدم تا داشبورد کسب‌وکارت فاصله داری</p>
        </div>

        {error && <div className="auth-error">{error}</div>}

        <form className="auth-form" onSubmit={handleSubmit}>
          <Input label="نام کسب‌وکار" required value={name} onChange={(e) => setName(e.target.value)} />

          <Select
            label="نوع کسب‌وکار"
            required
            value={businessType}
            onChange={(e) => setBusinessType(e.target.value)}
          >
            {BUSINESS_TYPE_OPTIONS.map((opt) => (
              <option key={opt.value} value={opt.value}>
                {opt.label}
              </option>
            ))}
          </Select>

          <Input label="آدرس (اختیاری)" value={address} onChange={(e) => setAddress(e.target.value)} />
          <Input
            label="شماره تماس (اختیاری)"
            type="tel"
            dir="ltr"
            value={phone}
            onChange={(e) => setPhone(e.target.value)}
          />
          <Textarea
            label="توضیحات (اختیاری)"
            rows={3}
            value={description}
            onChange={(e) => setDescription(e.target.value)}
          />

          <Button type="submit" block loading={loading}>
            ثبت کسب‌وکار و ورود به داشبورد
          </Button>
        </form>

        <p className="auth-switch">پس از ثبت، کسب‌وکارت در وضعیت «در انتظار تایید» قرار می‌گیره.</p>
      </div>
    </div>
  );
}
