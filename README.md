# 🍔 Foody — پلتفرم سفارش غذا و رزرو کافه/رستوران

پلتفرم Foody یک سیستم سفارش غذا و مدیریت کسب‌وکارهای غذایی (کافه/رستوران/فست‌فود) است که از سه پنل **مشتری**، **کسب‌وکار** و **ادمین** و سه روش تحویل **Pickup**، **Delivery** و **Dine-in/Reservation** پشتیبانی می‌کند.

> این README به‌صورت چک‌لیست نوشته شده تا پیشرفت پروژه قدم‌به‌قدم پیگیری بشه. هر فیچر که تکمیل و روی گیت‌هاب پوش بشه، تیک می‌خوره.

---

## 🧱 تکنولوژی‌ها

- **Backend:** Spring Boot (Modular Monolith) + Maven + Java 21
- **Auth:** Spring Security + JWT (Access + Refresh Token)
- **Database:** MySQL + Flyway Migrations
- **Frontend:** React + Vite + TypeScript + React Router + TanStack Query — طراحی سبک اپل (Vazirmatn، RTL)
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
- [x] تنظیمات CORS (`foody.cors.allowed-origins`) — بدونش فرانت‌اند از origin جدا (مثلاً `localhost:5173`) نمی‌تونست به بک‌اند وصل بشه
- [x] Seed یک اکانت ADMIN (`V5__seed_admin_user.sql`) برای تست پنل ادمین — چون مسیر ثبت‌نام فقط CUSTOMER می‌سازه
- [x] اسکلت فرانت‌اند (Vite + React + TS + Router + TanStack Query)

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
- [x] Unit test (Mockito) برای سرویس‌های Auth, Users, Business, Menu, Product, Order, Reservation, Admin
- [x] Controller test (MockMvc) برای همه‌ی endpoint های مشتری: Business, Menu, Product, Order, Reservation, Users
- [x] Controller test (MockMvc) برای همه‌ی endpoint های پنل کسب‌وکار (Owner controllers): Business, Menu, Product, Order, Reservation
- [x] Controller test (MockMvc) برای Admin
- [x] Integration test کامل با Testcontainers برای Auth flow

> ✅ معیار دمو فاز ۱ طبق سند اسپک (ثبت‌نام → پیدا کردن کسب‌وکار → سفارش → تغییر وضعیت توسط کسب‌وکار → پیگیری مشتری → رزرو میز → تایید ادمین) از نظر **بک‌اند** تکمیل شده است.

---

## 🎨 فرانت‌اند (Vite + React + TS)

طراحی: سبک اپل (سفید + رنگ برند ember، فونت Vazirmatn، بدون عکس استوک، کاملاً RTL).
جزئیات پالت/اصول طراحی در `foody-frontend/DESIGN.md`.

**زیرساخت**
- [x] اسکلت Vite + React + TS + React Router + TanStack Query
- [x] کلاینت API با تزریق JWT + رفرش خودکار توکن روی ۴۰۱
- [x] Context‌های Auth / Cart / Toast + گارد مسیر بر اساس نقش (`RequireAuth`)
- [x] کامپوننت‌های مشترک: Button, Field, Badge (پیل وضعیت هر ۴ دامنه), Segmented, ConfirmDialog, DashboardShell

**پنل مشتری**
- [x] ورود / ثبت‌نام
- [x] Discover (هیرو + جستجو + فیلتر نوع کسب‌وکار)
- [x] جزئیات کسب‌وکار (منو + سبد خرید کناری)
- [x] Checkout (Pickup/Delivery)
- [x] سفارش‌های من + جزئیات سفارش + لغو سفارش
- [x] رزرو جدید + رزروهای من + لغو رزرو

**پنل کسب‌وکار**
- [x] پروفایل (مشاهده/ویرایش)
- [x] منو و محصولات (ساخت منو، افزودن محصول، تغییر موجودی)
- [x] سفارش‌ها (فیلتر وضعیت + دکمه‌های تغییر وضعیت طبق state machine بک‌اند)
- [x] رزروها (فیلتر تاریخ + تغییر وضعیت)

**پنل ادمین**
- [x] داشبورد (کارت‌های آماری)
- [x] مدیریت کسب‌وکارها (فیلتر وضعیت + تایید/رد/معلق‌سازی)

**وضعیت فعلی**
- [x] `npm run build` بدون خطای TypeScript پاس می‌شه
- [ ] تست End-to-End دستی روی سناریوی کامل دمو در حال انجامه — چند مشکل محیطی جزئی (پورت دیتابیس، پروفایل Spring) در حال رفع شدنه، نه باگ فرانت‌اند/بک‌اند

---

## ⏳ باقی‌مانده‌های فاز ۱

