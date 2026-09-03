import { useState, type FormEvent } from "react";
import { useMutation, useQuery, useQueryClient } from "@tanstack/react-query";
import { businessApi } from "../businesses/businessApi";
import { menuApi, productApi } from "../catalog/catalogApi";
import { DashboardShell } from "../../components/DashboardShell";
import { Input, Textarea } from "../../components/Field";
import { Button } from "../../components/Button";
import { Switch, PageSpinner, EmptyState } from "../../components/Controls";
import { ConfirmDialog, useToast, errorMessage } from "../../components/Feedback";
import { formatToman } from "../../lib/format";
import { ownerNavItems } from "./ownerNav";
import {
  PencilIcon,
  TrashIcon,
  CheckIcon,
  CloseIcon,
  ImagePlaceholderIcon,
} from "../../components/icons";
import type { Menu, Product } from "../../types/api";

export function OwnerMenusPage() {
  const { data: business } = useQuery({ queryKey: ["business", "profile"], queryFn: businessApi.myProfile });

  return (
    <DashboardShell navItems={ownerNavItems} title="منو و محصولات">
      {!business ? (
        <PageSpinner />
      ) : (
        <div style={{ display: "flex", flexDirection: "column", gap: 32 }}>
          <NewMenuForm businessId={business.id} />
          <MenuList businessId={business.id} />
        </div>
      )}
    </DashboardShell>
  );
}

function NewMenuForm({ businessId }: { businessId: number }) {
  const queryClient = useQueryClient();
  const { notify } = useToast();
  const [name, setName] = useState("");

  const mutation = useMutation({
    mutationFn: () => menuApi.create({ name }),
    onSuccess: () => {
      queryClient.invalidateQueries({ queryKey: ["menus", businessId] });
      setName("");
      notify("منو ساخته شد", "ok");
    },
    onError: (err) => notify(errorMessage(err), "danger"),
  });

  function handleSubmit(e: FormEvent) {
    e.preventDefault();
    if (!name.trim()) return;
    mutation.mutate();
  }

  return (
    <form onSubmit={handleSubmit} style={{ display: "flex", gap: 10, alignItems: "flex-end", maxWidth: 420 }}>
      <div style={{ flex: 1 }}>
        <Input label="منوی جدید" placeholder="مثلاً منوی اصلی" value={name} onChange={(e) => setName(e.target.value)} />
      </div>
      <Button type="submit" loading={mutation.isPending}>
        افزودن
      </Button>
    </form>
  );
}

function MenuList({ businessId }: { businessId: number }) {
  const { data: menus, isLoading } = useQuery({
    queryKey: ["menus", businessId],
    queryFn: () => menuApi.listForBusiness(businessId),
  });

  if (isLoading) return <PageSpinner />;
  if (!menus || menus.length === 0) return <EmptyState title="هنوز منویی نساختی" />;

  return (
    <div style={{ display: "flex", flexDirection: "column", gap: 28 }}>
      {menus.map((menu) => (
        <MenuBlock key={menu.id} menu={menu} businessId={businessId} />
      ))}
    </div>
  );
}

