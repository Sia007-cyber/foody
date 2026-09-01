import { useState, type InputHTMLAttributes, type ReactNode, type SelectHTMLAttributes, type TextareaHTMLAttributes } from "react";

interface FieldWrapperProps {
  label?: string;
  error?: string;
  children: ReactNode;
  htmlFor?: string;
}

function FieldWrapper({ label, error, children, htmlFor }: FieldWrapperProps) {
  return (
    <div className="field">
      {label && (
        <label className="field-label" htmlFor={htmlFor}>
          {label}
        </label>
      )}
      {children}
      {error && <span className="field-error">{error}</span>}
    </div>
  );
}

interface InputProps extends InputHTMLAttributes<HTMLInputElement> {
  label?: string;
  error?: string;
}

export function Input({ label, error, id, className, ...rest }: InputProps) {
  return (
    <FieldWrapper label={label} error={error} htmlFor={id}>
      <input id={id} className={`input ${className ?? ""}`} {...rest} />
    </FieldWrapper>
  );
}

interface PasswordInputProps extends Omit<InputHTMLAttributes<HTMLInputElement>, "type"> {
  label?: string;
  error?: string;
}

/** Password input with a toggle to reveal/hide the value. */
export function PasswordInput({ label, error, id, className, ...rest }: PasswordInputProps) {
  const [visible, setVisible] = useState(false);

  return (
    <FieldWrapper label={label} error={error} htmlFor={id}>
      <div className="password-field">
        <input
          id={id}
          type={visible ? "text" : "password"}
          className={`input ${className ?? ""}`}
          {...rest}
        />
        <button
          type="button"
          className="password-toggle"
          onClick={() => setVisible((v) => !v)}
          aria-label={visible ? "پنهان کردن رمز عبور" : "نمایش رمز عبور"}
          tabIndex={-1}
        >
          {visible ? <EyeOffIcon /> : <EyeIcon />}
        </button>
      </div>
    </FieldWrapper>
  );
}

function EyeIcon() {
  return (
    <svg width="18" height="18" viewBox="0 0 24 24" fill="none" xmlns="http://www.w3.org/2000/svg">
      <path
        d="M1.5 12S5 5 12 5s10.5 7 10.5 7-3.5 7-10.5 7S1.5 12 1.5 12Z"
        stroke="currentColor"
        strokeWidth="1.6"
        strokeLinecap="round"
        strokeLinejoin="round"
      />
      <circle cx="12" cy="12" r="3" stroke="currentColor" strokeWidth="1.6" />
    </svg>
  );
}

function EyeOffIcon() {
  return (
    <svg width="18" height="18" viewBox="0 0 24 24" fill="none" xmlns="http://www.w3.org/2000/svg">
      <path
        d="M3 3l18 18M10.6 5.2C11 5.1 11.5 5 12 5c7 0 10.5 7 10.5 7-.6 1.2-1.7 2.9-3.4 4.3M6.6 6.6C3.7 8.3 1.5 12 1.5 12S5 19 12 19c1.4 0 2.6-.3 3.7-.7M9.9 9.9a3 3 0 0 0 4.2 4.2"
        stroke="currentColor"
        strokeWidth="1.6"
        strokeLinecap="round"
        strokeLinejoin="round"
      />
    </svg>
  );
}

interface TextareaProps extends TextareaHTMLAttributes<HTMLTextAreaElement> {
  label?: string;
  error?: string;
}

export function Textarea({ label, error, id, className, ...rest }: TextareaProps) {
  return (
    <FieldWrapper label={label} error={error} htmlFor={id}>
      <textarea id={id} className={`textarea ${className ?? ""}`} {...rest} />
    </FieldWrapper>
  );
}

interface SelectProps extends SelectHTMLAttributes<HTMLSelectElement> {
  label?: string;
  error?: string;
}

export function Select({ label, error, id, className, children, ...rest }: SelectProps) {
  return (
    <FieldWrapper label={label} error={error} htmlFor={id}>
      <select id={id} className={`select ${className ?? ""}`} {...rest}>
        {children}
      </select>
    </FieldWrapper>
  );
}
