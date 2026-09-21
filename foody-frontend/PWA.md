# Foody PWA

Foody uses `vite-plugin-pwa` to generate the production web app manifest and
Workbox service worker. The manifest uses `/` as its start URL and scope, runs in
standalone mode, and reuses the existing Foody mark for standard, maskable, and
Apple touch icons.

## Caching and offline behavior

The service worker precaches only the generated application shell and static
build assets. It defines no runtime caches, background sync, or request replay.
`/api` and `/uploads` are explicitly excluded from SPA navigation fallback, and
API/upload responses, authorization headers, tokens, wallet data, purchases,
reservations, admin operations, and user uploads are never added to a service
worker cache.

When connectivity is lost, the cached shell may remain available and shows an
offline notice. Live data and transactional actions still require the network;
the application does not claim that an offline action succeeded or queue it for
later.

## Updates

The browser checks the generated service worker normally. When a new release is
ready, Foody prompts the user before activating it and reloading the page, so an
in-progress form is not disrupted without warning. Old generated caches are
removed after the replacement service worker activates.

## Installation

When the browser exposes a real installation path, Foody shows a discreet
**Install Foody** action in its existing navigation. Chromium browsers receive
the native install prompt only after the user selects that action; a dismissed
or consumed prompt is not offered again in that page session. The action is
hidden while Foody is already running in standalone mode and in browsers with
no supported installation path.

On iPhone and iPad, where the native prompt is not generally available, the
same user-initiated action opens concise Persian instructions to use Safari's
Share menu and **Add to Home Screen**. It does not claim to install the app
automatically.

## Production validation

After deploying over HTTPS, use a fresh browser profile and verify:

1. DevTools **Application > Manifest** reports no installability errors and shows
   the regular and maskable icons.
2. Installing from `https://dayca.ir/` opens a standalone Foody window and SPA
   routes still refresh correctly.
3. DevTools **Application > Service Workers** shows `/sw.js` controlling `/`.
4. With the browser offline, the cached shell displays the offline notice and
   API-dependent screens/actions fail truthfully without queued requests.
5. Deploy a harmless frontend change, keep the old app open, and verify that the
   update prompt appears and reloads only after confirmation.
6. DevTools **Cache Storage** contains static build files only—no `/api` or
   `/uploads` responses.
7. In Chromium, use the navigation install action and verify the browser-native
   prompt appears only after clicking it; dismissing it removes the action for
   the current page session.
8. On iPhone/iPad Safari, use the navigation install action and verify it shows
   the Share / Add to Home Screen guidance, then confirm the action is absent
   after launching Foody from the home screen.
