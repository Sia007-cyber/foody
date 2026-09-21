import { createContext, useContext, useEffect, useRef, useState, type ReactNode } from "react";
import { registerSW } from "virtual:pwa-register";
import { ConfirmDialog } from "./Feedback";
import { DownloadIcon } from "./icons";

interface DeferredInstallPromptEvent extends Event {
  prompt: () => Promise<void>;
  userChoice: Promise<{ outcome: "accepted" | "dismissed" }>;
}

interface PwaInstallContextValue {
  available: boolean;
  installing: boolean;
  requestInstall: () => void;
}

const PwaInstallContext = createContext<PwaInstallContextValue | null>(null);

function isStandalone() {
  return window.matchMedia("(display-mode: standalone)").matches || (navigator as Navigator & { standalone?: boolean }).standalone === true;
}

function isIosSafari() {
  const userAgent = navigator.userAgent;
  const isAppleMobile = /iPad|iPhone|iPod/.test(userAgent) || (navigator.platform === "MacIntel" && navigator.maxTouchPoints > 1);
  const isSafari = /Safari/.test(userAgent) && !/CriOS|FxiOS|EdgiOS|OPiOS/.test(userAgent);
  return isAppleMobile && isSafari;
}

export function PwaInstallEntry({ variant = "icon", onActivate }: { variant?: "icon" | "navigation"; onActivate?: () => void }) {
  const install = useContext(PwaInstallContext);

  if (!install?.available) return null;

  return (
    <button
      type="button"
      className={`pwa-install-entry pwa-install-entry-${variant}`}
      aria-label="نصب فودی"
      title="نصب فودی"
      disabled={install.installing}
      onClick={() => {
        onActivate?.();
        install.requestInstall();
      }}
    >
      <DownloadIcon size={variant === "icon" ? 18 : 17} />
      {variant === "icon" ? <span className="visually-hidden">نصب فودی</span> : "نصب فودی"}
    </button>
  );
}

export function PwaLifecycle({ children }: { children: ReactNode }) {
  const [online, setOnline] = useState(() => navigator.onLine);
  const [updateAvailable, setUpdateAvailable] = useState(false);
  const [installed, setInstalled] = useState(isStandalone);
  const [installAvailable, setInstallAvailable] = useState(false);
  const [installing, setInstalling] = useState(false);
  const [showIosInstructions, setShowIosInstructions] = useState(false);
  const updateServiceWorker = useRef<((reloadPage?: boolean) => Promise<void>) | null>(null);
  const deferredInstallPrompt = useRef<DeferredInstallPromptEvent | null>(null);

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

  useEffect(() => {
    const displayMode = window.matchMedia("(display-mode: standalone)");
    const syncInstalledState = () => setInstalled(isStandalone());
    const handleBeforeInstallPrompt = (event: Event) => {
      event.preventDefault();
      if (isStandalone()) return;
      deferredInstallPrompt.current = event as DeferredInstallPromptEvent;
      setInstallAvailable(true);
    };
    const handleAppInstalled = () => {
      deferredInstallPrompt.current = null;
      setInstallAvailable(false);
      setInstalled(true);
    };

    window.addEventListener("beforeinstallprompt", handleBeforeInstallPrompt);
    window.addEventListener("appinstalled", handleAppInstalled);
    displayMode.addEventListener("change", syncInstalledState);
    return () => {
      window.removeEventListener("beforeinstallprompt", handleBeforeInstallPrompt);
      window.removeEventListener("appinstalled", handleAppInstalled);
      displayMode.removeEventListener("change", syncInstalledState);
    };
  }, []);

  const hasIosManualInstall = !installed && isIosSafari();
  const available = !installed && (installAvailable || hasIosManualInstall);

  const requestInstall = () => {
    if (installed) return;

    const prompt = deferredInstallPrompt.current;
    if (prompt) {
      deferredInstallPrompt.current = null;
      setInstallAvailable(false);
      setInstalling(true);
      void prompt
        .prompt()
        .then(() => prompt.userChoice)
        .then(({ outcome }) => {
          if (outcome === "dismissed") setInstallAvailable(false);
        })
        .catch(() => undefined)
        .finally(() => setInstalling(false));
      return;
    }

    if (hasIosManualInstall) setShowIosInstructions(true);
  };

  return (
    <PwaInstallContext.Provider value={{ available, installing, requestInstall }}>
      {children}
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
      {showIosInstructions && (
        <div className="modal-backdrop" onClick={() => setShowIosInstructions(false)}>
          <div className="modal-panel pwa-ios-instructions" role="dialog" aria-modal="true" aria-labelledby="pwa-ios-title" onClick={(event) => event.stopPropagation()}>
            <div style={{ display: "flex", flexDirection: "column", gap: 6 }}>
              <h3 id="pwa-ios-title" style={{ fontSize: 18, fontWeight: 700 }}>نصب فودی روی صفحهٔ اصلی</h3>
              <p>در سافاری، دکمهٔ اشتراک‌گذاری را بزن و «افزودن به صفحهٔ اصلی» را انتخاب کن.</p>
            </div>
            <button type="button" className="btn btn-primary" onClick={() => setShowIosInstructions(false)}>متوجه شدم</button>
          </div>
        </div>
      )}
    </PwaInstallContext.Provider>
  );
}
