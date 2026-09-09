-- Forward-only removal of published demo credentials and public demo content.
-- Keep rows and references intact so existing operational history is preserved.
UPDATE businesses b JOIN users u ON u.id = b.owner_user_id
SET b.status = 'SUSPENDED', b.version = b.version + 1
WHERE u.id = 1 AND u.email = 'owner@foody.test';

UPDATE users SET status = 'SUSPENDED', password_hash = '!disabled-demo-account!'
WHERE (id = 1 AND email = 'owner@foody.test')
   OR (id = 2 AND email = 'admin@foody.test');

UPDATE refresh_token_sessions s JOIN users u ON u.id = s.user_id
SET s.revoked_at = CURRENT_TIMESTAMP(6)
WHERE ((u.id = 1 AND u.email = 'owner@foody.test')
    OR (u.id = 2 AND u.email = 'admin@foody.test')) AND s.revoked_at IS NULL;
