# 🍔 Foody — پلتفرم سفارش غذا و رزرو کافه/رستوران

پلتفرم Foody یک سیستم سفارش غذا و مدیریت کسب‌وکارهای غذایی (کافه/رستوران/فست‌فود) است که از سه پنل **مشتری**، **کسب‌وکار** و **ادمین** و سه روش تحویل **Pickup**، **Delivery** و **Dine-in/Reservation** پشتیبانی می‌کند.

> این README به‌صورت چک‌لیست نوشته شده تا پیشرفت پروژه قدم‌به‌قدم پیگیری بشه. هر فیچر که تکمیل و روی گیت‌هاب پوش بشه، تیک می‌خوره.

---

## 🧱 تکنولوژی‌ها

- **Backend:** Spring Boot (Modular Monolith) + Maven + Java 21
- **Auth:** Spring Security + JWT (Access + Refresh Token)
- **Database:** MySQL + Flyway Migrations
- **Frontend:** React + Vite + TypeScript *(بعد از تثبیت بک‌اند و تایید UI/UX شروع می‌شه)*
- **تست:** JUnit + Mockito (Unit) + MockMvc (Controller) + Testcontainers (Integration)

## 🔒 تصمیمات قفل‌شده‌ی اسکوپ

- پرداخت آنلاین → خارج از فاز ۱ (فعلاً فقط `PAY_ON_DELIVERY` / `PAY_AT_COUNTER`)
- مدیریت پیک/ناوگان تحویل → کلاً خارج از اسکوپ Foody (فقط ثبت سفارش با نوع `DELIVERY`)
- رزرو میز و سفارش غذا → در فاز ۱ کاملاً مستقل از هم هستن

---

## ✅ فاز ۰ — زیرساخت پایه

- [x] راه‌اندازی پروژه Spring Boot + Maven
- [x] ماژول‌بندی پکیج‌ها طبق معماری Modular Monolith (`auth`, `users`, `businesses`, `menus`, `products`, `orders`, `reservations`, `reviews`, `notifications`, `admin`, `common`)
- [x] Flyway migrations (`V1__init.sql`, `V2__seed_demo_business.sql`)
- [x] Spring Security + JWT (access + refresh token)
- [x] Global Exception Handler با فرمت خطای یکسان برای همه‌ی API‌ها
- [x] مدل‌های پایه: `users`, `businesses`, `business_hours`, `menus`, `products`
- [x] رفع باگ امنیتی مسیر `/api/businesses/*` (عدم نیاز اشتباه به auth روی منو/محصولات)
- [ ] اسکلت فرانت‌اند (Vite + React + TS + Router + TanStack Query) — *به تعویق افتاده*

### Auth & Users
- [x] `POST /api/auth/register`
- [x] `POST /api/auth/login`
- [x] `POST /api/auth/refresh`
- [x] `POST /api/auth/logout`
- [x] `GET /api/users/me`
- [x] `PATCH /api/users/me`

---

## ✅ فاز ۱ — MVP هسته‌ای

### 👤 پنل مشتری (Customer)

**کشف کسب‌وکار**
- [x] `GET /api/businesses?type=&search=` (لیست کسب‌وکارهای تاییدشده)
- [x] `GET /api/businesses/{id}`

**منو و محصولات**
- [x] `GET /api/businesses/{id}/menus`
- [x] `GET /api/menus/{menuId}/products`
- [x] `GET /api/products/{id}`

**سبد خرید و سفارش**
- [x] `POST /api/orders` (ثبت سفارش با نوع Pickup/Delivery)
- [x] `GET /api/orders/{id}`
- [x] `GET /api/orders/my`
- [x] `PATCH /api/orders/{id}/cancel`
- [x] State machine وضعیت سفارش: `PENDING → ACCEPTED → PREPARING → READY → COMPLETED` / `REJECTED` / `CANCELLED`

**رزرو میز**
- [x] `GET /api/businesses/{id}/reservation-availability?date=`
- [x] `POST /api/reservations`
- [x] `GET /api/reservations/my`
- [x] `GET /api/reservations/{id}`
- [x] `PATCH /api/reservations/{id}/cancel`
- [x] State machine وضعیت رزرو: `PENDING → CONFIRMED → COMPLETED` / `REJECTED` / `CANCELLED`

