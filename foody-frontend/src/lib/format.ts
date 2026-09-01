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
