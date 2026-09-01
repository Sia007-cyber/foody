import { StrictMode } from "react";
import { createRoot } from "react-dom/client";
import "./styles/global.css";
import "./components/ui.css";
import "./components/layout.css";
import App from "./App.tsx";

document.documentElement.dir = "rtl";
document.documentElement.lang = "fa";

createRoot(document.getElementById("root")!).render(
  <StrictMode>
    <App />
  </StrictMode>
);
