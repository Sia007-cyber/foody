import { useRef, useState, type ChangeEvent, type FormEvent } from "react";
import { useMutation, useQuery, useQueryClient } from "@tanstack/react-query";
import { businessApi } from "../businesses/businessApi";
import { menuApi, productApi } from "../catalog/catalogApi";
import { DashboardShell } from "../../components/DashboardShell";
import { Input, Textarea } from "../../components/Field";
import { Button } from "../../components/Button";
import { Switch, PageSpinner, EmptyState } from "../../components/Controls";
import { ConfirmDialog, useToast, errorMessage } from "../../components/Feedback";
import { formatToman } from "../../lib/format";
import { resolveMediaUrl } from "../../lib/api";
import { ownerNavItems } from "./ownerNav";
import {
  PencilIcon,
  TrashIcon,
  CheckIcon,
  CloseIcon,
  ImagePlaceholderIcon,
  PlusIcon,
  CameraIcon,
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
          <MenusSection businessId={business.id} />
        </div>
      )}
    </DashboardShell>
  );
}

function MenusSection({ businessId }: { businessId: number }) {
  // Owner's own menus — works regardless of the business's approval status,
  // unlike the public listForBusiness endpoint used on the customer-facing pages.
  const { data: menus, isLoading } = useQuery({
    queryKey: ["menus", "mine", businessId],
    queryFn: () => menuApi.listMine(),
  });

  if (isLoading) return <PageSpinner />;

  const hasMenu = !!menus && menus.length > 0;

  return (
    <div style={{ display: "flex", flexDirection: "column", gap: 28 }}>
      {/* Each business can only ever have one menu; once it exists, owners just add
          products to it instead of creating another one. */}
      {!hasMenu && <NewMenuForm businessId={businessId} />}

      {!hasMenu ? (
        <EmptyState title="هنوز منویی نساختی" />
      ) : (
        menus!.map((menu) => <MenuBlock key={menu.id} menu={menu} businessId={businessId} />)
      )}
    </div>
  );
}

function NewMenuForm({ businessId }: { businessId: number }) {
  const queryClient = useQueryClient();
  const { notify } = useToast();
  const [name, setName] = useState("");

  const mutation = useMutation({
    mutationFn: () => menuApi.create({ name }),
    onSuccess: () => {
      queryClient.invalidateQueries({ queryKey: ["menus", "mine", businessId] });
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
      queryClient.invalidateQueries({ queryKey: ["menus", "mine", businessId] });
      setEditingTitle(false);
      notify("نام منو به‌روزرسانی شد", "ok");
    },
    onError: (err) => notify(errorMessage(err), "danger"),
  });

  const deleteMenu = useMutation({
    mutationFn: () => menuApi.remove(menu.id),
    onSuccess: () => {
      queryClient.invalidateQueries({ queryKey: ["menus", "mine", businessId] });
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
        <Button variant="ok" size="sm" onClick={() => setShowForm((s) => !s)}>
          {showForm ? (
            "بستن"
          ) : (
            <>
              <PlusIcon size={16} />
              افزودن محصول
            </>
          )}
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
                <img src={resolveMediaUrl(p.imageUrl) ?? undefined} alt={p.name} className="product-thumb" />
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

/** Lets the owner pick a photo from their device; uploads it right away and reports
 *  back the public URL to store as the product's imageUrl. Mirrors the profile-picture
 *  upload pattern (ProfilePage.tsx), just with a square/rounded thumbnail instead of
 *  a circular avatar. */
function ProductImagePicker({
  imageUrl,
  onChange,
}: {
  imageUrl: string;
  onChange: (url: string) => void;
}) {
  const { notify } = useToast();
  const fileInputRef = useRef<HTMLInputElement>(null);
  const [uploading, setUploading] = useState(false);
  const previewUrl = resolveMediaUrl(imageUrl);

  async function handleFileChange(e: ChangeEvent<HTMLInputElement>) {
    const file = e.target.files?.[0];
    e.target.value = ""; // allow re-selecting the same file later
    if (!file) return;

    setUploading(true);
    try {
      const { url } = await productApi.uploadImage(file);
      onChange(url);
    } catch (err) {
      notify(errorMessage(err), "danger");
    } finally {
      setUploading(false);
    }
  }

  return (
    <div className="image-picker-row">
      <button
        type="button"
        className="image-picker"
        onClick={() => fileInputRef.current?.click()}
        disabled={uploading}
        aria-label="آپلود عکس محصول"
      >
        {previewUrl ? (
          <img src={previewUrl} alt="" className="image-picker-img" />
        ) : (
          <span className="image-picker-fallback">
            <ImagePlaceholderIcon size={26} />
          </span>
        )}
        <span className="image-picker-edit">
          <CameraIcon size={13} />
        </span>
      </button>
      <input
        ref={fileInputRef}
        type="file"
        accept="image/jpeg,image/png,image/webp"
        hidden
        onChange={handleFileChange}
      />
      <span className={`image-picker-hint${uploading ? " uploading" : ""}`}>
        {uploading ? "در حال آپلود عکس..." : "برای آپلود عکس از گوشی یا کامپیوتر کلیک کن"}
      </span>
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
      <ProductImagePicker imageUrl={imageUrl} onChange={setImageUrl} />
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
        <ProductImagePicker imageUrl={imageUrl} onChange={setImageUrl} />
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
