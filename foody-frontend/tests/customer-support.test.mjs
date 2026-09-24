import test from "node:test";
import assert from "node:assert/strict";
import { readFile } from "node:fs/promises";

const [api, customerPage, app, publicNav, profile, bell, adminPage, types] = await Promise.all([
  readFile(new URL("../src/features/communications/communicationApi.ts", import.meta.url), "utf8"),
  readFile(new URL("../src/features/communications/CustomerSupportPage.tsx", import.meta.url), "utf8"),
  readFile(new URL("../src/App.tsx", import.meta.url), "utf8"),
  readFile(new URL("../src/components/PublicNav.tsx", import.meta.url), "utf8"),
  readFile(new URL("../src/features/profile/ProfilePage.tsx", import.meta.url), "utf8"),
  readFile(new URL("../src/features/notifications/NotificationBell.tsx", import.meta.url), "utf8"),
  readFile(new URL("../src/features/communications/AdminCommunicationsPage.tsx", import.meta.url), "utf8"),
  readFile(new URL("../src/types/api.ts", import.meta.url), "utf8"),
]);

test("customer support client reuses the ticket endpoints under /api/customer/communications", () => {
  assert.match(api, /customerCommunicationApi[\s\S]*\/api\/customer\/communications\/tickets/);
  assert.match(api, /customerCommunicationApi[\s\S]*createTicket:[\s\S]*method: "POST"/);
  assert.match(api, /customerCommunicationApi[\s\S]*reply:[\s\S]*\/replies/);
  assert.match(api, /customerCommunicationApi[\s\S]*unreadCount:[\s\S]*unread-count/);
});

test("customer support page creates tickets, lists only the customer's own, and threads replies", () => {
  assert.match(customerPage, /customerCommunicationApi\.createTicket/);
  assert.match(customerPage, /customerCommunicationApi\.tickets/);
  assert.match(customerPage, /customerCommunicationApi\.reply/);
  assert.match(customerPage, /TicketThread/);
  assert.match(customerPage, /detail\.ticket\.status !== "CLOSED"/);
});

test("the support destination is CUSTOMER-only and reachable from nav and profile", () => {
  assert.match(app, /roles=\{\["CUSTOMER"\]\}[\s\S]*\/support/);
  assert.match(publicNav, /user\?\.role === "CUSTOMER" && <NavLink to="\/support"/);
  assert.match(profile, /to="\/support"/);
});

test("customer unread state feeds the existing notification bell without a duplicate counter", () => {
  assert.match(bell, /customerCommunicationApi\.unreadCount/);
  assert.match(bell, /customerCommunicationUnread\?\.unreadCount/);
  assert.match(bell, /navigate\("\/support"\)/);
  assert.match(bell, /SUPPORT_TICKET.*\/support/);
});

test("admin ticket list distinguishes customer tickets from business tickets", () => {
  assert.match(adminPage, /requesterType/);
  assert.match(adminPage, /مشتری/);
  assert.match(adminPage, /sourceLabel/);
});

test("shared TicketSummary type models a nullable business context and a generic requester", () => {
  assert.match(types, /requesterType: TicketRequesterType/);
  assert.match(types, /businessId: number \| null/);
  assert.match(types, /requesterDisplayName: string/);
});
