import { beforeEach, after, test } from 'node:test';
import assert from 'node:assert/strict';

const storage = new Map();
Object.defineProperty(globalThis, 'localStorage', { value: {
  getItem: (key) => storage.get(key) ?? null,
  setItem: (key, value) => storage.set(key, String(value)),
  removeItem: (key) => storage.delete(key),
  clear: () => storage.clear(),
}, configurable: true });
globalThis.window = new EventTarget();
const originalFetch = globalThis.fetch;
const session = await import('../src/lib/session.ts');
const { apiRequest, apiUpload, ApiError } = await import('../src/lib/api.ts');
const { queryClient } = await import('../src/lib/queryClient.ts');
const auth = await import('../src/features/auth/authSession.ts');
// Removed/cancelled queries may schedule their normal five-minute GC after settling.
queryClient.setDefaultOptions({ ...queryClient.getDefaultOptions(), queries: { ...queryClient.getDefaultOptions().queries, gcTime: 0 }, mutations: { gcTime: 0 } });
const json = (body, status = 200) => new Response(JSON.stringify(body), { status, headers: { 'Content-Type': 'application/json' } });
const tokens = (name) => ({ accessToken: `${name}-access`, refreshToken: `${name}-refresh` });
const user = (id, role = 'CUSTOMER') => ({ id, email: `${id}@foody.test`, fullName: `User ${id}`, role, status: 'ACTIVE' });
const establish = (id = 1, role) => session.replaceSession(tokens(id), user(id, role), session.captureSession());
const deferred = () => { let resolve; const promise = new Promise((r) => { resolve = r; }); return { promise, resolve }; };
const tick = () => new Promise((r) => setImmediate(r));
const loggedOut = () => {
  assert.equal(session.getAccessToken(), null);
  assert.equal(session.getRefreshToken(), null);
  assert.equal(session.getSessionSnapshot().user, null);
  assert.equal(session.getSessionSnapshot().isLoading, false);
  assert.equal(queryClient.getQueryCache().getAll().length, 0);
};
function storageEvent(key) {
  const event = new Event('storage');
  Object.assign(event, { key, storageArea: localStorage });
  window.dispatchEvent(event);
}
beforeEach(() => {
  session.invalidateSession();
  globalThis.fetch = async () => { throw new Error('Unexpected fetch'); };
});
after(() => { queryClient.clear(); globalThis.fetch = originalFetch; });

for (const [name, body, status, contentType] of [
  ['JSON 401', JSON.stringify({ code: 'EXPIRED', message: 'expired', details: ['detail'] }), 401, 'application/json'],
  ['empty JSON 401', '', 401, 'application/json'],
  ['empty proxy 401', '', 401, 'text/plain'],
  ['plain 401', 'Unauthorized', 401, 'text/plain'],
  ['malformed JSON 401', '{bad', 401, 'application/json'],
  ['JSON null 401', 'null', 401, 'application/json'],
  ['non-JSON 500', 'upstream unavailable', 500, 'text/plain'],
]) {
  test(name + ' remains a usable ApiError', async () => {
    globalThis.fetch = async () => new Response(body, { status, headers: { 'Content-Type': contentType } });
    await assert.rejects(apiRequest('/public', { auth: false }), (error) => {
      assert.ok(error instanceof ApiError);
      assert.equal(error.status, status);
      assert.ok(error.message.length > 0);
      if (name === 'JSON 401') {
        assert.equal(error.code, 'EXPIRED');
        assert.equal(error.message, 'expired');
        assert.deepEqual(error.details, ['detail']);
      }
      return true;
    });
  });
}
test('malformed envelope fields never become invalid Error fields', () => {
  for (const value of [null, undefined, [], 123, { message: {}, code: [], details: {} }]) {
    const error = new ApiError(value, 401);
    assert.equal(error.status, 401);
    assert.equal(typeof error.message, 'string');
    assert.equal(typeof error.code, 'string');
    assert.equal(error.details, null);
  }
});

