# Foody Platform — Phase 0 & Phase 1 Technical Spec

> این سند برای شروع پیاده‌سازی با Claude Code نوشته شده. هدف: راه‌اندازی زیرساخت پایه (فاز ۰) و ساخت یک MVP قابل‌دمو (فاز ۱) بر اساس اسکوپ کامل پروژه.

## تصمیمات کلیدی (Locked Decisions)

| موضوع | تصمیم |
|---|---|
| نوع تحویل سفارش | سه حالت: `PICKUP`, `DELIVERY`, `DINE_IN` |
| مدیریت پیک Delivery | خارج از اسکوپ Foody — مسئولیت کسب‌وکاره. سیستم فقط سفارش را با نوع DELIVERY ثبت و به پنل کسب‌وکار نمایش می‌دهد. بدون تخصیص پیک، بدون ردیابی زنده. |
| پرداخت آنلاین | خارج از فاز ۱ — به فازی که در بخش "فازهای بعدی" مشخص شده موکول می‌شود. در فاز ۱ فقط `PAY_ON_DELIVERY` / `PAY_AT_COUNTER` وجود دارد. |
| رزرو میز + سفارش غذا | مستقل از هم. در فاز ۱ رزرو فقط جای نشستن را تضمین می‌کند؛ پیش‌سفارش غذا هنگام رزرو در این فاز نیست. |
| معماری | Modular Monolith (Spring Boot) — هر ماژول یک پکیج مجزا با مرز واضح، آماده برای جداسازی احتمالی در آینده |
| مدل Business/Product | Generic و قابل‌گسترش از همین فاز ۰ (نه hardcoded برای کافه/فست‌فود) |

---

## فاز ۰ — زیرساخت پایه

### هدف
قبل از هر فیچر کاربردی، اسکلت پروژه، auth، و مدل داده‌ی هسته باید آماده باشد.

### Backend Setup
- Spring Boot (نسخه‌ی پایدار فعلی) + Maven
- ماژول‌بندی به‌صورت پکیج‌های مجزا زیر یک اپلیکیشن:
  ```
  com.foody
  ├── auth
  ├── users
  ├── businesses
  ├── menus
  ├── products
  ├── orders
  ├── reservations
  ├── reviews
  ├── notifications
  ├── admin
  └── common (shared entities, exceptions, utils)
  ```
- هر ماژول: `controller`, `service`, `repository`, `entity`, `dto` جدا
- ماژول‌ها فقط از طریق service interface به هم دیگر دسترسی داشته باشند (نه repository مستقیم بین ماژول‌ها) — این قانون از همین اول رعایت شود تا جداسازی آینده به میکروسرویس راحت باشد.
- Flyway برای migration از commit اول فعال باشد (هیچ تغییر schema بدون migration file)
- Spring Security + JWT (access token + refresh token)
- Global exception handler استاندارد (یک فرمت واحد خطا برای همه‌ی API ها)

### Database (MySQL) — مدل‌های هسته فاز ۰

**users**
```
id, email, phone, password_hash, full_name, role (CUSTOMER/BUSINESS_OWNER/ADMIN),
status (ACTIVE/SUSPENDED), created_at, updated_at
```

**businesses**
```
id, owner_user_id, name, description, business_type (CAFE/FAST_FOOD — enum قابل‌گسترش),
address, latitude, longitude, phone, status (PENDING/APPROVED/REJECTED/SUSPENDED),
cover_image_url, created_at, updated_at
```
> نکته‌ی معماری: `business_type` باید enum باشد ولی به‌گونه‌ای طراحی شود که افزودن نوع جدید (RESTAURANT, BAKERY, ...) فقط نیاز به افزودن مقدار enum + یک رکورد پیکربندی داشته باشد، نه تغییر جدول.

**business_hours**
```
id, business_id, day_of_week, open_time, close_time, is_closed
```

**menus**
```
id, business_id, name, display_order
```

**products**
```
id, menu_id, name, description, price, image_url, is_available, display_order
```

### Auth Endpoints (فاز ۰)
```
POST /api/auth/register        (customer)
POST /api/auth/login
POST /api/auth/refresh
POST /api/auth/logout
GET  /api/users/me
PATCH /api/users/me
```

### خروجی قابل‌قبول فاز ۰
- اپ backend بالا می‌آید، migration ها اجرا می‌شوند
- ثبت‌نام/ورود مشتری کار می‌کند و JWT برمی‌گرداند
- یک کسب‌وکار می‌تواند به‌صورت دستی (seed data) در دیتابیس ساخته شود و از طریق API قابل مشاهده باشد
- Frontend اسکلت (Vite + React + TS + React Router + TanStack Query) بالا می‌آید و صفحه‌ی لاگین کار می‌کند

