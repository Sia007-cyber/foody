import { useState, type FormEvent } from "react";
import { useMutation, useQuery, useQueryClient } from "@tanstack/react-query";
import { businessApi } from "../businesses/businessApi";
import { menuApi, productApi } from "../catalog/catalogApi";
import { DashboardShell } from "../../components/DashboardShell";
import { Input, Textarea } from "../../components/Field";
import { Button } from "../../components/Button";
import { PageSpinner, EmptyState } from "../../components/Controls";
import { useToast, errorMessage } from "../../components/Feedback";
import { formatToman } from "../../lib/format";
import { ownerNavItems } from "./ownerNav";
import type { Menu } from "../../types/api";

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
        <MenuBlock key={menu.id} menu={menu} />
      ))}
    </div>
  );
}

function MenuBlock({ menu }: { menu: Menu }) {
  const [showForm, setShowForm] = useState(false);
  const { data: products, isLoading } = useQuery({
    queryKey: ["products", "menu", menu.id],
    queryFn: () => productApi.listForMenu(menu.id),
  });
  const queryClient = useQueryClient();
  const { notify } = useToast();

  const toggleAvailability = useMutation({
    mutationFn: ({ id, isAvailable }: { id: number; isAvailable: boolean }) =>
      productApi.update(id, { isAvailable }),
    onSuccess: () => queryClient.invalidateQueries({ queryKey: ["products", "menu", menu.id] }),
    onError: (err) => notify(errorMessage(err), "danger"),
  });

  return (
    <div>
      <div style={{ display: "flex", alignItems: "center", justifyContent: "space-between", marginBottom: 12 }}>
        <h3 style={{ fontSize: 17, fontWeight: 700 }}>{menu.name}</h3>
        <Button variant="secondary" size="sm" onClick={() => setShowForm((s) => !s)}>
          {showForm ? "بستن" : "افزودن محصول"}
        </Button>
      </div>

      {showForm && (
        <NewProductForm menuId={menu.id} onDone={() => setShowForm(false)} />
      )}

      {isLoading ? (
        <PageSpinner />
      ) : !products || products.length === 0 ? (
        <EmptyState title="محصولی ثبت نشده" />
      ) : (
        <div className="list-group">
          {products.map((p) => (
            <div key={p.id} className="list-row">
              <div className="list-row-main">
                <span className="list-row-title">{p.name}</span>
                <span className="list-row-sub">{formatToman(p.price)}</span>
              </div>
              <Button
                variant={p.isAvailable ? "secondary" : "primary"}
                size="sm"
                loading={toggleAvailability.isPending}
                onClick={() => toggleAvailability.mutate({ id: p.id, isAvailable: !p.isAvailable })}
              >
                {p.isAvailable ? "موجود" : "ناموجود"}
              </Button>
            </div>
          ))}
        </div>
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

  const mutation = useMutation({
    mutationFn: () => productApi.create({ menuId, name, description: description || undefined, price: Number(price) }),
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
