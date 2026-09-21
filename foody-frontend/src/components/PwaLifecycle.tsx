import { useEffect, useRef, useState } from "react";
import { registerSW } from "virtual:pwa-register";
import { ConfirmDialog } from "./Feedback";

export function PwaLifecycle() {
  const [online, setOnline] = useState(() => navigator.onLine);
  const [updateAvailable, setUpdateAvailable] = useState(false);
  const updateServiceWorker = useRef<((reloadPage?: boolean) => Promise<void>) | null>(null);

  useEffect(() => {
    updateServiceWorker.current = registerSW({
      immediate: true,
      onNeedRefresh: () => setUpdateAvailable(true),
      onRegisterError: (error) => console.error("Foody service worker registration failed", error),
    });

    const handleOnline = () => setOnline(true);
    const handleOffline = () => setOnline(false);
    window.addEventListener("online", handleOnline);
    window.addEventListener("offline", handleOffline);
    return () => {
      window.removeEventListener("online", handleOnline);
      window.removeEventListener("offline", handleOffline);
    };
  }, []);

  return (
    <>
      {!online && (
        <div className="pwa-offline-banner" role="status">
          اینترنت قطع است؛ اطلاعات تازه دریافت نمی‌شود و عملیات آنلاین انجام نخواهد شد.
        </div>
      )}
      {updateAvailable && (
        <ConfirmDialog
          title="نسخه جدید فودی آماده است"
          description="برای دریافت آخرین نسخه، صفحه دوباره بارگذاری می‌شود. اگر در حال تکمیل فرم هستی، ابتدا آن را تمام کن."
          confirmLabel="به‌روزرسانی"
          onConfirm={() => void updateServiceWorker.current?.(true)}
          onCancel={() => setUpdateAvailable(false)}
        />
      )}
    </>
  );
}
