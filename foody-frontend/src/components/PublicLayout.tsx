import { Outlet } from "react-router-dom";
import { PublicNav } from "./PublicNav";

export function PublicLayout() {
  return (
    <>
      <PublicNav />
      <Outlet />
    </>
  );
}
