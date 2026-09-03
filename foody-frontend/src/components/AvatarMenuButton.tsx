import { useNavigate } from "react-router-dom";
import { useAuth } from "../features/auth/AuthContext";
import { resolveMediaUrl } from "../lib/api";
import { UserIcon } from "./icons";

/** Avatar button shown next to the notification bell in every layout (public header
 *  and dashboard shells alike). Clicking it goes straight to the profile page — every
 *  role (customer, business owner, admin) has the exact same personal profile. */
export function AvatarMenuButton() {
  const { user } = useAuth();
  const navigate = useNavigate();

  if (!user) return null;

  const photoUrl = resolveMediaUrl(user.profileImageUrl);

  return (
    <button
      type="button"
      className="avatar-btn"
      aria-label="پروفایل من"
      title="پروفایل من"
      onClick={() => navigate("/profile")}
    >
      {photoUrl ? (
        <img src={photoUrl} alt="" className="avatar-btn-img" />
      ) : (
        <span className="avatar-btn-fallback">
          <UserIcon size={17} />
        </span>
      )}
    </button>
  );
}