test('expired access refreshes once and retries with new credentials', async () => {
  establish();
  const requests = [];
  globalThis.fetch = async (url, options) => {
    requests.push([url, options.headers.Authorization]);
    if (url.endsWith('/refresh')) return json(tokens('new'));
    if (options.headers.Authorization === 'Bearer 1-access') return new Response('', { status: 401 });
    return json({ id: 7 });
  };
  assert.deepEqual(await apiRequest('/api/orders/my'), { id: 7 });
  assert.equal(requests.length, 3);
  assert.equal(requests[2][1], 'Bearer new-access');
  assert.equal(session.getRefreshToken(), 'new-refresh');
  assert.equal(session.getSessionSnapshot().user.id, 1);
});

test('concurrent JSON/empty/text 401s share one refresh, including a late old-token 401', async () => {
  establish();
  const refresh = deferred();
  const late = deferred();
  let refreshCount = 0;
  let initialCount = 0;
  globalThis.fetch = async (url, options) => {
    if (url.endsWith('/refresh')) { refreshCount++; return refresh.promise; }
    if (options.headers.Authorization === 'Bearer 1-access') {
      initialCount++;
      if (url.endsWith('/late')) return late.promise;
      return new Response(initialCount === 1 ? '' : 'Unauthorized', { status: 401 });
    }
    return json({ ok: true });
  };
  const requests = ['/one', '/two', '/late'].map((path) => apiRequest(path));
  await tick();
  assert.equal(refreshCount, 1);
  refresh.resolve(json(tokens('new')));
  await Promise.all(requests.slice(0, 2));
  late.resolve(new Response('{broken', { status: 401, headers: { 'Content-Type': 'application/json' } }));
  await requests[2];
  assert.equal(refreshCount, 1);
});

test('failed shared refresh invalidates user, tokens, cache exactly once without retry loop', async () => {
  establish();
  queryClient.setQueryData(['wallet', 'balance'], { owner: 1 });
  let boundaries = 0;
  const off = session.onSessionBoundary(() => boundaries++);
  let refreshes = 0;
  let calls = 0;
  const refresh = deferred();
  globalThis.fetch = async (url) => {
    calls++;
    if (url.endsWith('/refresh')) { refreshes++; return refresh.promise; }
    return new Response('', { status: 401 });
  };
  const results = Promise.allSettled([apiRequest('/one'), apiRequest('/two')]);
  await tick();
  refresh.resolve(new Response('Unauthorized', { status: 401 }));
  assert.ok((await results).every((result) => result.status === 'rejected'));
  assert.equal(refreshes, 1);
  assert.equal(calls, 3);
  assert.equal(boundaries, 1);
  off();
  loggedOut();
});

test('401 on retry invalidates session, without a second refresh', async () => {
  establish();
  let refreshes = 0;
  let requests = 0;
  globalThis.fetch = async (url) => {
    if (url.endsWith('/refresh')) { refreshes++; return json(tokens('new')); }
    requests++;
    return json(null, 401);
  };
  await assert.rejects(apiRequest('/private'), { status: 401 });
  assert.equal(requests, 2);
  assert.equal(refreshes, 1);
  loggedOut();
});

test('missing or malformed refresh credentials invalidate instead of storing undefined', async () => {
  establish();
  globalThis.fetch = async (url) => url.endsWith('/refresh') ? json({ accessToken: null }) : json(null, 401);
  await assert.rejects(apiRequest('/private'), { status: 401 });
  loggedOut();
});

test('uploads share refresh and robust parsing and do not set a multipart Content-Type', async () => {
  establish();
  let refreshes = 0;
  let uploads = 0;
  globalThis.fetch = async (url, options) => {
    if (url.endsWith('/refresh')) { refreshes++; return json(tokens('new')); }
    uploads++;
    assert.ok(options.body instanceof FormData);
    assert.equal(options.headers['Content-Type'], undefined);
    return uploads === 1 ? new Response('', { status: 401 }) : json({ url: '/uploads/photo.png' });
  };
  assert.deepEqual(await apiUpload('/api/uploads/image', new File(['x'], 'photo.png')), { url: '/uploads/photo.png' });
  assert.equal(refreshes, 1);
  assert.equal(uploads, 2);
  globalThis.fetch = async () => new Response('proxy down', { status: 500 });
  await assert.rejects(apiUpload('/api/uploads/image', new File(['x'], 'x')), { status: 500, message: 'proxy down' });
});

