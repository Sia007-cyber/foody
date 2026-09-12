import { useQuery } from "@tanstack/react-query";
import { productApi } from "../catalog/catalogApi";
import { formatToman } from "../../lib/format";
import { Spinner } from "../../components/Controls";
import type { Menu } from "../../types/api";
import { ProductReviews } from "./ProductReviews";

export function MenuSection({ menu, canReview }: { menu: Menu; canReview: boolean }) {
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
          return (
            <div
              key={product.id}
              className={`product-row ${!product.isAvailable ? "product-row-unavailable" : ""}`}
            >
              <div className="product-row-main">
                <span className="product-row-name">{product.name}</span>
                {product.description && <span className="product-row-desc">{product.description}</span>}
                <span className="product-row-price">{formatToman(product.price)}</span>
                <ProductReviews productId={product.id} canReview={canReview} />
              </div>
              <div className="product-row-actions">
                {!product.isAvailable ? (
                  <span className="product-row-desc">ناموجود</span>
                ) : null}
              </div>
            </div>
          );
        })}
      </div>
    </div>
  );
}
