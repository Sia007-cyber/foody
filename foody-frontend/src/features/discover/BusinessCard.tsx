import { Link } from "react-router-dom";
import type { Business } from "../../types/api";

const typeLabel: Record<string, string> = {
  CAFE: "کافه",
  FAST_FOOD: "فست‌فود",
};

const typeEmoji: Record<string, string> = {
  CAFE: "☕",
  FAST_FOOD: "🍔",
};

export function BusinessCard({ business }: { business: Business }) {
  const coverClass = `business-cover-${business.businessType.toLowerCase()}`;

  return (
    <Link to={`/businesses/${business.id}`} className="business-card">
      <div className={`business-cover ${coverClass}`}>
        <span className="business-cover-emoji" aria-hidden="true">
          {typeEmoji[business.businessType] ?? "🍽️"}
        </span>
      </div>
      <div className="business-card-body">
        <span className="business-card-name">{business.name}</span>
        <span className="business-card-meta">
          {typeLabel[business.businessType] ?? business.businessType}
          {business.address ? ` · ${business.address}` : ""}
        </span>
      </div>
    </Link>
  );
}
