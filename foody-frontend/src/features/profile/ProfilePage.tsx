import { useRef, useState, type FormEvent } from "react";
import { useAuth } from "../auth/AuthContext";
import { usersApi } from "./usersApi";
import { Input, PasswordInput, Textarea } from "../../components/Field";
import { Button } from "../../components/Button";
import { useToast, errorMessage } from "../../components/Feedback";
import { CameraIcon, UserIcon } from "../../components/icons";
import { LocationPicker } from "../../components/LocationPicker";
import { resolveMediaUrl } from "../../lib/api";
import "./profile.css";

const ROLE_LABEL: Record<string, string> = {
  CUSTOMER: "مشتری",
  BUSINESS_OWNER: "کافه‌دار / رستوران‌دار",
  ADMIN: "ادمین",
};

export function ProfilePage() {
  const { user, updateUser } = useAuth();
  const { notify } = useToast();
  const fileInputRef = useRef<HTMLInputElement>(null);

  const [fullName, setFullName] = useState(user?.fullName ?? "");
  const [email, setEmail] = useState(user?.email ?? "");
  const [phone, setPhone] = useState(user?.phone ?? "");
  const [address, setAddress] = useState(user?.address ?? "");
  const [latitude, setLatitude] = useState<number | null>(user?.latitude ?? null);
  const [longitude, setLongitude] = useState<number | null>(user?.longitude ?? null);
  const [password, setPassword] = useState("");
  const [confirmPassword, setConfirmPassword] = useState("");

  const [saving, setSaving] = useState(false);
  const [uploadingPhoto, setUploadingPhoto] = useState(false);
  const [error, setError] = useState<string | null>(null);

  if (!user) return null;

  const photoUrl = resolveMediaUrl(user.profileImageUrl);

  async function handlePhotoChange(e: React.ChangeEvent<HTMLInputElement>) {
    const file = e.target.files?.[0];
    e.target.value = ""; // allow re-selecting the same file later
    if (!file) return;

    setUploadingPhoto(true);
    try {
      const { url } = await usersApi.uploadPhoto(file);
      const updated = await usersApi.updateMe({ profileImageUrl: url });
      updateUser(updated);
      notify("عکس پروفایل به‌روز شد", "ok");
    } catch (err) {
      notify(errorMessage(err), "danger");
    } finally {
      setUploadingPhoto(false);
    }
  }

  async function handleSubmit(e: FormEvent) {
    e.preventDefault();
    setError(null);

    if (password && password !== confirmPassword) {
      setError("رمز عبور و تکرار آن یکسان نیستند");
      return;
    }

    setSaving(true);
    try {
      const updated = await usersApi.updateMe({
        fullName,
        email,
        phone: phone || undefined,
        address: address || undefined,
        latitude: latitude ?? undefined,
        longitude: longitude ?? undefined,
        password: password || undefined,
      });
      updateUser(updated);
      setPassword("");
      setConfirmPassword("");
      notify("پروفایل ذخیره شد", "ok");
    } catch (err) {
      notify(errorMessage(err), "danger");
    } finally {
      setSaving(false);
    }
  }

  return (
    <div className="container profile-page">
      <h1 className="profile-title">پروفایل من</h1>

      <div className="profile-avatar-row">
        <button
          type="button"
          className="profile-avatar"
          onClick={() => fileInputRef.current?.click()}
          disabled={uploadingPhoto}
          aria-label="تغییر عکس پروفایل"
        >
          {photoUrl ? (
            <img src={photoUrl} alt="" className="profile-avatar-img" />
          ) : (
            <span className="profile-avatar-fallback">
              <UserIcon size={32} />
            </span>
          )}
          <span className="profile-avatar-edit">
            <CameraIcon size={16} />
          </span>
        </button>
        <input
          ref={fileInputRef}
          type="file"
          accept="image/jpeg,image/png,image/webp"
          hidden
          onChange={handlePhotoChange}
        />
        <div className="profile-avatar-meta">
          <span className="profile-avatar-name">{user.fullName}</span>
          <span className="profile-avatar-role">{ROLE_LABEL[user.role] ?? user.role}</span>
          {uploadingPhoto && <span className="profile-avatar-uploading">در حال آپلود عکس...</span>}
        </div>
      </div>

      <form onSubmit={handleSubmit} className="profile-form">
        <Input label="نام کامل" value={fullName} onChange={(e) => setFullName(e.target.value)} required />
        <Input
          label="ایمیل"
          type="email"
          dir="ltr"
          value={email}
          onChange={(e) => setEmail(e.target.value)}
          required
        />
        <Input label="شماره تماس" dir="ltr" value={phone} onChange={(e) => setPhone(e.target.value)} />
        <Textarea
          label="آدرس دقیق"
          value={address}
          onChange={(e) => setAddress(e.target.value)}
          rows={3}
          placeholder="استان، شهر، خیابان، پلاک، واحد..."
        />
        <LocationPicker
          latitude={latitude}
          longitude={longitude}
          onChange={(lat, lng) => {
            setLatitude(lat);
            setLongitude(lng);
          }}
        />

        <div className="profile-password-section">
          <span className="profile-section-label">تغییر رمز عبور (اختیاری)</span>
          <div className="profile-password-grid">
            <PasswordInput
              label="رمز عبور جدید"
              value={password}
              onChange={(e) => setPassword(e.target.value)}
              autoComplete="new-password"
            />
            <PasswordInput
              label="تکرار رمز عبور جدید"
              value={confirmPassword}
              onChange={(e) => setConfirmPassword(e.target.value)}
              autoComplete="new-password"
            />
          </div>
        </div>

        {error && <span className="field-error">{error}</span>}

        <Button type="submit" loading={saving}>
          ذخیره تغییرات
        </Button>
      </form>
    </div>
  );
}
