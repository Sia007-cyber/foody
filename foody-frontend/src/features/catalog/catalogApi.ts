import { apiRequest, apiUpload } from "../../lib/api";
import type { Menu, Product } from "../../types/api";

export const menuApi = {
  listForBusiness: (businessId: number) =>
    apiRequest<Menu[]>(`/api/businesses/${businessId}/menus`, { auth: false }),

  // Business panel: the owner's own menus, regardless of the business's approval
  // status. listForBusiness above 404s until the business is APPROVED, which made
  // freshly-created menus disappear from the owner's dashboard.
  listMine: () => apiRequest<Menu[]>("/api/business/menus"),

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

  // Uploads a product photo from the owner's device and returns its public URL;
  // the caller still needs to send that URL as imageUrl on create/update. Reuses
  // the same generic image-upload endpoint the profile picture and business
  // cover photo use.
  uploadImage: (file: File) => apiUpload<{ url: string }>("/api/uploads/image", file),

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
