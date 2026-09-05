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
- [x] کامپوننت مشترک `ErrorState` جایگزین fail سکوت‌شده روی ~۱۵ صفحه
- [x] سیستم رنگ برند سه‌تایی (ember/نارنجی، violet/بنفش، pistachio/سبز کله‌غازی) روی کارت‌های KPI، اکشن‌های سریع و آمار تاثیر اپ

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

- [x] `POST /api/business` — ثبت‌نام مستقل کسب‌وکار توسط خودِ owner (بدون نیاز به seed دستی) + گارد مسیر `RequireOwnerBusiness` سمت فرانت
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
- [x] Discover (هیرو + جستجو + فیلتر نوع کسب‌وکار) — در حال بازطراحی، پایین رو ببین
- [x] جزئیات کسب‌وکار (منو + سبد خرید کناری)
- [x] Checkout (Pickup/Delivery)
- [x] سفارش‌های من + جزئیات سفارش + لغو سفارش
- [x] رزرو جدید + رزروهای من + لغو رزرو
- [x] موجودی کیف پول در صفحه‌ی هوم (متصل به `GET /api/wallet`)

**پنل کسب‌وکار**
- [x] ثبت‌نام مستقل کسب‌وکار (`OwnerRegisterBusinessPage`) — owner دیگه نیازی به seed دستی نداره
- [x] پروفایل (مشاهده/ویرایش)
- [x] منو و محصولات (ساخت منو، افزودن محصول، تغییر موجودی)
- [x] سفارش‌ها (فیلتر وضعیت + دکمه‌های تغییر وضعیت طبق state machine بک‌اند)
- [x] رزروها (فیلتر تاریخ + تغییر وضعیت)
- [x] بازطراحی کامل داشبورد owner: هدر خوش‌آمد، کارت شعبه‌ها، کارت‌های KPI با روند صعودی/نزولی، اکشن‌های سریع، نظرات مشتری، آمار تاثیر اپ، بنر پروموشن، خلاصه‌ی ۳۰ روزه

**پنل ادمین**
- [x] داشبورد (کارت‌های آماری)
- [x] مدیریت کسب‌وکارها (فیلتر وضعیت + تایید/رد/معلق‌سازی)

**وضعیت فعلی**
- [x] `npm run build` بدون خطای TypeScript پاس می‌شه
- [x] دیپلوی تست روی Render (بک‌اند) + Vercel (فرانت‌اند) با موفقیت انجام شد — پایین رو ببین
- [ ] بازطراحی صفحه‌ی Discover/کشف در حال انجامه (طبق فیدبک: چیدمان فعلی نیاز به بازطراحی کامل داره)

---

## ⏳ باقی‌مانده‌های فاز ۱

- [x] پیاده‌سازی فرانت‌اند (React + Vite + TS) برای هر سه پنل
- [x] اتصال فرانت‌اند به API‌های بک‌اند
- [x] سیستم اعلان (Notification) پایه — لیست، تعداد نخوانده، علامت‌گذاری به‌عنوان خوانده‌شده (بک‌اند + فرانت)
- [x] دیپلوی تست عمومی (Render + Vercel) برای دمو به کارفرما
- [ ] بازطراحی صفحه‌ی Discover/کشف

---

## 🔮 فاز ۲ — کیف پول و درگیرسازی مشتری (بعدی، الان روش کار می‌کنیم)

> جهت محصولی جدید بر اساس یک رفرنس UI که برای پروژه خواسته شده (اپ‌محور،
> کیف پول در مرکز، اعتبار/ماموریت، اسکن QR). **رقابت/لیدربورد کافه‌ها عمداً
> از اسکوپ فعلی حذف شده** — شاید بعداً، نه الان.
>
> صفحه‌ی هوم مشتری طبق موکاپ کارفرما بازطراحی می‌شه: هدر (لوکیشن + اعلان +
> آواتار)، کارت کیف پول/اعتبار + بنر تخفیف اپ، ردیف اکشن‌های سریع (اسکن QR
> میز، رزرو میز، شارژ کیف پول، پیشنهادها)، بنر پروموشن، لیست کافه‌های نزدیک
> (با تصویر/تخفیف/امتیاز/فاصله)، و بخش «ماموریت‌های امروز».
> ⚠️ کیف پول یعنی فقط **شارژ دستی/داخلی** (بدون درگاه پرداخت واقعی — طبق
> تصمیم قفل‌شده‌ی فاز ۱)؛ اتصال درگاه واقعی هنوز فاز ۳ هست.

### بک‌اند
- [x] ماژول `wallet` — جدول موجودی هر کاربر + جدول تاریخچه‌ی تراکنش (شارژ/کسر/پاداش)، قفل خوش‌بینانه (optimistic locking) روی موجودی، `InsufficientBalanceException`، migration `V7__wallet.sql`
  - [x] `GET /api/wallet` (موجودی فعلی)
  - [x] `POST /api/wallet/topup` (شارژ دستی، بدون درگاه واقعی)
  - [x] `GET /api/wallet/transactions`
  - [ ] کسر خودکار از کیف پول حین ثبت سفارش (در انتظار تصمیم: کیف پول جایگزین `PAY_ON_DELIVERY` بشه یا کنارش باشه؟)
