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
