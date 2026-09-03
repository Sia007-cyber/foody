import { useState } from "react";
import { Button } from "./Button";
import { MapPinIcon, CheckIcon } from "./icons";

/**
 * Lets the user attach their device's current GPS coordinates to whatever
 * free-text address they've typed, for any role (customer, business owner,
 * admin). Uses the browser's Geolocation API directly — no map picker UI,
 * just a "use my current location" button, consistent across every profile
 * form that has an address field.
 */
export function LocationPicker({
  latitude,
  longitude,
  onChange,
}: {
  latitude: number | null | undefined;
  longitude: number | null | undefined;
  onChange: (latitude: number, longitude: number) => void;
}) {
  const [status, setStatus] = useState<"idle" | "loading" | "error">("idle");
  const [errorMessage, setErrorMessage] = useState("");

  const hasLocation = latitude != null && longitude != null;

  function handleClick() {
    if (!("geolocation" in navigator)) {
      setStatus("error");
      setErrorMessage("مرورگر شما از موقعیت‌یابی پشتیبانی نمی‌کند");
      return;
    }

    setStatus("loading");
    setErrorMessage("");
    navigator.geolocation.getCurrentPosition(
      (position) => {
        onChange(position.coords.latitude, position.coords.longitude);
        setStatus("idle");
      },
      (err) => {
        setStatus("error");
        setErrorMessage(
          err.code === err.PERMISSION_DENIED
            ? "اجازه‌ی دسترسی به موقعیت مکانی داده نشد"
            : "دریافت موقعیت مکانی با خطا مواجه شد"
        );
      },
      { enableHighAccuracy: true, timeout: 10000 }
    );
  }

  return (
    <div className="location-picker">
      <Button
        type="button"
        variant="secondary"
        size="sm"
        onClick={handleClick}
        loading={status === "loading"}
      >
        <MapPinIcon size={16} />
        {hasLocation ? "به‌روزرسانی موقعیت مکانی" : "دریافت موقعیت مکانی فعلی"}
      </Button>

      {hasLocation && status !== "error" && (
        <span className="location-picker-status ok">
          <CheckIcon size={14} />
          موقعیت مکانی ثبت شده
        </span>
      )}
      {status === "error" && <span className="location-picker-status danger">{errorMessage}</span>}
    </div>
  );
}
