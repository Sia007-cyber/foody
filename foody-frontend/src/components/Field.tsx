import type { InputHTMLAttributes, ReactNode, SelectHTMLAttributes, TextareaHTMLAttributes } from "react";

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