function MenuBlock({ menu, businessId }: { menu: Menu; businessId: number }) {
  const [showForm, setShowForm] = useState(false);
  const [editingTitle, setEditingTitle] = useState(false);
  const [titleValue, setTitleValue] = useState(menu.name);
  const [confirmDeleteMenu, setConfirmDeleteMenu] = useState(false);
  const [editingProduct, setEditingProduct] = useState<Product | null>(null);
  const [deletingProduct, setDeletingProduct] = useState<Product | null>(null);

  const { data: products, isLoading } = useQuery({
    queryKey: ["products", "menu", menu.id],
    queryFn: () => productApi.listForMenu(menu.id),
  });
  const queryClient = useQueryClient();
  const { notify } = useToast();

  const renameMenu = useMutation({
    mutationFn: () => menuApi.update(menu.id, { name: titleValue }),
    onSuccess: () => {
      queryClient.invalidateQueries({ queryKey: ["menus", businessId] });
      setEditingTitle(false);
      notify("نام منو به‌روزرسانی شد", "ok");
    },
    onError: (err) => notify(errorMessage(err), "danger"),
  });

  const deleteMenu = useMutation({
    mutationFn: () => menuApi.remove(menu.id),
    onSuccess: () => {
      queryClient.invalidateQueries({ queryKey: ["menus", businessId] });
      notify("منو حذف شد", "ok");
    },
    onError: (err) => {
      notify(errorMessage(err), "danger");
      setConfirmDeleteMenu(false);
    },
  });

  const toggleAvailability = useMutation({
    mutationFn: ({ id, isAvailable }: { id: number; isAvailable: boolean }) =>
      productApi.update(id, { isAvailable }),
    onSuccess: () => queryClient.invalidateQueries({ queryKey: ["products", "menu", menu.id] }),
    onError: (err) => notify(errorMessage(err), "danger"),
  });

  const deleteProduct = useMutation({
    mutationFn: (id: number) => productApi.remove(id),
    onSuccess: () => {
      queryClient.invalidateQueries({ queryKey: ["products", "menu", menu.id] });
      notify("محصول حذف شد", "ok");
      setDeletingProduct(null);
    },
    onError: (err) => {
      notify(errorMessage(err), "danger");
      setDeletingProduct(null);
    },
  });

  return (
    <div>
      <div style={{ display: "flex", alignItems: "center", justifyContent: "space-between", marginBottom: 12 }}>
        {editingTitle ? (
          <div className="menu-title-edit">
            <Input value={titleValue} onChange={(e) => setTitleValue(e.target.value)} autoFocus />
            <button
              type="button"
              className="icon-btn"
              aria-label="ذخیره نام منو"
              onClick={() => titleValue.trim() && renameMenu.mutate()}
            >
              <CheckIcon size={16} />
            </button>
            <button
              type="button"
              className="icon-btn"
              aria-label="انصراف"
              onClick={() => {
                setTitleValue(menu.name);
                setEditingTitle(false);
              }}
            >
              <CloseIcon size={16} />
            </button>
          </div>
        ) : (
          <div className="menu-title-row">
            <h3 style={{ fontSize: 17, fontWeight: 700 }}>{menu.name}</h3>
            <button type="button" className="icon-btn" aria-label="ویرایش نام منو" onClick={() => setEditingTitle(true)}>
              <PencilIcon size={15} />
            </button>
            <button
              type="button"
              className="icon-btn danger"
              aria-label="حذف منو"
              onClick={() => setConfirmDeleteMenu(true)}
            >
              <TrashIcon size={15} />
            </button>
          </div>
        )}
        <Button variant="secondary" size="sm" onClick={() => setShowForm((s) => !s)}>
          {showForm ? "بستن" : "افزودن محصول"}
        </Button>
      </div>

      {showForm && <NewProductForm menuId={menu.id} onDone={() => setShowForm(false)} />}

      {isLoading ? (
        <PageSpinner />
      ) : !products || products.length === 0 ? (
        <EmptyState title="محصولی ثبت نشده" />
      ) : (
        <div className="list-group">
          {products.map((p) => (
            <div key={p.id} className="product-row">
              {p.imageUrl ? (
                <img src={p.imageUrl} alt={p.name} className="product-thumb" />
              ) : (
                <span className="product-thumb-placeholder">
                  <ImagePlaceholderIcon size={20} />
                </span>
              )}

              <div className="product-row-main">
                <span className="list-row-title">{p.name}</span>
                {p.description && <span className="product-row-desc">{p.description}</span>}
                <span className="list-row-sub">{formatToman(p.price)}</span>
              </div>

              <div className="list-row-actions">
                <Switch
                  checked={p.isAvailable}
                  disabled={toggleAvailability.isPending}
                  onChange={() => toggleAvailability.mutate({ id: p.id, isAvailable: !p.isAvailable })}
                  label={p.isAvailable ? "موجود" : "ناموجود"}
                />
                <button type="button" className="icon-btn" aria-label="ویرایش محصول" onClick={() => setEditingProduct(p)}>
                  <PencilIcon size={15} />
                </button>
                <button
                  type="button"
                  className="icon-btn danger"
                  aria-label="حذف محصول"
                  onClick={() => setDeletingProduct(p)}
                >
                  <TrashIcon size={15} />
                </button>
              </div>
            </div>
          ))}
        </div>
      )}

      {editingProduct && (
        <EditProductModal
          product={editingProduct}
          menuId={menu.id}
          onClose={() => setEditingProduct(null)}
        />
      )}

      {deletingProduct && (
        <ConfirmDialog
          title="حذف محصول"
          description={`«${deletingProduct.name}» برای همیشه حذف می‌شود.`}
          confirmLabel="حذف"
          danger
          loading={deleteProduct.isPending}
          onCancel={() => setDeletingProduct(null)}
          onConfirm={() => deleteProduct.mutate(deletingProduct.id)}
        />
      )}

      {confirmDeleteMenu && (
        <ConfirmDialog
          title="حذف منو"
          description={`«${menu.name}» و همه‌ی محصولات داخلش برای همیشه حذف می‌شوند.`}
          confirmLabel="حذف"
          danger
          loading={deleteMenu.isPending}
          onCancel={() => setConfirmDeleteMenu(false)}
          onConfirm={() => deleteMenu.mutate()}
        />
      )}
    </div>
  );
}

