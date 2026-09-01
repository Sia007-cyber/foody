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
import { OwnerProfilePage } from "./features/owner/OwnerProfilePage";
import { OwnerMenusPage } from "./features/owner/OwnerMenusPage";
import { OwnerOrdersPage } from "./features/owner/OwnerOrdersPage";
import { OwnerReservationsPage } from "./features/owner/OwnerReservationsPage";
import { AdminDashboardPage } from "./features/admin/AdminDashboardPage";
import { AdminBusinessesPage } from "./features/admin/AdminBusinessesPage";
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

                  <Route element={<RequireAuth roles={["CUSTOMER"]} />}>
                    <Route path="/checkout" element={<CheckoutPage />} />
                    <Route path="/orders" element={<MyOrdersPage />} />
                    <Route path="/orders/:id" element={<OrderDetailPage />} />
                    <Route path="/reservations" element={<MyReservationsPage />} />
                    <Route path="/businesses/:id/reserve" element={<NewReservationPage />} />
                  </Route>
                </Route>

                <Route element={<RequireAuth roles={["BUSINESS_OWNER"]} />}>
                  <Route path="/business" element={<OwnerProfilePage />} />
                  <Route path="/business/menus" element={<OwnerMenusPage />} />
                  <Route path="/business/orders" element={<OwnerOrdersPage />} />
                  <Route path="/business/reservations" element={<OwnerReservationsPage />} />
                </Route>

                <Route element={<RequireAuth roles={["ADMIN"]} />}>
                  <Route path="/admin" element={<AdminDashboardPage />} />
                  <Route path="/admin/businesses" element={<AdminBusinessesPage />} />
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