test('upload refresh failure also clears the complete session', async () => {
  establish();
  queryClient.setQueryData(['business', 'profile'], user(1));
  globalThis.fetch = async () => new Response('', { status: 401 });
  await assert.rejects(apiUpload('/api/uploads/image', new File(['x'], 'x')), { status: 401 });
  loggedOut();
});

for (const kind of ['login', 'register']) {
  test(`${kind} commits B only after me succeeds and clears every old cache family`, async () => {
    establish();
    for (const key of [['orders','my'], ['reservations','my'], ['wallet','balance'], ['notifications','my'],
      ['business','profile'], ['business','orders'], ['menus','mine',1], ['products','menu',1], ['admin','dashboard'], ['users','me']]) {
      queryClient.setQueryData(key, { owner: 1 });
    }
    const me = deferred();
    globalThis.fetch = async (url, options) => {
      if (url.endsWith(`/${kind}`)) return json(tokens(2));
      assert.equal(options.headers.Authorization, 'Bearer 2-access');
      return me.promise;
    };
    const result = auth[kind]({ email: '2@foody.test', password: 'password', role: 'CUSTOMER', fullName: 'Two' });
    await tick();
    assert.equal(session.getSessionSnapshot().user.id, 1);
    assert.equal(session.getAccessToken(), '1-access');
    me.resolve(json(user(2)));
    assert.equal((await result).id, 2);
    assert.equal(session.getSessionSnapshot().user.id, 2);
    assert.equal(session.getAccessToken(), '2-access');
    assert.equal(queryClient.getQueryCache().getAll().length, 0);
  });
}

test('transient candidate me failure retries once without repeating registration', async () => {
  establish();
  let registrations = 0;
  let me = 0;
  globalThis.fetch = async (url) => {
    if (url.endsWith('/register')) { registrations++; return json(tokens(2)); }
    return ++me === 1 ? new Response('unavailable', { status: 503 }) : json(user(2));
  };
  await auth.register({ email: '2@foody.test', password: 'password', fullName: 'Two', role: 'CUSTOMER' });
  assert.equal(registrations, 1);
  assert.equal(me, 2);
  assert.equal(session.getSessionSnapshot().user.id, 2);
});

for (const kind of ['login', 'register']) {
  test(`${kind} persistent me failure rolls back without losing A or installing B tokens`, async () => {
    establish();
    queryClient.setQueryData(['wallet'], { owner: 1 });
    globalThis.fetch = async (url) => url.endsWith(`/${kind}`) ? json(tokens(2)) : new Response('unavailable', { status: 503 });
    await assert.rejects(auth[kind]({ email: '2@foody.test', password: 'password', fullName: 'Two', role: 'CUSTOMER' }));
    assert.equal(session.getAccessToken(), '1-access');
    assert.equal(session.getSessionSnapshot().user.id, 1);
    assert.deepEqual(queryClient.getQueryData(['wallet']), { owner: 1 });
  });
}

test('logout clears immediately, calls server with A token, and late completion cannot log out B', async () => {
  establish();
  queryClient.setQueryData(['orders'], { owner: 1 });
  const server = deferred();
  globalThis.fetch = async (url, options) => {
    assert.ok(url.endsWith('/logout'));
    assert.equal(options.headers.Authorization, 'Bearer 1-access');
    assert.equal(options.signal, undefined);
    return server.promise;
  };
  const result = auth.logout();
  loggedOut();
  establish(2);
  server.resolve(new Response('', { status: 500 }));
  await result;
  assert.equal(session.getSessionSnapshot().user.id, 2);
});

