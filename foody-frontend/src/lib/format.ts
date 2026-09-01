/** Backend stores amounts in Toman (BigDecimal); render as a grouped Latin-numeral integer + "تومان". */
export function formatToman(value: string | number): string {
  const num = typeof value === "string" ? Number(value) : value;
  if (!Number.isFinite(num)) return "—";
  return `${new Intl.NumberFormat("en-US").format(num)} تومان`;
}

export function formatDate(iso: string): string {
  return new Date(iso).toLocaleDateString("fa-IR-u-nu-latn", {
    year: "numeric",
    month: "long",
    day: "numeric",
  });
}

export function formatDateTime(iso: string): string {
  return new Date(iso).toLocaleString("fa-IR-u-nu-latn", {
    year: "numeric",
    month: "long",
    day: "numeric",
    hour: "2-digit",
    minute: "2-digit",
  });
}

/** Formats a "HH:mm:ss" or "HH:mm" time string as "HH:mm". */
export function formatTime(time: string): string {
  return time.slice(0, 5);
}

const RELATIVE_UNITS: [Intl.RelativeTimeFormatUnit, number][] = [
  ["year", 60 * 60 * 24 * 365],
  ["month", 60 * 60 * 24 * 30],
  ["week", 60 * 60 * 24 * 7],
  ["day", 60 * 60 * 24],
  ["hour", 60 * 60],
  ["minute", 60],
];

const relativeFormatter = new Intl.RelativeTimeFormat("fa-IR-u-nu-latn", { numeric: "auto" });

/** Formats an ISO timestamp as a Persian relative time, e.g. "۵ دقیقه پیش". */
export function formatRelativeTime(iso: string): string {
  const diffSeconds = Math.round((new Date(iso).getTime() - Date.now()) / 1000);
  const abs = Math.abs(diffSeconds);

  if (abs < 60) return "همین الان";

  for (const [unit, secondsInUnit] of RELATIVE_UNITS) {
    if (abs >= secondsInUnit) {
      return relativeFormatter.format(Math.round(diffSeconds / secondsInUnit), unit);
    }
  }
  return relativeFormatter.format(Math.round(diffSeconds / 60), "minute");
}
