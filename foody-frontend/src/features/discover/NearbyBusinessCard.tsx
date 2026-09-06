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

export function NearbyBusinessCard({ business }: { business: Business }) {
  const coverClass = `business-cover-${business.businessType.toLowerCase()}`;

  return (
    <Link to={`/businesses/${business.id}`} className="nearby-card">
      <div className={`nearby-card-cover business-cover ${coverClass}`}>
        <span className="business-cover-emoji" aria-hidden="true">
          {typeEmoji[business.businessType] ?? "🍽️"}
        </span>
      </div>
      <div className="nearby-card-body">
        <span className="nearby-card-name">{business.name}</span>
        <span className="nearby-card-type">{typeLabel[business.businessType] ?? business.businessType}</span>
      </div>
    </Link>
  );
}