test('A logout then B login cannot reuse A cache', async () => {
  establish();
  queryClient.setQueryData(['wallet'], { owner: 1 });
  globalThis.fetch = async (url) => url.endsWith('/logout') ? new Response(null, { status: 204 }) :
    url.endsWith('/login') ? json(tokens(2)) : json(user(2));
  await auth.logout();
  await auth.login({ email: '2@foody.test', password: 'password' });
  assert.equal(queryClient.getQueryData(['wallet']), undefined);
  assert.equal(session.getSessionSnapshot().user.id, 2);
});

test('startup with invalid stored credentials becomes cleanly unauthenticated', async () => {
  localStorage.setItem('foody.accessToken', 'invalid');
  localStorage.setItem('foody.refreshToken', 'invalid');
  queryClient.setQueryData(['orders'], { owner: 1 });
  globalThis.fetch = async () => new Response('', { status: 401 });
  await auth.restoreSession();
  loggedOut();
});

test('startup can recover from a missing access token with a valid refresh token', async () => {
  localStorage.setItem('foody.refreshToken', 'valid');
  globalThis.fetch = async (url, options) => url.endsWith('/refresh') ? json(tokens(2)) :
    options.headers.Authorization ? json(user(2)) : new Response('', { status: 401 });
  await auth.restoreSession();
  assert.equal(session.getSessionSnapshot().user.id, 2);
});

test('logout aborts private requests and late responses cannot repopulate query cache', async () => {
  establish();
  const response = deferred();
  let signal;
  globalThis.fetch = async (_url, options) => { signal = options.signal; return response.promise; };
  const pending = queryClient.fetchQuery({ queryKey: ['orders'], queryFn: () => apiRequest('/orders') });
  const settled = Promise.allSettled([pending]);
  session.invalidateSession();
  assert.ok(signal.aborted);
  response.resolve(json({ owner: 1 }));
  assert.equal((await settled)[0].status, 'rejected');
  await tick();
  loggedOut();
});

test('late private response and stale profile update cannot restore A after replacement', async () => {
  establish();
  const ticket = session.captureSession();
  const response = deferred();
  globalThis.fetch = async () => response.promise;
  const pending = apiRequest('/api/users/me');
  establish(2);
  response.resolve(json(user(1)));
  await assert.rejects(pending, session.SessionChangedError);
  assert.throws(() => session.updateSessionUser(user(1), ticket), session.SessionChangedError);
  assert.equal(session.getSessionSnapshot().user.id, 2);
});

for (const success of [true, false]) {
  test(`late refresh ${success ? 'success' : 'failure'} cannot overwrite or clear B`, async () => {
    establish();
    const response = deferred();
    let signal;
    globalThis.fetch = async (url, options) => {
      if (url.endsWith('/refresh')) { signal = options.signal; return response.promise; }
      return new Response('', { status: 401 });
    };
    const result = Promise.allSettled([apiRequest('/private')]);
    await tick();
    establish(2);
    assert.ok(signal.aborted);
    response.resolve(success ? json(tokens('stale')) : new Response('', { status: 401 }));
    await result;
    assert.equal(session.getAccessToken(), '2-access');
    assert.equal(session.getSessionSnapshot().user.id, 2);
  });
}

test('login completing after logout cannot restore authentication', async () => {
  establish();
  const response = deferred();
  globalThis.fetch = async () => response.promise;
  const result = auth.login({ email: '2@foody.test', password: 'password' });
  session.invalidateSession();
  response.resolve(json(tokens(2)));
  await assert.rejects(result, session.SessionChangedError);
  loggedOut();
});

test('older concurrent login cannot win over a newer attempt', async () => {
  const old = deferred();
  globalThis.fetch = async (url, options) => {
    if (url.endsWith('/login')) return JSON.parse(options.body).email === 'old' ? old.promise : json(tokens(2));
    return json(user(2));
  };
  const first = auth.login({ email: 'old', password: 'password' });
  await auth.login({ email: 'new', password: 'password' });
  old.resolve(json(tokens(1)));
  await assert.rejects(first, session.SessionChangedError);
  assert.equal(session.getSessionSnapshot().user.id, 2);
});

