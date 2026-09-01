import { useQuery } from "@tanstack/react-query";
import { productApi } from "../catalog/catalogApi";
import { useCart } from "../cart/CartContext";
import { formatToman } from "../../lib/format";
import { Spinner } from "../../components/Controls";
import type { Menu } from "../../types/api";

export function MenuSection({ menu, businessId }: { menu: Menu; businessId: number }) {
  const { lines, addItem, setQuantity } = useCart();
  const { data: products, isLoading } = useQuery({
    queryKey: ["products", "menu", menu.id],
    queryFn: () => productApi.listForMenu(menu.id),
  });

  if (isLoading) {
    return (
      <div className="menu-section">
        <h2>{menu.name}</h2>
        <Spinner />
      </div>
    );
  }

  if (!products || products.length === 0) return null;

  return (
    <div className="menu-section">
      <h2>{menu.name}</h2>
      <div>
        {products.map((product) => {
          const line = lines.find((l) => l.product.id === product.id);
          return (
            <div
              key={product.id}
              className={`product-row ${!product.isAvailable ? "product-row-unavailable" : ""}`}
            >
              <div className="product-row-main">
                <span className="product-row-name">{product.name}</span>
                {product.description && <span className="product-row-desc">{product.description}</span>}
                <span className="product-row-price">{formatToman(product.price)}</span>
              </div>
              <div className="product-row-actions">
                {!product.isAvailable ? (
                  <span className="product-row-desc">ناموجود</span>
                ) : line ? (
                  <div className="qty-control">
                    <button
                      type="button"
                      className="qty-btn"
                      onClick={() => setQuantity(product.id, line.quantity - 1)}
                      aria-label="کم کردن"
                    >
                      −
                    </button>
                    <span>{line.quantity}</span>
                    <button
                      type="button"
                      className="qty-btn"
                      onClick={() => setQuantity(product.id, line.quantity + 1)}
                      aria-label="زیاد کردن"
                    >
                      +
                    </button>
                  </div>
                ) : (
                  <button
                    type="button"
                    className="btn btn-secondary btn-sm"
                    onClick={() => addItem(businessId, product)}
                  >
                    افزودن
                  </button>
                )}
              </div>
            </div>
          );
        })}
      </div>
    </div>
  );
}
