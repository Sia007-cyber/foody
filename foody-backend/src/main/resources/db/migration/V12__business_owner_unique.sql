-- Enforce the one-business-per-owner invariant even when registrations race.
ALTER TABLE businesses
    ADD CONSTRAINT uk_businesses_owner_user_id UNIQUE (owner_user_id);