### 🏪 پنل کسب‌وکار (Business Owner)

- [x] `GET /api/business/profile`
- [x] `PATCH /api/business/profile`
- [x] `POST /api/business/menus`
- [x] `POST /api/business/products`
- [x] `PATCH /api/business/products/{id}`
- [x] `GET /api/business/orders?status=`
- [x] `PATCH /api/business/orders/{id}/status`
- [x] `GET /api/business/reservations?date=`
- [x] `PATCH /api/business/reservations/{id}/status`

### 🛠️ پنل ادمین (Admin)

- [x] `GET /api/admin/businesses?status=PENDING`
- [x] `PATCH /api/admin/businesses/{id}/approve`
- [x] `PATCH /api/admin/businesses/{id}/reject`
- [x] `PATCH /api/admin/businesses/{id}/suspend`
- [x] `GET /api/admin/dashboard/summary`

### 🧪 تست‌ها (تا این مرحله)
- [x] Unit test (Mockito) برای سرویس‌های Business, Menu, Product, Order, Reservation, Admin
- [ ] Unit test (Mockito) برای سرویس‌های `Auth` و `Users` — هنوز نوشته نشده
- [x] Controller test (MockMvc) برای همه‌ی endpoint های مشتری: Business, Menu, Product, Order, Reservation, Users
- [x] Controller test (MockMvc) برای همه‌ی endpoint های پنل کسب‌وکار (Owner controllers): Business, Menu, Product, Order, Reservation
- [x] Controller test (MockMvc) برای Admin
- [x] Integration test کامل با Testcontainers برای Auth flow

> ✅ معیار دمو فاز ۱ طبق سند اسپک (ثبت‌نام → پیدا کردن کسب‌وکار → سفارش → تغییر وضعیت توسط کسب‌وکار → پیگیری مشتری → رزرو میز → تایید ادمین) از نظر **بک‌اند** تکمیل شده است. باقی‌مانده‌ی اصلی، **فرانت‌اند** است.

---

## ⏳ باقی‌مانده‌های فاز ۱

- [ ] Unit test برای `AuthServiceImpl` و `UserServiceImpl`
- [ ] پیاده‌سازی فرانت‌اند (React + Vite + TS) برای هر سه پنل
- [ ] اتصال فرانت‌اند به API‌های بک‌اند
- [ ] تست End-to-End روی سناریوی کامل دمو

---

## 🔮 فازهای بعدی (خارج از اسکوپ فعلی)

### فاز ۲
- [ ] پرداخت آنلاین (درگاه پرداخت)
- [ ] نوتیفیکیشن واقعی (Push / SMS) — ماژول `notifications` فعلاً فقط اسکلت خالی داره
- [ ] نظرات و امتیازدهی (Reviews) — ماژول `reviews` فعلاً فقط اسکلت خالی داره

### فاز ۳
- [ ] کیف پول (Wallet)
- [ ] کد تخفیف
- [ ] سیستم Referral / Reward

### فاز ۴
- [ ] تبلیغات
- [ ] آمار و گزارش‌گیری پیشرفته
- [ ] PWA کامل (Offline support + Push)

---

## 📂 ساختار پروژه

```
foody/
├── foody-backend/                  # Spring Boot Modular Monolith
│   └── src/main/java/com/foody/
│       ├── auth/
│       ├── users/
│       ├── businesses/
│       ├── menus/
│       ├── products/
│       ├── orders/
│       ├── reservations/
│       ├── reviews/          (اسکلت، پیاده‌سازی نشده)
│       ├── notifications/    (اسکلت، پیاده‌سازی نشده)
│       ├── admin/
│       └── common/
└── foody-phase0-phase1-spec.md     # سند مرجع معماری
```

## 🚀 اجرا (Backend)

```bash
cd foody-backend

# با Testcontainers (نیاز به Docker، ساده‌ترین راه)
mvn test
mvn spring-boot:run

# یا با MySQL محلی
mvn spring-boot:run -Dspring-boot.run.profiles=local \
  -Dspring-boot.run.jvmArguments="-DDB_USERNAME=foody -DDB_PASSWORD=foody"
```

---
