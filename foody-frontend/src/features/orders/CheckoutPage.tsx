import { useState, type FormEvent } from "react";
import { useNavigate } from "react-router-dom";
import { useCart } from "../cart/CartContext";
import { orderApi } from "./orderApi";
import { formatToman } from "../../lib/format";
import { Segmented } from "../../components/Controls";
import { EmptyState } from "../../components/Controls";
import { Textarea } from "../../components/Field";
import { Button } from "../../components/Button";
import { useToast, errorMessage } from "../../components/Feedback";
import type { FulfillmentType } from "../../types/api";

const fulfillmentOptions: { value: FulfillmentType; label: string }[] = [
  { value: "PICKUP", label: "دریافت حضوری" },
  { value: "DELIVERY", label: "ارسال" },
];

export function CheckoutPage() {
  const { lines, businessId, totalAmount, clear } = useCart();
  const navigate = useNavigate();
  const { notify } = useToast();
  const [fulfillmentType, setFulfillmentType] = useState<FulfillmentType>("PICKUP");
  const [address, setAddress] = useState("");
  const [submitting, setSubmitting] = useState(false);
  const [error, setError] = useState<string | null>(null);

  if (!businessId || lines.length === 0) {
    return (
      <div className="container" style={{ paddingTop: 40 }}>
        <EmptyState title="سبد خریدت خالیه" description="اول یه چیزی به سبدت اضافه کن." />
      </div>
    );
  }

  async function handleSubmit(e: FormEvent) {
    e.preventDefault();
    setError(null);
    setSubmitting(true);
    try {
      const order = await orderApi.create({
        businessId: businessId!,
        fulfillmentType,
        items: lines.map((l) => ({ productId: l.product.id, quantity: l.quantity })),
        deliveryAddress: fulfillmentType === "DELIVERY" ? address : undefined,
      });
      clear();
      notify("سفارش با موفقیت ثبت شد", "ok");
      navigate(`/orders/${order.id}`);
    } catch (err) {
      setError(errorMessage(err));
    } finally {
      setSubmitting(false);
    }
  }

  return (
    <div className="container" style={{ maxWidth: 520, paddingBlock: 48 }}>
      <h1 style={{ fontSize: 24, fontWeight: 800, marginBottom: 24 }}>تکمیل سفارش</h1>

      <div className="list-group" style={{ marginBottom: 24 }}>
        {lines.map((line) => (
          <div key={line.product.id} className="list-row">
            <div className="list-row-main">
              <span className="list-row-title">
                {line.product.name} × {line.quantity}
              </span>
            </div>
            <span>{formatToman(Number(line.product.price) * line.quantity)}</span>
          </div>
        ))}
        <div className="list-row">
          <span className="list-row-title">جمع کل</span>
          <span style={{ fontWeight: 700, color: "var(--ember-deep)" }}>{formatToman(totalAmount)}</span>
        </div>
      </div>

      {error && <div className="auth-error" style={{ marginBottom: 16 }}>{error}</div>}

      <form onSubmit={handleSubmit} style={{ display: "flex", flexDirection: "column", gap: 18 }}>
        <div>
          <p className="field-label" style={{ marginBottom: 8 }}>
            نحوه‌ی تحویل
          </p>
          <Segmented value={fulfillmentType} onChange={setFulfillmentType} options={fulfillmentOptions} />
        </div>

        {fulfillmentType === "DELIVERY" && (
          <Textarea
            label="آدرس ارسال"
            required
            value={address}
            onChange={(e) => setAddress(e.target.value)}
            placeholder="آدرس کامل رو بنویس..."
          />
        )}

        <Button type="submit" block loading={submitting}>
          ثبت سفارش
        </Button>
      </form>
    </div>
  );
}
