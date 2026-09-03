-- V9: Full user profile — every role (customer, business owner, admin) can set
-- their exact address, phone (already existed), full name (already existed) and
-- a profile picture from the profile page next to the notification bell.

ALTER TABLE users
    ADD COLUMN address VARCHAR(512) NULL AFTER phone,
    ADD COLUMN profile_image_url VARCHAR(512) NULL AFTER address;
