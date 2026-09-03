import { useEffect, useRef, useState, type FormEvent } from "react";
import { useMutation, useQuery, useQueryClient } from "@tanstack/react-query";
import { businessApi } from "../businesses/businessApi";
import { DashboardShell } from "../../components/DashboardShell";
import { Input, Textarea } from "../../components/Field";
import { Button } from "../../components/Button";
import { PageSpinner, ErrorState } from "../../components/Controls";
import { BusinessStatusBadge } from "../../components/Badge";
import { useToast, errorMessage } from "../../components/Feedback";
import { CameraIcon, StoreIcon } from "../../components/icons";
import { LocationPicker } from "../../components/LocationPicker";
import { resolveMediaUrl } from "../../lib/api";
import { ownerNavItems } from "./ownerNav";

export function OwnerProfilePage() {
  const queryClient = useQueryClient();
  const { notify } = useToast();
  const fileInputRef = useRef<HTMLInputElement>(null);
  const [uploadingCover, setUploadingCover] = useState(false);
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

  const [form, setForm] = useState<{
    name: string;
    description: string;
    address: string;
    phone: string;
    latitude: number | null;
    longitude: number | null;
  }>({ name: "", description: "", address: "", phone: "", latitude: null, longitude: null });

  useEffect(() => {
    if (business) {
      setForm({
        name: business.name ?? "",
        description: business.description ?? "",
        address: business.address ?? "",
        phone: business.phone ?? "",
        latitude: business.latitude ?? null,
        longitude: business.longitude ?? null,
      });
    }
  }, [business]);

  const mutation = useMutation({
    mutationFn: () =>
      businessApi.updateMyProfile({
        ...form,
        latitude: form.latitude ?? undefined,
        longitude: form.longitude ?? undefined,
      }),
    onSuccess: () => {
      queryClient.invalidateQueries({ queryKey: ["business", "profile"] });
      notify("پروفایل به‌روزرسانی شد", "ok");
    },
    onError: (err) => notify(errorMessage(err), "danger"),
  });

  async function handleCoverChange(e: React.ChangeEvent<HTMLInputElement>) {
    const file = e.target.files?.[0];
    e.target.value = ""; // allow re-selecting the same file later
    if (!file) return;

    setUploadingCover(true);
    try {
      const { url } = await businessApi.uploadCoverImage(file);
      await businessApi.updateMyProfile({ coverImageUrl: url });
      queryClient.invalidateQueries({ queryKey: ["business", "profile"] });
      notify("عکس کسب‌وکار به‌روز شد", "ok");
    } catch (err) {
      notify(errorMessage(err), "danger");
    } finally {
      setUploadingCover(false);
    }
  }

  function handleSubmit(e: FormEvent) {
    e.preventDefault();
    mutation.mutate();
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

          <div className="owner-cover-row">
            <button
              type="button"
              className="owner-cover-btn"
              onClick={() => fileInputRef.current?.click()}
              disabled={uploadingCover}
              aria-label="تغییر عکس کسب‌وکار"
            >
              {business.coverImageUrl ? (
                <img src={resolveMediaUrl(business.coverImageUrl) ?? undefined} alt="" className="owner-cover-img" />
              ) : (
                <span className="owner-cover-fallback">
                  <StoreIcon size={28} />
                </span>
              )}
              <span className="owner-cover-edit">
                <CameraIcon size={15} />
              </span>
            </button>
            <input
              ref={fileInputRef}
              type="file"
              accept="image/jpeg,image/png,image/webp"
              hidden
              onChange={handleCoverChange}
            />
            <span className="owner-cover-hint">
              {uploadingCover ? "در حال آپلود عکس..." : "برای تغییر عکس کسب‌وکار کلیک کن"}
            </span>
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
            <LocationPicker
              latitude={form.latitude}
              longitude={form.longitude}
              onChange={(lat, lng) => setForm({ ...form, latitude: lat, longitude: lng })}
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