test('cross-tab token removal and localStorage.clear remove authenticated state via storage listener', async () => {
  const stop = auth.startAuthSession();
  try {
    for (const key of ['foody.accessToken', null]) {
      establish();
      queryClient.setQueryData(['notifications'], { owner: 1 });
      globalThis.fetch = async () => new Response('', { status: 401 });
      if (key) localStorage.removeItem(key); else localStorage.clear();
      storageEvent(key);
      assert.equal(session.getSessionSnapshot().user, null);
      assert.equal(queryClient.getQueryCache().getAll().length, 0);
      await auth.restoreSession();
      loggedOut();
    }
  } finally { stop(); }
});

test('cross-tab replacement hides A immediately and resolves the new identity (including owner/admin)', async () => {
  const stop = auth.startAuthSession();
  try {
    for (const role of ['BUSINESS_OWNER', 'ADMIN']) {
      establish();
      queryClient.setQueryData(['business', 'profile'], { owner: 1 });
      const me = deferred();
      globalThis.fetch = async () => me.promise;
      localStorage.setItem('foody.accessToken', `${role}-access`);
      localStorage.setItem('foody.refreshToken', `${role}-refresh`);
      storageEvent('foody.accessToken');
      storageEvent('foody.refreshToken');
      assert.equal(session.getSessionSnapshot().user, null);
      assert.equal(session.getSessionSnapshot().isLoading, true);
      assert.equal(queryClient.getQueryCache().getAll().length, 0);
      me.resolve(json(user(2, role)));
      await auth.restoreSession();
      assert.equal(session.getSessionSnapshot().user.role, role);
    }
  } finally { stop(); }
});

test('storage replacement detected before its event prevents an old refresh overwriting it', async () => {
  establish();
  const response = deferred();
  globalThis.fetch = async (url) => url.endsWith('/refresh') ? response.promise : json(null, 401);
  const result = Promise.allSettled([apiRequest('/private')]);
  await tick();
  localStorage.setItem('foody.accessToken', '2-access');
  localStorage.setItem('foody.refreshToken', '2-refresh');
  response.resolve(json(tokens('stale')));
  await result;
  assert.equal(session.getAccessToken(), '2-access');
  assert.equal(session.getSessionSnapshot().user, null);
});

test('public 401 never refreshes or invalidates the current session; public success and 204 still work', async () => {
  establish();
  let calls = 0;
  globalThis.fetch = async (_url, options) => {
    calls++;
    assert.equal(options.headers.Authorization, undefined);
    return new Response('', { status: 401 });
  };
  await assert.rejects(apiRequest('/api/businesses', { auth: false }), { status: 401 });
  assert.equal(calls, 1);
  assert.equal(session.getSessionSnapshot().user.id, 1);
  globalThis.fetch = async () => json([{ id: 1 }]);
  assert.deepEqual(await apiRequest('/api/businesses', { auth: false }), [{ id: 1 }]);
  globalThis.fetch = async () => new Response(null, { status: 204 });
  assert.equal(await apiRequest('/private', { method: 'DELETE' }), undefined);
});

test('unreadable error body preserves HTTP 401 for refresh', async () => {
  establish();
  let refreshes = 0;
  globalThis.fetch = async (url, options) => {
    if (url.endsWith('/refresh')) { refreshes++; return json(tokens('new')); }
    if (options.headers.Authorization === 'Bearer new-access') return json({ ok: true });
    return { status: 401, ok: false, text: async () => { throw new TypeError('stream failed'); } };
  };
  assert.deepEqual(await apiRequest('/private'), { ok: true });
  assert.equal(refreshes, 1);
});