---

## فاز ۱ — MVP هسته‌ای

### دامنه‌ی فاز ۱
مسیر Ordering (Pickup + Delivery بدون مدیریت پیک) + Reservation ساده + پنل کسب‌وکار حداقلی + پنل ادمین حداقلی.

### 1. Customer App

**Discover**
```
GET /api/businesses?type=&search=&lat=&lng=
GET /api/businesses/{id}
```
- بدون جستجوی پیشرفته/فیلتر پیچیده در فاز ۱ — فقط فیلتر نوع کسب‌وکار + جستجوی متنی ساده روی نام

**Menu**
```
GET /api/businesses/{id}/menus
GET /api/products/{id}
```

**Cart & Order**
```
POST /api/orders
  body: { business_id, fulfillment_type: PICKUP|DELIVERY, items: [{product_id, qty}], delivery_address? }
GET  /api/orders/{id}
GET  /api/orders/my
PATCH /api/orders/{id}/cancel   (فقط قبل از تایید کسب‌وکار)
```

**Order status flow (فاز ۱):**
```
PENDING → ACCEPTED → PREPARING → READY → COMPLETED
                   → REJECTED
PENDING → CANCELLED (توسط مشتری)
```

**Reservation**
```
GET  /api/businesses/{id}/reservation-availability?date=
POST /api/reservations
  body: { business_id, date, time, guest_count }
GET  /api/reservations/my
PATCH /api/reservations/{id}/cancel
```

**Reservation status flow:**
```
PENDING → CONFIRMED → COMPLETED
        → REJECTED
PENDING/CONFIRMED → CANCELLED (توسط مشتری)
```

### 2. Business Panel

```
PATCH /api/business/profile          (اطلاعات کسب‌وکار، ساعات کاری)
POST  /api/business/menus
POST  /api/business/products
PATCH /api/business/products/{id}    (قیمت، موجودی)

GET   /api/business/orders?status=
PATCH /api/business/orders/{id}/status

GET   /api/business/reservations?date=
PATCH /api/business/reservations/{id}/status
```

### 3. Admin Panel

```
GET   /api/admin/businesses?status=PENDING
PATCH /api/admin/businesses/{id}/approve
PATCH /api/admin/businesses/{id}/reject
PATCH /api/admin/businesses/{id}/suspend

GET   /api/admin/dashboard/summary
  → تعداد کاربران، کسب‌وکارهای فعال، تعداد سفارش/رزرو کل
```

### خروجی قابل‌قبول فاز ۱ (Demo Criteria)
1. مشتری ثبت‌نام می‌کند، کسب‌وکاری را پیدا می‌کند، منو را می‌بیند
2. مشتری سفارش pickup یا delivery ثبت می‌کند (بدون پرداخت آنلاین)
3. صاحب کسب‌وکار سفارش را در پنل خودش می‌بیند و وضعیتش را تغییر می‌دهد
4. مشتری وضعیت سفارش را دنبال می‌کند
5. مشتری میز رزرو می‌کند و کسب‌وکار آن را تایید/رد می‌کند
6. ادمین یک کسب‌وکار جدید را تایید می‌کند تا در اپ نمایش داده شود

---

## فازهای بعدی (خارج از این سند، فقط برای مرجع)
- فاز ۲: پرداخت آنلاین، نوتیفیکیشن واقعی (push/SMS)، نظرات و امتیازدهی
- فاز ۳: Wallet، تخفیف و کد تخفیف، Referral/Reward
- فاز ۴: تبلیغات، آمار پیشرفته، PWA کامل (offline, push)

---

## نکات برای Claude Code
- از همین فاز ۰ migration های Flyway را version-controlled و incremental بساز (`V1__init.sql`, `V2__...`)
- entity ها را با روابط JPA درست تعریف کن ولی از cascade های خطرناک (مثل `CascadeType.REMOVE` روی order↔product) پرهیز کن
- برای هر ماژول یک DTO جدا از Entity استفاده کن (هیچ‌وقت Entity مستقیم در response API برنگرده)
- تست‌های واحد حداقل برای منطق تغییر وضعیت سفارش/رزرو (state machine) نوشته شود
- برای frontend: state سبد خرید را فعلاً در React state/context نگه دار (نه localStorage در artifact، ولی در پروژه‌ی واقعی React Native/Web مشکلی نداره)
