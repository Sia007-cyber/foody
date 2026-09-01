-- V5: Seed an ADMIN account so the admin panel is testable out of the box.
-- Without this there is no way to reach /api/admin/** — no signup flow creates ADMIN
-- users (RegisterRequest is hardcoded to CUSTOMER, see auth/dto/RegisterRequest.java).
-- The bcrypt hash below is for password 'password123' (bcrypt cost 12), same as the
-- demo owner seeded in V2, purely for local/manual exploration.

INSERT INTO users (id, email, phone, password_hash, full_name, role, status)
VALUES (2, 'admin@foody.test', '+10000000002',
        '$2b$12$NS4HyOdHTn5GqiPkubBk8uF7IGf.f80wzp2p0/a1BCL/CfA209hDO',
        'Demo Admin', 'ADMIN', 'ACTIVE');