test('missing refresh token and network refresh failure both clear the session', async () => {
  for (const missing of [true, false]) {
    establish();
    if (missing) {
      localStorage.removeItem('foody.refreshToken');
      session.syncStoredSession();
    }
    globalThis.fetch = async (url) => {
      if (url.endsWith('/refresh')) throw new TypeError('offline');
      return new Response('', { status: 401 });
    };
    await assert.rejects(apiRequest('/private'), { status: 401 });
    loggedOut();
  }
});

test('subscriber sees user and cache cleared together at invalidation', () => {
  establish();
  queryClient.setQueryData(['wallet'], { owner: 1 });
  let notified = false;
  const off = session.subscribeSession(() => { notified = true; loggedOut(); });
  session.invalidateSession();
  off();
  assert.ok(notified);
});

test('a late startup me response cannot restore the user after logout', async () => {
  localStorage.setItem('foody.accessToken', '1-access');
  localStorage.setItem('foody.refreshToken', '1-refresh');
  const response = deferred();
  globalThis.fetch = async () => response.promise;
  const result = auth.restoreSession();
  session.invalidateSession();
  response.resolve(json(user(1)));
  await result;
  loggedOut();
});

test('candidate me 401 does not refresh or invalidate the previous account', async () => {
  establish();
  let calls = 0;
  globalThis.fetch = async (url) => {
    calls++;
    return url.endsWith('/login') ? json(tokens(2)) : new Response('', { status: 401 });
  };
  await assert.rejects(auth.login({ email: '2@foody.test', password: 'password' }), { status: 401 });
  assert.equal(calls, 2);
  assert.equal(session.getSessionSnapshot().user.id, 1);
  assert.equal(session.getAccessToken(), '1-access');
});

test('late mutation response cannot run success callbacks or refill cache for B', async () => {
  establish();
  const response = deferred();
  globalThis.fetch = async () => response.promise;
  let success = false;
  const mutation = queryClient.getMutationCache().build(queryClient, {
    mutationFn: () => apiRequest('/private', { method: 'PATCH', body: {} }),
    onSuccess: (data) => { success = true; queryClient.setQueryData(['profile'], data); },
  });
  const result = Promise.allSettled([mutation.execute()]);
  await tick();
  establish(2);
  response.resolve(json(user(1)));
  await result;
  assert.equal(success, false);
  assert.equal(queryClient.getQueryData(['profile']), undefined);
  assert.equal(queryClient.getMutationCache().getAll().length, 0);
});

test('old public catalog request also cannot refill the cleared QueryClient', async () => {
  establish();
  const response = deferred();
  globalThis.fetch = async () => response.promise;
  const result = Promise.allSettled([queryClient.fetchQuery({
    queryKey: ['products', 'menu', 1], queryFn: () => apiRequest('/public', { auth: false }),
  })]);
  establish(2);
  response.resolve(json([{ id: 1 }]));
  await result;
  await tick();
  assert.equal(queryClient.getQueryData(['products', 'menu', 1]), undefined);
});

test('unrelated storage events do not clear the active user', async () => {
  const stop = auth.startAuthSession();
  try {
    establish();
    const generation = session.getSessionSnapshot().generation;
    localStorage.setItem('unrelated', 'value');
    storageEvent('unrelated');
    storageEvent('foody.accessToken');
    assert.equal(session.getSessionSnapshot().generation, generation);
    assert.equal(session.getSessionSnapshot().user.id, 1);
  } finally { stop(); }
});

test('cross-tab removal of either token logs out even when the remaining credential is valid', async () => {
  const stop = auth.startAuthSession();
  try {
    for (const key of ['foody.accessToken', 'foody.refreshToken']) {
      establish();
      let calls = 0;
      globalThis.fetch = async (url) => { calls++; return url.endsWith('/refresh') ? json(tokens(1)) : json(user(1)); };
      localStorage.removeItem(key);
      storageEvent(key);
      await auth.restoreSession();
      loggedOut();
      assert.equal(calls, 0);
    }
  } finally { stop(); }
});
