import { useEffect, useState, type FormEvent } from "react";
import { useMutation, useQuery, useQueryClient } from "@tanstack/react-query";
import { businessApi } from "../businesses/businessApi";
import { DashboardShell } from "../../components/DashboardShell";
import { Input, Textarea } from "../../components/Field";
import { Button } from "../../components/Button";
import { PageSpinner, ErrorState } from "../../components/Controls";
import { BusinessStatusBadge } from "../../components/Badge";
import { useToast, errorMessage } from "../../components/Feedback";
import { ownerNavItems } from "./ownerNav";

export function OwnerProfilePage() {
  const queryClient = useQueryClient();
  const { notify } = useToast();
  const {
    data: business,
    isLoading,
    isError,
    error,
    refetch,
  } = useQuery({
    queryKey: ["business", "profile"],
    queryFn: businessApi.myProfile,
  });

  const [form, setForm] = useState({ name: "", description: "", address: "", phone: "" });

  useEffect(() => {
    if (business) {
      setForm({
        name: business.name ?? "",
        description: business.description ?? "",
        address: business.address ?? "",
        phone: business.phone ?? "",
      });
    }
  }, [business]);

  const mutation = useMutation({
    mutationFn: businessApi.updateMyProfile,
    onSuccess: () => {
      queryClient.invalidateQueries({ queryKey: ["business", "profile"] });
      notify("پروفایل به‌روزرسانی شد", "ok");
    },
    onError: (err) => notify(errorMessage(err), "danger"),
  });

  function handleSubmit(e: FormEvent) {
    e.preventDefault();
    mutation.mutate(form);
  }

  return (
    <DashboardShell navItems={ownerNavItems} title="پروفایل کسب‌وکار">
      {isLoading ? (
        <PageSpinner />
      ) : isError || !business ? (
        <ErrorState error={error} onRetry={() => refetch()} title="پروفایل لود نشد" />
      ) : (
        <div style={{ maxWidth: 480 }}>
          <div style={{ marginBottom: 20 }}>
            <BusinessStatusBadge status={business.status} />
          </div>
          <form onSubmit={handleSubmit} style={{ display: "flex", flexDirection: "column", gap: 14 }}>
            <Input
              label="نام کسب‌وکار"
              value={form.name}
              onChange={(e) => setForm({ ...form, name: e.target.value })}
            />
            <Textarea
              label="توضیحات"
              value={form.description}
              onChange={(e) => setForm({ ...form, description: e.target.value })}
            />
            <Input
              label="آدرس"
              value={form.address}
              onChange={(e) => setForm({ ...form, address: e.target.value })}
            />
            <Input
              label="تلفن"
              dir="ltr"
              value={form.phone}
              onChange={(e) => setForm({ ...form, phone: e.target.value })}
            />
            <Button type="submit" loading={mutation.isPending}>
              ذخیره تغییرات
            </Button>
          </form>
        </div>
      )}
    </DashboardShell>
  );
}
