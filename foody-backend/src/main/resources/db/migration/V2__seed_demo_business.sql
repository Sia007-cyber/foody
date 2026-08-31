-- V2: Seed reference data + one demo business so Phase 0 acceptance ("a business exists
-- and is viewable") is satisfied out of the box. The bcrypt hash below is for
-- password 'password123' (bcrypt cost 12) and is present only so the seeded owner
-- account is usable for manual exploration.

INSERT INTO business_types (code, label, is_active) VALUES
    ('CAFE', 'Cafe', TRUE),
    ('FAST_FOOD', 'Fast Food', TRUE);

-- Demo business owner (BUSINESS_OWNER). Password: password123
INSERT INTO users (id, email, phone, password_hash, full_name, role, status)
VALUES (1, 'owner@foody.test', '+10000000000',
        '$2b$12$NS4HyOdHTn5GqiPkubBk8uF7IGf.f80wzp2p0/a1BCL/CfA209hDO',
        'Demo Owner', 'BUSINESS_OWNER', 'ACTIVE');

-- Demo approved cafe.
INSERT INTO businesses (id, owner_user_id, name, description, business_type, address,
                        latitude, longitude, phone, status, cover_image_url)
VALUES (1, 1, 'Cafe Sunrise', 'A cozy neighborhood cafe serving coffee and pastries.',
        'CAFE', '123 Bean Street', 35.6892, 51.3890, '+10000000001', 'APPROVED',
        'https://example.com/cafe-sunrise.jpg');

-- Business hours: Mon-Sat 08:00-20:00, Sunday closed.
INSERT INTO business_hours (business_id, day_of_week, open_time, close_time, is_closed) VALUES
    (1, 1, '08:00', '20:00', FALSE),
    (1, 2, '08:00', '20:00', FALSE),
    (1, 3, '08:00', '20:00', FALSE),
    (1, 4, '08:00', '20:00', FALSE),
    (1, 5, '08:00', '20:00', FALSE),
    (1, 6, '08:00', '20:00', FALSE),
    (1, 7, '00:00', '00:00', TRUE);

-- One menu with a few products.
INSERT INTO menus (id, business_id, name, display_order) VALUES (1, 1, 'Main Menu', 1);

INSERT INTO products (id, menu_id, name, description, price, image_url, is_available, display_order) VALUES
    (1, 1, 'Espresso', 'Single shot of rich espresso.', 2.50, 'https://example.com/espresso.jpg', TRUE, 1),
    (2, 1, 'Cappuccino', 'Espresso with steamed milk foam.', 3.50, 'https://example.com/cappuccino.jpg', TRUE, 2),
    (3, 1, 'Croissant', 'Buttery flaky pastry.', 2.00, 'https://example.com/croissant.jpg', TRUE, 3),
    (4, 1, 'Latte', 'Espresso with a generous amount of steamed milk.', 4.00, 'https://example.com/latte.jpg', FALSE, 4);