- [ ] ماژول `rewards` (اعتبار/ماموریت — Referral & Reward)
  - [ ] جدول تعریف ماموریت‌ها + جدول تکمیل ماموریت هر کاربر
  - [ ] دعوت دوست → کد دعوت اختصاصی هر کاربر + پاداش هنگام ثبت‌نام مدعو
  - [ ] اسکن QR میز → پاداش یک‌باره
  - [ ] اولین خرید → پاداش یک‌باره
  - [ ] `GET /api/rewards/missions` (لیست ماموریت‌های امروز + وضعیت هرکدوم)
- [ ] ماژول `qr` — اسکن QR میز/کافه
  - [ ] تولید کد QR برای هر میز توسط پنل owner (لینک/کد کوتاه به کسب‌وکار یا میز)
  - [ ] `POST /api/qr/scan` سمت مشتری → چک‌این میز + trigger ماموریت اسکن
- [ ] کد تخفیف (سطح اپ یا کسب‌وکار)
  - [ ] جدول discount/coupon + اعتبارسنجی حین ثبت سفارش
  - [ ] فیلد درصد تخفیف روی کارت کسب‌وکار برای نمایش در هوم/کشف
- [ ] فیلد امتیاز (rating) و فاصله (distance) روی کارت کسب‌وکار در هوم — امتیاز فعلاً دستی/seed (ماژول reviews واقعی فاز ۳ هست)، فاصله بر پایه‌ی lat/lng کسب‌وکار و موقعیت کاربر
- [ ] آپلود عکس کسب‌وکار — **توسط خودِ صاحب کافه از پنل owner، نه seed/placeholder ما** (فیچر آپلود لازم داره: ذخیره‌سازی فایل + endpoint)

### فرانت‌اند
- [ ] بازطراحی هدر صفحه‌ی هوم مشتری (انتخاب لوکیشن + زنگ اعلان + آواتار)
- [ ] کارت کیف پول/اعتبار + بنر تخفیف اپ (متصل به `GET /api/wallet`)
- [ ] ردیف اکشن سریع: اسکن QR میز، رزرو میز، شارژ کیف پول، پیشنهادها (رقابت کافه‌ها **حذف شده از این فاز**)
- [ ] بنر پروموشن (کاروسل ساده)
- [ ] بازطراحی کارت کسب‌وکار در هوم: تصویر، بج تخفیف، امتیاز، فاصله، دکمه «مشاهده منو»
- [ ] بخش «ماموریت‌های امروز» (متصل به `GET /api/rewards/missions`)
- [ ] صفحه‌ی کیف پول (موجودی + شارژ + تاریخچه‌ی تراکنش)
- [ ] صفحه‌ی اسکن QR (دوربین/ورود دستی کد میز)

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

## 🌐 دیپلوی تست (Live Demo)

برای دموی سریع به کارفرما، پروژه روی زیرساخت رایگان دیپلوی شده:

| بخش | سرویس | آدرس |
|---|---|---|
| بک‌اند | Render (Docker Web Service) | `https://foody-n39r.onrender.com` |
| فرانت‌اند | Vercel | `https://foody-eight-tau.vercel.app` |
| دیتابیس | Aiven (MySQL 8، پلن Free) | فقط داخلی، از طریق env vars |

**نکات مهم زیرساخت:**
- Render به‌صورت native فقط PostgreSQL/Redis می‌ده، نه MySQL — به همین خاطر دیتابیس روی **Aiven** (فری‌تایر همیشگی، بدون کارت اعتباری) میزبانی شده و از طریق `DB_HOST`/`DB_PORT`/`DB_NAME`/`DB_USERNAME`/`DB_PASSWORD` در Render وصل می‌شه.
- پروفایل Spring روی Render باید `SPRING_PROFILES_ACTIVE=prod` باشه (پروفایل جدید `prod` در `application.yml` که مقادیرش کاملاً از env میاد، شامل `sslMode=REQUIRED` چون Aiven اتصال رمزنگاری‌شده اجباری می‌کنه).
- `server.port` و `foody.jwt.secret` هم از env می‌خونن (`PORT`, `FOODY_JWT_SECRET`) — قبلاً هاردکد بودن که برای دیپلوی مشکل‌ساز بود.
- `FOODY_CORS_ALLOWED_ORIGINS` روی Render باید دقیقاً برابر آدرس ثابت Vercel باشه (نه آدرس هر deployment جدا).
- آپلود فایل فعلاً روی دیسک محلی سرویس ذخیره می‌شه (`FOODY_UPLOAD_DIR`) که روی Render **پرسیستنت نیست** — برای این تست مشکلی نداره، ولی اگه مشتری عکس آپلود کنه ممکنه بعد از ری‌استارت سرویس از بین بره. راه‌حل بلندمدت: Render Persistent Disk یا S3-compatible storage (فاز ۲).
- Render پلن Free بعد از مدتی بی‌فعالیتی sleep می‌ره؛ اولین درخواست بعدش چند ثانیه کند خواهد بود.
- هر `git push` به `main` هر دو سرویس رو خودکار دوباره دیپلوی می‌کنه (Render فقط اگه تغییر داخل `foody-backend/` باشه؛ Vercel برای هر تغییری، مگر Ignored Build Step تنظیم بشه).

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
