import type { ButtonHTMLAttributes } from "react";

interface ButtonProps extends ButtonHTMLAttributes<HTMLButtonElement> {
  variant?: "primary" | "secondary" | "ghost" | "danger" | "ok";
  size?: "md" | "sm";
  block?: boolean;
  loading?: boolean;
}

export function Button({
  variant = "primary",
  size = "md",
  block,
  loading,
  disabled,
  className,
  children,
  ...rest
}: ButtonProps) {
  const classes = [
    "btn",
    `btn-${variant}`,
    size === "sm" ? "btn-sm" : "",
    block ? "btn-block" : "",
    className ?? "",
  ]
    .filter(Boolean)
    .join(" ");

  return (
    <button className={classes} disabled={disabled || loading} {...rest}>
      {loading ? <span className="spinner" style={{ width: 16, height: 16 }} /> : children}
    </button>
  );
}
