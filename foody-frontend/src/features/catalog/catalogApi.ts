import { apiRequest } from "../../lib/api";
import type { Menu, Product } from "../../types/api";

export const menuApi = {
  listForBusiness: (businessId: number) =>
    apiRequest<Menu[]>(`/api/businesses/${businessId}/menus`, { auth: false }),

  create: (payload: { name: string; displayOrder?: number }) =>
    apiRequest<Menu>("/api/business/menus", { method: "POST", body: payload }),

  update: (id: number, payload: { name: string }) =>
    apiRequest<Menu>(`/api/business/menus/${id}`, { method: "PATCH", body: payload }),

  remove: (id: number) => apiRequest<void>(`/api/business/menus/${id}`, { method: "DELETE" }),
};

export const productApi = {
  listForMenu: (menuId: number) =>
    apiRequest<Product[]>(`/api/menus/${menuId}/products`, { auth: false }),

  getById: (id: number) => apiRequest<Product>(`/api/products/${id}`, { auth: false }),

  create: (payload: {
    menuId: number;
    name: string;
    description?: string;
    price: number;
    imageUrl?: string;
    displayOrder?: number;
  }) => apiRequest<Product>("/api/business/products", { method: "POST", body: payload }),

  update: (
    id: number,
    payload: Partial<{
      name: string;
      description: string;
      price: number;
      imageUrl: string;
      isAvailable: boolean;
      displayOrder: number;
    }>
  ) => apiRequest<Product>(`/api/business/products/${id}`, { method: "PATCH", body: payload }),

  remove: (id: number) => apiRequest<void>(`/api/business/products/${id}`, { method: "DELETE" }),
};
