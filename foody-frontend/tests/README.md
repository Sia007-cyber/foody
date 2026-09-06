# Session regression tests

Run from `foody-frontend`:

```bash
npm test
npm run build
npm run lint
```

From the repository root, run `git diff --check`.

The tests use Node's built-in test runner and native TypeScript loading (verified
with Node 26.8.1). There are no added dependencies, browser binaries, or running
backend requirements. `--test-isolation=none` runs the tests in one process;
they are sequential because they exercise the application singleton session and
QueryClient. Query/mutation GC delays are disabled only in the test fixture.

The fixture replaces fetch and localStorage and dispatches storage events through
an EventTarget. It imports the actual API client, session lifecycle, auth API and
production QueryClient. It does not mount React components or launch a browser.

Coverage includes:

- JSON, null, empty, plain text, malformed JSON, invalid envelope fields and
  unreadable error streams; 401 status preservation and usable non-JSON 500 errors.
- Refresh success, concurrent and delayed 401s, missing/malformed refresh tokens,
  network/HTTP refresh failures, one retry maximum and second-401 invalidation.
- Multipart upload success, refresh, normalized errors and session invalidation.
- Login/register account switching, all private cache families, a transient `/me`
  retry, persistent-failure rollback, overlapping login attempts and logout races.
- Immediate logout with the captured token sent to the server; a slow logout
  response cannot delay navigation or affect the next session.
- Invalid startup, refresh-only startup recovery and stale startup responses.
- Request abort signals, late reads/mutations/refreshes, stale profile updates,
  query cancellation and prevention of old public catalog cache repopulation.
- Storage token removal, storage clear, replacement with owner/admin identities,
  unchanged/unrelated events, and replacement before the storage event is delivered.
- Public requests do not refresh/invalidate a session; JSON success and 204 remain
  supported.

The route wiring was reviewed: `RequireAuth` uses the same user/loading snapshot,
role checks are unchanged, and `RequireOwnerBusiness` remains behind the owner
role guard. The provider remounts session-dependent UI on generation changes to
remove old observers and local form state. CartProvider remains outside this
boundary to preserve the existing guest-cart-to-login flow.

No browser/manual interaction or live backend end-to-end run is claimed by these
tests. Server logout revocation, refresh-token rotation/reuse detection, and
password-change session invalidation are intentionally outside this task.
