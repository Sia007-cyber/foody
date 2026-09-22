import { useMutation, useQuery, useQueryClient } from "@tanstack/react-query";
import { useNavigate } from "react-router-dom";
import type { MouseEvent } from "react";
import { HeartIcon } from "../../components/icons";
import { useAuth } from "../auth/AuthContext";
import { favoritesApi } from "./favoritesApi";

export function FavoriteButton({ businessId, className = "" }: { businessId: number; className?: string }) {
  const { user } = useAuth();
  const navigate = useNavigate();
  const queryClient = useQueryClient();
  const isCustomer = user?.role === "CUSTOMER";
  const idsQuery = useQuery({ queryKey: ["favorites", "ids"], queryFn: favoritesApi.businessIds, enabled: isCustomer });
  const isFavorite = (idsQuery.data ?? []).includes(businessId);
  const mutation = useMutation({
    mutationFn: () => isFavorite ? favoritesApi.remove(businessId) : favoritesApi.add(businessId),
    onMutate: async () => {
      await queryClient.cancelQueries({ queryKey: ["favorites", "ids"] });
      const previous = queryClient.getQueryData<number[]>(["favorites", "ids"]);
      queryClient.setQueryData<number[]>(["favorites", "ids"], (ids = []) => isFavorite ? ids.filter((id) => id !== businessId) : [...ids, businessId]);
      return { previous };
    },
    onError: (_error, _variables, context) => queryClient.setQueryData(["favorites", "ids"], context?.previous),
    onSettled: () => {
      queryClient.invalidateQueries({ queryKey: ["favorites", "ids"] });
      queryClient.invalidateQueries({ queryKey: ["favorites", "list"] });
    },
  });
  function onClick(event: MouseEvent<HTMLButtonElement>) {
    event.preventDefault(); event.stopPropagation();
    if (!user) { navigate("/login", { state: { from: `/businesses/${businessId}` } }); return; }
    if (!isCustomer) return;
    mutation.mutate();
  }
  if (user && !isCustomer) return null;
  return <button type="button" className={`favorite-button ${isFavorite ? "is-favorite" : ""} ${className}`} onClick={onClick} disabled={mutation.isPending} aria-label={isFavorite ? "حذف از علاقه‌مندی‌ها" : "افزودن به علاقه‌مندی‌ها"} aria-pressed={isFavorite}>
    <HeartIcon size={20} />
  </button>;
}
