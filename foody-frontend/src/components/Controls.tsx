import type { ReactNode } from "react";

interface SegmentedOption<T extends string> {
  value: T;
  label: string;
}

interface SegmentedProps<T extends string> {
  options: SegmentedOption<T>[];
  value: T;
  onChange: (value: T) => void;
}

export function Segmented<T extends string>({ options, value, onChange }: SegmentedProps<T>) {
  return (
    <div className="segmented" role="tablist">
      {options.map((opt) => (
        <button
          key={opt.value}
          type="button"
          role="tab"
          className="segmented-option"
          aria-pressed={opt.value === value}
          onClick={() => onChange(opt.value)}
        >
          {opt.label}
        </button>
      ))}
    </div>
  );
}

export function Spinner() {
  return <span className="spinner" role="status" aria-label="در حال بارگذاری" />;
}

export function PageSpinner() {
  return (
    <div className="spinner-page">
      <Spinner />
    </div>
  );
}

export function EmptyState({ title, description }: { title: string; description?: string }) {
  return (
    <div className="empty-state">
      <p className="empty-state-title">{title}</p>
      {description && <p>{description}</p>}
    </div>
  );
}

/** Generic white card used to group dashboard content, with an optional header + link-out. */
export function Panel({
  title,
  icon,
  action,
  className,
  children,
}: {
  title?: string;
  icon?: ReactNode;
  action?: ReactNode;
  className?: string;
  children: ReactNode;
}) {
  return (
    <section className={`panel ${className ?? ""}`}>
      {title && (
        <div className="panel-header">
          <h2 className="panel-title">
            {icon}
            {title}
          </h2>
          {action}
        </div>
      )}
      {children}
    </section>
  );
}

/**
 * Placeholder for a feature that isn't built yet. Keeps the intended layout/UX visible
 * to stakeholders while making it unambiguous that the section isn't wired to real data.
 */
export function ComingSoonPanel({
  title,
  icon,
  note = "این بخش به‌زودی اضافه خواهد شد",
}: {
  title?: string;
  icon?: ReactNode;
  note?: string;
}) {
  return (
    <Panel title={title} icon={icon}>
      <div className="coming-soon">
        <span className="coming-soon-badge">به‌زودی</span>
        <p>{note}</p>
      </div>
    </Panel>
  );
}
