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
