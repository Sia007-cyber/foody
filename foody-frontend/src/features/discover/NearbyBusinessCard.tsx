import { Link } from "react-router-dom";
import type { Business } from "../../types/api";
import { StarIcon } from "../../components/icons";

// امتیاز/فاصله/درصد تخفیف هنوز توی مدل Business نیستن (بک‌اند فاز ۲ و ۳).
// اینجا فقط برای نمایش موکاپ، از id کسب‌وکار یه مقدار ثابت (نه رندوم هر
// بار) می‌سازیم تا حداقل بین رفرش‌ها یکسان بمونه. با اضافه‌شدن reviews و
// discount واقعی، این تابع‌ها حذف و مقادیر واقعی از API جایگزین می‌شن.
function mockRating(id: number): string {
  const r = 4.4 + ((id * 7) % 6) / 10;
  return r.toFixed(1);
}
function mockDistanceKm(id: number): string {
  const d = 0.8 + ((id * 13) % 20) / 10;
  return d.toFixed(1);
}
function mockDiscountPercent(id: number): number {
  const options = [15, 20, 25, 30];
  return options[id % options.length];
}

const typeLabel: Record<string, string> = {
  CAFE: "کافه",
  FAST_FOOD: "فست‌فود",
};

export function NearbyBusinessCard({ business }: { business: Business }) {
  const coverClass = `business-cover-${business.businessType.toLowerCase()}`;

  return (
    <Link to={`/businesses/${business.id}`} className="nearby-card">
      <div className={`nearby-card-cover business-cover ${coverClass}`}>
        <span className="nearby-card-discount">٪{mockDiscountPercent(business.id)} تخفیف</span>
      </div>
      <div className="nearby-card-body">
        <span className="nearby-card-name">{business.name}</span>
        <span className="nearby-card-meta">
          <span className="nearby-card-distance">{mockDistanceKm(business.id)} کیلومتر</span>
          <span className="nearby-card-rating">
            <StarIcon size={13} />
            {mockRating(business.id)}
          </span>
        </span>
        <span className="nearby-card-type">{typeLabel[business.businessType] ?? business.businessType}</span>
      </div>
    </Link>
  );
}
