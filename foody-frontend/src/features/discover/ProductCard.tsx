import { Link } from "react-router-dom";
import type { DiscoveryProduct } from "../../types/api";
import { resolveMediaUrl } from "../../lib/api";
import { formatToman } from "../../lib/format";

export function ProductCard({ product }: { product: DiscoveryProduct }) {
  return (
    <Link to={`/businesses/${product.businessId}`} className="product-discovery-card">
      <div className="product-discovery-image">
        {product.imageUrl ? (
          <img src={resolveMediaUrl(product.imageUrl) ?? undefined} alt={product.name} />
        ) : (
          <span aria-hidden="true">🍽️</span>
        )}
      </div>
      <div className="product-discovery-body">
        <strong>{product.name}</strong>
        <span className="product-discovery-business">{product.businessName}</span>
        <span className="product-discovery-meta">
          {product.averageRating != null && <span>★ {product.averageRating.toFixed(1)}</span>}
          <span>{formatToman(Number(product.price))}</span>
        </span>
      </div>
    </Link>
  );
}
