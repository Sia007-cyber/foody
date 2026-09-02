import { QueryClientProvider } from "@tanstack/react-query";
import { BrowserRouter, Route, Routes } from "react-router-dom";
import { queryClient } from "./lib/queryClient";
import { AuthProvider } from "./features/auth/AuthContext";
import { CartProvider } from "./features/cart/CartContext";
import { ToastProvider } from "./components/Feedback";
import { PublicLayout } from "./components/PublicLayout";
import { RequireAuth } from "./components/RequireAuth";

import { LoginPage } from "./features/auth/LoginPage";
import { RegisterPage } from "./features/auth/RegisterPage";
import { DiscoverPage } from "./features/discover/DiscoverPage";
import { BusinessDetailPage } from "./features/business-detail/BusinessDetailPage";
import { CheckoutPage } from "./features/orders/CheckoutPage";
import { MyOrdersPage } from "./features/orders/MyOrdersPage";
import { OrderDetailPage } from "./features/orders/OrderDetailPage";
import { MyReservationsPage } from "./features/reservations/MyReservationsPage";
import { NewReservationPage } from "./features/reservations/NewReservationPage";
import { OwnerDashboardPage } from "./features/owner/OwnerDashboardPage";
import { OwnerRegisterBusinessPage } from "./features/owner/OwnerRegisterBusinessPage";
import { OwnerProfilePage } from "./features/owner/OwnerProfilePage";
import { OwnerMenusPage } from "./features/owner/OwnerMenusPage";
import { OwnerOrdersPage } from "./features/owner/OwnerOrdersPage";
import { OwnerReservationsPage } from "./features/owner/OwnerReservationsPage";
import { RequireOwnerBusiness } from "./components/RequireOwnerBusiness";
import { ownerNavItems } from "./features/owner/ownerNav";
import { AdminDashboardPage } from "./features/admin/AdminDashboardPage";
import { AdminBusinessesPage } from "./features/admin/AdminBusinessesPage";
import { ComingSoonFeaturePage } from "./pages/ComingSoonFeaturePage";
import { adminNavItems } from "./features/admin/adminNav";
import {
  UsersIcon,
  ReceiptIcon,
  WalletIcon,
  MegaphoneIcon,
  ChatIcon,
  ShieldIcon,
  ChartIcon,
  SettingsIcon,
} from "./components/icons";
import { NotFoundPage } from "./pages/NotFoundPage";

