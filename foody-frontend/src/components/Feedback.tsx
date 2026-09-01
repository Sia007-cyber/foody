import { createContext, useCallback, useContext, useState, type ReactNode } from "react";
import { Button } from "./Button";

interface ConfirmDialogProps {
  title: string;
  description?: string;
  confirmLabel?: string;
  danger?: boolean;
  onConfirm: () => void;
  onCancel: () => void;
  loading?: boolean;
}

export function ConfirmDialog({
  title,
  description,
  confirmLabel = "تایید",
  danger,
  onConfirm,
  onCancel,
  loading,
}: ConfirmDialogProps) {
  return (
    <div className="modal-backdrop" onClick={onCancel}>
      <div className="modal-panel" onClick={(e) => e.stopPropagation()} role="alertdialog" aria-modal="true">
        <div style={{ display: "flex", flexDirection: "column", gap: 6 }}>
          <h3 style={{ fontSize: 18, fontWeight: 700 }}>{title}</h3>
          {description && <p style={{ color: "var(--ink-soft)", fontSize: 14 }}>{description}</p>}
        </div>
        <div className="modal-actions">
          <Button variant="secondary" onClick={onCancel} disabled={loading}>
            انصراف
          </Button>
          <Button variant={danger ? "danger" : "primary"} onClick={onConfirm} loading={loading}>
            {confirmLabel}
          </Button>
        </div>
      </div>
    </div>
  );
}

// ---------- Toasts ----------

interface Toast {
  id: number;
  message: string;
  tone: "default" | "ok" | "danger";
}

interface ToastContextValue {
  notify: (message: string, tone?: Toast["tone"]) => void;
}

const ToastContext = createContext<ToastContextValue | null>(null);

export function ToastProvider({ children }: { children: ReactNode }) {
  const [toasts, setToasts] = useState<Toast[]>([]);

  const notify = useCallback((message: string, tone: Toast["tone"] = "default") => {
    const id = Date.now() + Math.random();
    setToasts((prev) => [...prev, { id, message, tone }]);
    setTimeout(() => {
      setToasts((prev) => prev.filter((t) => t.id !== id));
    }, 3500);
  }, []);

  return (
    <ToastContext.Provider value={{ notify }}>
      {children}
      <div className="toast-stack" aria-live="polite">
        {toasts.map((t) => (
          <div key={t.id} className={`toast ${t.tone === "default" ? "" : `toast-${t.tone}`}`}>
            {t.message}
          </div>
        ))}
      </div>
    </ToastContext.Provider>
  );
}

export function useToast(): ToastContextValue {
  const ctx = useContext(ToastContext);
  if (!ctx) throw new Error("useToast must be used within ToastProvider");
  return ctx;
}

/** Extracts a user-facing message from any thrown value (ApiError or otherwise). */
export function errorMessage(err: unknown): string {
  if (err instanceof Error) return err.message;
  return "خطایی رخ داد، دوباره تلاش کن.";
}
