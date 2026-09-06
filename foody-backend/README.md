# Foody Backend

بک‌اند Foody با معماری Spring Boot Modular Monolith برای سفارش غذا، رزرو مستقل میز و پنل‌های مشتری، مالک و ادمین پیاده‌سازی شده است. وضعیت قابلیت‌ها، محدودیت‌ها و اولویت‌های ادامهٔ کار در [README اصلی](../README.md) نگهداری می‌شوند.

## فناوری و ماژول‌ها

- Java 21، Spring Boot 3.5.16 و Maven
- Spring Security و JWT برای access و refresh
- Spring Data JPA، MySQL و Flyway؛ migrationهای V1 تا V11
- JUnit، Mockito، MockMvc و Testcontainers با MySQL 8.4

ماژول‌های فعال شامل `auth`، `users`، `businesses`، `menus`، `products`، `orders`، `reservations`، `notifications`، `wallet` و `admin` هستند. `reviews` هنوز اسکلت است. کیف پول موجودی، شارژ شبیه‌سازی‌شده و تاریخچه دارد و هنوز به پرداخت سفارش متصل نیست.

قاعدهٔ معماری، ارتباط ماژول‌ها از طریق interface سرویس‌هاست؛ تست خودکار مرز ماژول‌ها هنوز در نقشهٔ راه قرار دارد. تغییر schema باید با migration جدید انجام شود.

## اجرای محلی

دستورها را از پوشهٔ `foody-backend` اجرا کنید. ابتدا یک MySQL محلی با دیتابیس و کاربر `foody` آماده کنید؛ نمونهٔ Docker در [راهنمای اصلی](../README.md) آمده است.

```bash
SPRING_PROFILES_ACTIVE=local \
DB_HOST=localhost DB_PORT=3309 DB_USERNAME=foody DB_PASSWORD=foody \
mvn spring-boot:run
```

پورت بالا با نمونهٔ Docker راهنمای اصلی مطابقت دارد؛ برای MySQL روی پورت معمول، `DB_PORT=3306` قرار دهید. برنامه به‌صورت پیش‌فرض روی `http://localhost:8080` اجرا می‌شود و Flyway migrationها را اعمال می‌کند.

پروفایل پیش‌فرض `tc` برای تست‌ها تنظیم شده است؛ راه‌اندازی خودکار کانتینر در زیرساخت تست انجام می‌شود و اجرای معمول برنامه با `mvn spring-boot:run` به‌تنهایی کانتینر ایجاد نمی‌کند.

## تنظیمات

| متغیر | کاربرد |
|---|---|
| `SPRING_PROFILES_ACTIVE` | `local` برای اجرای محلی، `prod` برای محیط منتشرشده |
| `DB_HOST`, `DB_PORT`, `DB_USERNAME`, `DB_PASSWORD` | اتصال MySQL |
| `DB_NAME` | نام دیتابیس در پروفایل `prod`؛ در `local` نام `foody` است |
| `PORT` | پورت HTTP؛ پیش‌فرض `8080` |
| `FOODY_JWT_SECRET` | کلید HMAC با Base64 معتبر؛ در `prod` الزامی و حداقل ۳۲ بایت پس از decode |
| `FOODY_CORS_ALLOWED_ORIGINS` | originهای مجاز، جداشده با کاما؛ پیش‌فرض `http://localhost:5173` |
| `FOODY_UPLOAD_DIR` | مسیر محلی تصاویر؛ پیش‌فرض `./uploads` |

عمر access پیش‌فرض ۱۵ دقیقه و refresh هفت روز است. پروفایل‌های `local` و `tc` برای راحتی توسعه از fallback شناخته‌شدهٔ مخزن استفاده می‌کنند. پروفایل `prod` بدون `FOODY_JWT_SECRET` اجرا نمی‌شود و مقدار خالی، fallback توسعه، Base64 نامعتبر یا کلید کوتاه‌تر از ۲۵۶ بیت را هنگام startup رد می‌کند.

برای تولید یک کلید تصادفی ۲۵۶ بیتی مناسب:

```bash
openssl rand -base64 32
```

خروجی را فقط در secret/environment محیط انتشار قرار دهید. آن را در مخزن، README یا فایل `.env` commit نکنید و از fallback توسعه در production استفاده نکنید. migrationها همچنان حساب‌های نمایشی مالک و ادمین ایجاد می‌کنند؛ بررسی سیاست seed محیط منتشرشده جداگانه باقی مانده است.

## قراردادها و محدودیت‌های مهم

- ثبت‌نام برای `CUSTOMER` و `BUSINESS_OWNER` مجاز است؛ ثبت‌نام `ADMIN` مجاز نیست.
- حساب معلق یا حذف‌شده در access و refresh رد می‌شود. logout هنوز ابطال سمت سرور ندارد و refresh rotation پیاده‌سازی نشده است.
- سفارش فقط `PICKUP` یا `DELIVERY` است؛ رزرو میز مستقل از سفارش است.
- `GET /api/businesses/{id}/reservation-availability?date=2026-09-06` برای کسب‌وکار تأییدشده، پاسخ زیر را می‌دهد:

```json
{"date":"2026-09-06","availabilityCalculated":false}
```

این پاسخ هیچ رکورد یا اطلاعات مشتری ندارد. مقدار `false` یعنی ظرفیت محاسبه نشده است؛ وضعیت آزاد یا پر بودن را مشخص نمی‌کند. کسب‌وکار ناموجود یا تأییدنشده `404` می‌دهد. جزئیات رزرو فقط برای مشتری یا مالک مجاز در دسترس است.

خطاهای برنامه handler مشترک دارند؛ قالب همهٔ خطاهای امنیتی و درخواست هنوز یکسان نشده است. فهرست APIهای اصلی در [README اصلی](../README.md) آمده است.

## تست

با Docker در حال اجرا:

```bash
mvn test
```

تست‌های یکپارچه MySQL واقعی را با Testcontainers راه‌اندازی می‌کنند. آخرین اجرای کامل شامل ۲۰۰ تست با صفر failure، error و skip است. رقابت‌های وضعیت Order و Reservation در تراکنش‌ها و persistence contextهای جدا بررسی می‌شوند. تست اختصاصی کیف پول و مرز ماژول‌ها هنوز اضافه نشده است.