export default function App() {
  return (
    <QueryClientProvider client={queryClient}>
      <BrowserRouter>
        <AuthProvider>
          <CartProvider>
            <ToastProvider>
              <Routes>
                <Route element={<PublicLayout />}>
                  <Route path="/" element={<DiscoverPage />} />
                  <Route path="/businesses/:id" element={<BusinessDetailPage />} />
                  <Route path="/login" element={<LoginPage />} />
                  <Route path="/register" element={<RegisterPage />} />

                  <Route element={<RequireAuth roles={["CUSTOMER", "BUSINESS_OWNER"]} />}>
                    <Route path="/checkout" element={<CheckoutPage />} />
                    <Route path="/orders" element={<MyOrdersPage />} />
                    <Route path="/orders/:id" element={<OrderDetailPage />} />
                    <Route path="/reservations" element={<MyReservationsPage />} />
                    <Route path="/businesses/:id/reserve" element={<NewReservationPage />} />
                  </Route>
                </Route>

                <Route element={<RequireAuth roles={["BUSINESS_OWNER"]} />}>
                  <Route path="/business/register" element={<OwnerRegisterBusinessPage />} />

                  <Route element={<RequireOwnerBusiness />}>
                    <Route path="/business" element={<OwnerDashboardPage />} />
                    <Route path="/business/profile" element={<OwnerProfilePage />} />
                    <Route path="/business/menus" element={<OwnerMenusPage />} />
                    <Route path="/business/orders" element={<OwnerOrdersPage />} />
                    <Route path="/business/reservations" element={<OwnerReservationsPage />} />
                    <Route
                      path="/business/discounts"
                      element={
                        <ComingSoonFeaturePage
                          navItems={ownerNavItems}
                          title="تخفیف‌ها"
                          icon={<MegaphoneIcon size={40} />}
                          description="ایجاد و مدیریت کدهای تخفیف برای مشتری‌های کافه‌ات به‌زودی اضافه خواهد شد."
                        />
                      }
                    />
                    <Route
                      path="/business/messages"
                      element={
                        <ComingSoonFeaturePage
                          navItems={ownerNavItems}
                          title="پیام‌ها"
                          icon={<ChatIcon size={40} />}
                          description="ارسال پیام به مشتری‌ها و مشاهده‌ی گفتگوها به‌زودی اضافه خواهد شد."
                        />
                      }
                    />
                    <Route
                      path="/business/reports"
                      element={
                        <ComingSoonFeaturePage
                          navItems={ownerNavItems}
                          title="گزارش فروش"
                          icon={<ChartIcon size={40} />}
                          description="گزارش‌های تحلیلی فروش و عملکرد کسب‌وکارت به‌زودی اضافه خواهد شد."
                        />
                      }
                    />
                    <Route
                      path="/business/manual-order"
                      element={
                        <ComingSoonFeaturePage
                          navItems={ownerNavItems}
                          title="ثبت سفارش دستی"
                          icon={<ReceiptIcon size={40} />}
                          description="ثبت سفارش حضوری برای مشتری‌ها به‌زودی اضافه خواهد شد."
                        />
                      }
                    />
                  </Route>
                </Route>

                <Route element={<RequireAuth roles={["ADMIN"]} />}>
                  <Route path="/admin" element={<AdminDashboardPage />} />
                  <Route path="/admin/businesses" element={<AdminBusinessesPage />} />
                  <Route
                    path="/admin/users"
                    element={
                      <ComingSoonFeaturePage
                        navItems={adminNavItems}
                        title="کاربران"
                        icon={<UsersIcon size={40} />}
                        description="فهرست کاربران، جزئیات پروفایل و امکان تعلیق/فعال‌سازی حساب از این بخش به‌زودی اضافه خواهد شد."
                      />
                    }
                  />
                  <Route
                    path="/admin/orders"
                    element={
                      <ComingSoonFeaturePage
                        navItems={adminNavItems}
                        title="سفارش‌ها"
                        icon={<ReceiptIcon size={40} />}
                        description="نمای کلی سفارش‌های همه‌ی کافه‌ها برای ادمین به‌زودی اضافه خواهد شد."
                      />
                    }
                  />
                  <Route
                    path="/admin/transactions"
                    element={
                      <ComingSoonFeaturePage
                        navItems={adminNavItems}
                        title="تراکنش‌ها"
                        icon={<WalletIcon size={40} />}
                        description="پرداخت آنلاین در فاز فعلی پیاده‌سازی نشده؛ این بخش با اضافه‌شدن پرداخت آنلاین فعال می‌شود."
                      />
                    }
                  />
                  <Route
                    path="/admin/ads"
                    element={
                      <ComingSoonFeaturePage
                        navItems={adminNavItems}
                        title="تبلیغات"
                        icon={<MegaphoneIcon size={40} />}
                        description="ابزارهای تبلیغاتی برای کافه‌ها (تبلیغ در صفحه اصلی، پوش نوتیفیکیشن و غیره) به‌زودی اضافه خواهد شد."
                      />
                    }
                  />
                  <Route
                    path="/admin/reviews"
                    element={
                      <ComingSoonFeaturePage
                        navItems={adminNavItems}
                        title="نظرات و بازخوردها"
                        icon={<ChatIcon size={40} />}
                        description="مدیریت و پاسخ‌دهی به نظرات کاربران به‌زودی اضافه خواهد شد."
                      />
                    }
                  />
                  <Route
                    path="/admin/violations"
                    element={
                      <ComingSoonFeaturePage
                        navItems={adminNavItems}
                        title="گزارشات تخلف"
                        icon={<ShieldIcon size={40} />}
                        description="بررسی گزارش‌های تخلف ثبت‌شده توسط کاربران به‌زودی اضافه خواهد شد."
                      />
                    }
                  />
                  <Route
                    path="/admin/reports"
                    element={
                      <ComingSoonFeaturePage
                        navItems={adminNavItems}
                        title="گزارش‌ها"
                        icon={<ChartIcon size={40} />}
                        description="گزارش‌های تحلیلی و آماری پلتفرم به‌زودی اضافه خواهد شد."
                      />
                    }
                  />
                  <Route
                    path="/admin/settings"
                    element={
                      <ComingSoonFeaturePage
                        navItems={adminNavItems}
                        title="تنظیمات"
                        icon={<SettingsIcon size={40} />}
                        description="تنظیمات عمومی پلتفرم به‌زودی اضافه خواهد شد."
                      />
                    }
                  />
                </Route>

                <Route path="*" element={<NotFoundPage />} />
              </Routes>
            </ToastProvider>
          </CartProvider>
        </AuthProvider>
      </BrowserRouter>
    </QueryClientProvider>
  );
}
