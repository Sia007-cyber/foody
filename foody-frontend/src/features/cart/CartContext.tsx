import { createContext, useContext, useMemo, useState, type ReactNode } from "react";
import type { Product } from "../../types/api";

export interface CartLine {
  product: Product;
  quantity: number;
}

interface CartContextValue {
  businessId: number | null;
  lines: CartLine[];
  totalItems: number;
  totalAmount: number;
  addItem: (businessId: number, product: Product) => void;
  removeItem: (productId: number) => void;
  setQuantity: (productId: number, quantity: number) => void;
  clear: () => void;
}

const CartContext = createContext<CartContextValue | null>(null);

export function CartProvider({ children }: { children: ReactNode }) {
  const [businessId, setBusinessId] = useState<number | null>(null);
  const [lines, setLines] = useState<CartLine[]>([]);

  function addItem(newBusinessId: number, product: Product) {
    // A cart can only hold items from one business at a time (orders are per-business).
    if (businessId !== null && businessId !== newBusinessId) {
      setLines([{ product, quantity: 1 }]);
      setBusinessId(newBusinessId);
      return;
    }
    setBusinessId(newBusinessId);
    setLines((prev) => {
      const existing = prev.find((l) => l.product.id === product.id);
      if (existing) {
        return prev.map((l) => (l.product.id === product.id ? { ...l, quantity: l.quantity + 1 } : l));
      }
      return [...prev, { product, quantity: 1 }];
    });
  }

  function removeItem(productId: number) {
    setLines((prev) => {
      const next = prev.filter((l) => l.product.id !== productId);
      if (next.length === 0) setBusinessId(null);
      return next;
    });
  }

  function setQuantity(productId: number, quantity: number) {
    if (quantity <= 0) {
      removeItem(productId);
      return;
    }
    setLines((prev) => prev.map((l) => (l.product.id === productId ? { ...l, quantity } : l)));
  }

  function clear() {
    setLines([]);
    setBusinessId(null);
  }

  const totalItems = useMemo(() => lines.reduce((sum, l) => sum + l.quantity, 0), [lines]);
  const totalAmount = useMemo(
    () => lines.reduce((sum, l) => sum + Number(l.product.price) * l.quantity, 0),
    [lines]
  );

  return (
    <CartContext.Provider
      value={{ businessId, lines, totalItems, totalAmount, addItem, removeItem, setQuantity, clear }}
    >
      {children}
    </CartContext.Provider>
  );
}

export function useCart(): CartContextValue {
  const ctx = useContext(CartContext);
  if (!ctx) throw new Error("useCart must be used within CartProvider");
  return ctx;
}