- [x] پیاده‌سازی فرانت‌اند (React + Vite + TS) برای هر سه پنل
- [x] اتصال فرانت‌اند به API‌های بک‌اند
- [x] سیستم اعلان (Notification) پایه — لیست، تعداد نخوانده، علامت‌گذاری به‌عنوان خوانده‌شده (بک‌اند + فرانت)
- [ ] تست End-to-End روی سناریوی کامل دمو (در حال انجام — رفع مشکلات محیط لوکال)

---

## 🔮 فاز ۲ — کیف پول و درگیرسازی مشتری (بعدی، الان روش کار می‌کنیم)

> جهت محصولی جدید بر اساس یک رفرنس UI که برای پروژه خواسته شده (اپ‌محور،
> کیف پول در مرکز، اعتبار/ماموریت، اسکن QR). **رقابت/لیدربورد کافه‌ها عمداً
> از اسکوپ فعلی حذف شده** — شاید بعداً، نه الان.

- [ ] کیف پول (Wallet) — موجودی کاربر، شارژ، کسر موجودی حین سفارش، تاریخچه‌ی تراکنش
- [ ] سیستم اعتبار/ماموریت (Referral & Reward) — دعوت دوست، اسکن QR، اولین خرید → اعتبار
- [ ] اسکن QR میز/کافه — تولید QR توسط کسب‌وکار، اسکن توسط مشتری
- [ ] کد تخفیف (سطح اپ یا کسب‌وکار)
- [ ] آپلود عکس کسب‌وکار — **توسط خودِ صاحب کافه از پنل owner، نه seed/placeholder ما** (فیچر آپلود لازم داره: ذخیره‌سازی فایل + endpoint)

---

## 🔮 فازهای بعدی (خارج از اسکوپ فعلی)

### فاز ۳
- [ ] پرداخت آنلاین (درگاه پرداخت)
- [ ] نوتیفیکیشن Push / SMS واقعی (فعلاً فقط اعلان داخل‌اپلیکیشنی داریم)
- [ ] نظرات و امتیازدهی (Reviews) — ماژول `reviews` فعلاً فقط اسکلت خالی داره

### فاز ۴
- [ ] تبلیغات
- [ ] رقابت / لیدربورد کافه‌ها (عمداً به تعویق افتاده، نه فراموش‌شده)
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
├── foody-frontend/                 # Vite + React + TS
│   ├── DESIGN.md                   # پالت رنگ، تایپوگرافی، اصول طراحی
│   ├── SETUP.md                    # راهنمای نصب و اکانت‌های تست
│   └── src/
│       ├── components/             # UI مشترک (Button, Field, Badge, DashboardShell, ...)
│       ├── features/               # auth, discover, business-detail, cart, orders,
│       │                           # reservations, owner, admin — هرکدوم api+pages خودشون
│       ├── lib/                    # کلاینت API، فرمت‌بندی، queryClient
│       └── types/                  # تایپ‌های TS منطبق با DTOهای بک‌اند
└── foody-phase0-phase1-spec.md     # سند مرجع معماری
```

## 🚀 اجرا (Backend)

بک‌اند به یه MySQL واقعی نیاز داره. دو راه:

**با Testcontainers** (نیاز به Docker، پروفایل پیش‌فرض `tc` — فقط برای `mvn test`، نه برای اجرای واقعی اپ):
```bash
cd foody-backend
mvn test
```

**با MySQL محلی/داکر** (برای اجرای واقعی `spring-boot:run`، پروفایل `local`):
```bash
# یک بار: یه MySQL بساز (اگه پورت 3306 قبلاً اشغاله، پورت دیگه‌ای مثل 3309 بگیر)
docker run -d --name foody-mysql \
  -e MYSQL_ROOT_PASSWORD=root -e MYSQL_DATABASE=foody \
  -e MYSQL_USER=foody -e MYSQL_PASSWORD=foody \
  -p 3309:3306 mysql:8.4

cd foody-backend
SPRING_PROFILES_ACTIVE=local \
SPRING_DATASOURCE_URL="jdbc:mysql://localhost:3309/foody?createDatabaseIfNotExist=true&serverTimezone=UTC&useUnicode=true&characterEncoding=utf8" \
mvn spring-boot:run
```
روی `http://localhost:8080` بالا میاد. Flyway خودکار همه‌ی migration‌ها رو اجرا می‌کنه
(شامل seed اکانت owner و admin — پایین رو ببین).

### اکانت‌های تست (seed شده)
| نقش | ایمیل | رمز |
|---|---|---|
| Business Owner | `owner@foody.test` | `password123` |
| Admin | `admin@foody.test` | `password123` |

## 🚀 اجرا (Frontend)

```bash
cd foody-frontend
npm install
cp .env.example .env
npm run dev
```
روی `http://localhost:5173` بالا میاد. `VITE_API_BASE_URL` تو `.env` باید به آدرس
بک‌اند اشاره کنه (پیش‌فرض `http://localhost:8080`).

---
