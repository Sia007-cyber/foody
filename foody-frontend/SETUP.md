# Foody Frontend — راه‌اندازی

## نصب
```bash
cd foody-frontend
npm install
cp .env.example .env
npm run dev
```
روی `http://localhost:5173` بالا میاد.

## پیش‌نیاز: تغییرات بک‌اند
این فرانت‌اند برای کار کردن به دو تغییر کوچیک در بک‌اند نیاز داره که در
`backend-cors-and-admin-seed.patch` هستن:

1. **CORS** — بک‌اند فعلاً هیچ تنظیم CORSای نداره. بدونش مرورگر همه‌ی
   درخواست‌های فرانت‌اند (`localhost:5173`) به بک‌اند (`localhost:8080`) رو بلاک
   می‌کنه. پچ یه `CorsConfigurationSource` اضافه می‌کنه که از روی property
   `foody.cors.allowed-origins` (پیش‌فرض `http://localhost:5173`) می‌خونه.
2. **Seed اکانت ادمین** — چون مسیر ثبت‌نام فقط CUSTOMER می‌سازه، بدون این
   migration هیچ راهی برای تست پنل ادمین نبود. یه migration جدید
   (`V5__seed_admin_user.sql`) یه اکانت ادمین seed می‌کنه:
   - ایمیل: `admin@foody.test`
   - رمز: `password123`

اعمال پچ:
```bash
cd foody   # ریشه‌ی ریپو
git apply backend-cors-and-admin-seed.patch
```

## اکانت‌های تست
| نقش | ایمیل | رمز |
|---|---|---|
| Business Owner (seed شده در V2) | `owner@foody.test` | `password123` |
| Admin (seed شده در V5 جدید) | `admin@foody.test` | `password123` |
| Customer | با فرم ثبت‌نام خودت بساز |

## نکات
- هیچ عکس واقعی استفاده نشده — کاور کسب‌وکارها با گرادیان رنگی جایگزین شده
  (به‌جای عکس استوک). جزئیات طراحی در `DESIGN.md`.
- تم فعلاً فقط روشن (Light) هست.
- `npm run build` تست شد و بدون خطای TypeScript پاس می‌شه.