function NewProductForm({ menuId, onDone }: { menuId: number; onDone: () => void }) {
  const queryClient = useQueryClient();
  const { notify } = useToast();
  const [name, setName] = useState("");
  const [description, setDescription] = useState("");
  const [price, setPrice] = useState("");
  const [imageUrl, setImageUrl] = useState("");

  const mutation = useMutation({
    mutationFn: () =>
      productApi.create({
        menuId,
        name,
        description: description || undefined,
        price: Number(price),
        imageUrl: imageUrl || undefined,
      }),
    onSuccess: () => {
      queryClient.invalidateQueries({ queryKey: ["products", "menu", menuId] });
      notify("محصول اضافه شد", "ok");
      onDone();
    },
    onError: (err) => notify(errorMessage(err), "danger"),
  });

  function handleSubmit(e: FormEvent) {
    e.preventDefault();
    mutation.mutate();
  }

  return (
    <form
      onSubmit={handleSubmit}
      className="list-group"
      style={{ padding: 16, display: "flex", flexDirection: "column", gap: 10, marginBottom: 14 }}
    >
      <Input label="نام محصول" required value={name} onChange={(e) => setName(e.target.value)} />
      <Textarea label="توضیحات (اختیاری)" value={description} onChange={(e) => setDescription(e.target.value)} />
      <Input
        label="آدرس عکس (اختیاری)"
        placeholder="https://..."
        dir="ltr"
        value={imageUrl}
        onChange={(e) => setImageUrl(e.target.value)}
      />
      <Input
        label="قیمت (تومان)"
        type="number"
        min={0}
        required
        value={price}
        onChange={(e) => setPrice(e.target.value)}
      />
      <Button type="submit" loading={mutation.isPending}>
        ثبت محصول
      </Button>
    </form>
  );
}

function EditProductModal({
  product,
  menuId,
  onClose,
}: {
  product: Product;
  menuId: number;
  onClose: () => void;
}) {
  const queryClient = useQueryClient();
  const { notify } = useToast();
  const [name, setName] = useState(product.name);
  const [description, setDescription] = useState(product.description ?? "");
  const [price, setPrice] = useState(String(product.price));
  const [imageUrl, setImageUrl] = useState(product.imageUrl ?? "");
  const [isAvailable, setIsAvailable] = useState(product.isAvailable);

  const mutation = useMutation({
    mutationFn: () =>
      productApi.update(product.id, {
        name,
        description,
        price: Number(price),
        imageUrl,
        isAvailable,
      }),
    onSuccess: () => {
      queryClient.invalidateQueries({ queryKey: ["products", "menu", menuId] });
      notify("محصول به‌روزرسانی شد", "ok");
      onClose();
    },
    onError: (err) => notify(errorMessage(err), "danger"),
  });

  function handleSubmit(e: FormEvent) {
    e.preventDefault();
    mutation.mutate();
  }

  return (
    <div className="modal-backdrop" onClick={onClose}>
      <form
        onSubmit={handleSubmit}
        className="modal-panel"
        onClick={(e) => e.stopPropagation()}
        style={{ maxWidth: 460 }}
      >
        <h3 style={{ fontSize: 18, fontWeight: 700 }}>ویرایش محصول</h3>

        <Input label="نام محصول" required value={name} onChange={(e) => setName(e.target.value)} />
        <Textarea label="توضیحات" value={description} onChange={(e) => setDescription(e.target.value)} />
        <Input
          label="آدرس عکس"
          placeholder="https://..."
          dir="ltr"
          value={imageUrl}
          onChange={(e) => setImageUrl(e.target.value)}
        />
        <Input
          label="قیمت (تومان)"
          type="number"
          min={0}
          required
          value={price}
          onChange={(e) => setPrice(e.target.value)}
        />
        <Switch checked={isAvailable} onChange={() => setIsAvailable((v) => !v)} label={isAvailable ? "موجود" : "ناموجود"} />

        <div className="modal-actions">
          <Button type="button" variant="secondary" onClick={onClose} disabled={mutation.isPending}>
            انصراف
          </Button>
          <Button type="submit" loading={mutation.isPending}>
            ذخیره
          </Button>
        </div>
      </form>
    </div>
  );
}
