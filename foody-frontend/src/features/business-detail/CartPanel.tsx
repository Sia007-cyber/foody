import { useNavigate } from "react-router-dom";
import { useCart } from "../cart/CartContext";
import { formatToman } from "../../lib/format";
import { Button } from "../../components/Button";

export function CartPanel() {
  const { lines, businessId, totalAmount, setQuantity } = useCart();
  const navigate = useNavigate();

  if (!businessId || lines.length === 0) {
    return (
      <aside className="cart-panel">
        <h3>سبد خرید</h3>
        <p style={{ color: "var(--ink-soft)", fontSize: 14 }}>هنوز چیزی اضافه نکردی.</p>
      </aside>
    );
  }

  return (
    <aside className="cart-panel">
      <h3>سبد خرید</h3>
      {lines.map((line) => (
        <div key={line.product.id} className="cart-line">
          <span>
            {line.product.name} × {line.quantity}
          </span>
          <div style={{ display: "flex", alignItems: "center", gap: 8 }}>
            <span>{formatToman(Number(line.product.price) * line.quantity)}</span>
            <button
              type="button"
              className="btn-ghost"
              style={{ border: "none", background: "none", color: "var(--ink-soft)" }}
              onClick={() => setQuantity(line.product.id, 0)}
              aria-label="حذف"
            >
              ×
            </button>
          </div>
        </div>
      ))}
      <div className="cart-total-row">
        <span>جمع کل</span>
        <span>{formatToman(totalAmount)}</span>
      </div>
      <Button block onClick={() => navigate("/checkout")}>
        ادامه‌ی سفارش
      </Button>
    </aside>
  );
}
