import { useState, type FormEvent } from "react";
import { useMutation } from "@tanstack/react-query";
import { Button } from "../../components/Button";
import { Input } from "../../components/Field";
import { errorMessage } from "../../components/Feedback";
import type { CustomerLookup, CustomerSearchResponse } from "../../types/api";
import { ownerWalletApi } from "./ownerWalletApi";

export function CustomerPicker({ selected, onSelect }: { selected: CustomerLookup | null; onSelect: (customer: CustomerLookup | null) => void }) {
  const [query, setQuery] = useState("");
  const [result, setResult] = useState<CustomerSearchResponse | null>(null);
  const [searchedQuery, setSearchedQuery] = useState("");
  const [searchError, setSearchError] = useState("");
  const search = useMutation({
    mutationFn: ({ value, page }: { value: string; page: number }) => ownerWalletApi.searchCustomers(value, page),
    onSuccess: (data) => { setResult(data); setSearchError(""); },
    onError: (error) => { setResult(null); setSearchError(errorMessage(error)); },
  });

  function submit(event: FormEvent) {
    event.preventDefault();
    if (query.trim().length < 2) { setSearchError("حداقل ۲ نویسه وارد کنید."); return; }
    onSelect(null);
    const value = query.trim();
    setSearchedQuery(value);
    search.mutate({ value, page: 0 });
  }

  return <div className="customer-picker">
    <form className="owner-wallet-action-form customer-picker-search" onSubmit={submit} role="search">
      <Input id="customer-search" label="نام یا شناسه فودی مشتری" placeholder="مثلاً سارا احمدی یا F-…" required minLength={2} maxLength={80}
        value={query} onChange={(event) => { setQuery(event.target.value); setSearchError(""); }} aria-describedby="customer-search-help" />
      <Button type="submit" size="sm" loading={search.isPending}>جست‌وجو</Button>
    </form>
    <p id="customer-search-help" className="customer-picker-help">برای حفظ حریم خصوصی، فقط نتیجه‌های مطابق جست‌وجو نمایش داده می‌شوند.</p>
    {searchError && <p className="field-error" role="alert">{searchError}</p>}
    {result && result.items.length === 0 && <p className="customer-picker-empty">مشتری مطابقی پیدا نشد.</p>}
    {result && result.items.length > 0 && <div className="customer-picker-results" role="listbox" aria-label="نتیجه‌های جست‌وجوی مشتری">
      {result.items.map((customer) => <button key={customer.publicId} type="button" role="option"
        aria-selected={selected?.publicId === customer.publicId}
        className={`customer-picker-result ${selected?.publicId === customer.publicId ? "is-selected" : ""}`}
        onClick={() => onSelect(customer)}>
        <span>{customer.displayName}</span><code dir="ltr">{customer.publicId}</code>
      </button>)}
    </div>}
    {result && (result.page > 0 || result.hasMore) && <div className="customer-picker-pagination">
      <Button type="button" variant="ghost" size="sm" disabled={result.page === 0 || search.isPending} onClick={() => search.mutate({ value: searchedQuery, page: result.page - 1 })}>قبلی</Button>
      <span>صفحه {result.page + 1}</span>
      <Button type="button" variant="ghost" size="sm" disabled={!result.hasMore || search.isPending} onClick={() => search.mutate({ value: searchedQuery, page: result.page + 1 })}>بعدی</Button>
    </div>}
  </div>;
}
