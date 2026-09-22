import test from "node:test";
import assert from "node:assert/strict";
import { readFile } from "node:fs/promises";

const [api, owner, admin, app, ownerNav, adminNav, bell] = await Promise.all([
  readFile(new URL("../src/features/communications/communicationApi.ts", import.meta.url), "utf8"),
  readFile(new URL("../src/features/communications/OwnerCommunicationsPage.tsx", import.meta.url), "utf8"),
  readFile(new URL("../src/features/communications/AdminCommunicationsPage.tsx", import.meta.url), "utf8"),
  readFile(new URL("../src/App.tsx", import.meta.url), "utf8"),
  readFile(new URL("../src/features/owner/ownerNav.tsx", import.meta.url), "utf8"),
  readFile(new URL("../src/features/admin/adminNav.tsx", import.meta.url), "utf8"),
  readFile(new URL("../src/features/notifications/NotificationBell.tsx", import.meta.url), "utf8"),
]);

test("owner support flow creates, lists, opens, and replies to authenticated-business tickets", () => {
  assert.match(api, /\/api\/business\/communications\/tickets/);
  assert.match(api, /createTicket:[\s\S]*method: "POST"/);
  assert.match(api, /reply:[\s\S]*\/replies/);
  assert.match(owner, /درخواست جدید/);
  assert.match(owner, /TicketThread/);
  assert.match(owner, /detail\.ticket\.status!=="CLOSED"/);
});

test("admin manages tickets and sends direct or broadcast messages", () => {
  assert.match(api, /\/api\/admin\/communications\/tickets/);
  assert.match(api, /sendDirect:[\s\S]*messages\/direct/);
  assert.match(api, /sendBroadcast:[\s\S]*messages\/broadcast/);
  assert.match(admin, /adminApi\.businesses/);
  assert.match(admin, /بستن درخواست/);
  assert.match(admin, /همه کسب‌وکارها/);
});

test("communication destinations are role-protected and present in both dashboards", () => {
  assert.match(app, /roles=\{\["BUSINESS_OWNER"\]\}[\s\S]*\/business\/communications/);
  assert.match(app, /roles=\{\["ADMIN"\]\}[\s\S]*\/admin\/communications/);
  assert.match(ownerNav, /\/business\/communications/);
  assert.match(adminNav, /\/admin\/communications/);
});

test("owner communication unread state feeds the existing notification bell", () => {
  assert.match(bell, /ownerCommunicationApi\.unreadCount/);
  assert.match(bell, /communicationUnread\?\.unreadCount/);
  assert.match(bell, /navigate\("\/business\/communications"\)/);
  assert.match(owner, /ownerCommunicationApi\.message\(id\)/);
});
