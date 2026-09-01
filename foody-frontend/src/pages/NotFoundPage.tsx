import { Link } from "react-router-dom";
import { Button } from "../components/Button";

export function NotFoundPage() {
  return (
    <div className="empty-state" style={{ minHeight: "60vh" }}>
      <p className="empty-state-title">این صفحه پیدا نشد</p>
      <Link to="/">
        <Button variant="secondary">برگشت به صفحه‌ی اصلی</Button>
      </Link>
    </div>
  );
}
