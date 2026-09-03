-- V10: Let every role (customer, business owner, admin) attach a precise
-- location (not just free-text address) to their profile, same columns
-- already used by businesses (see V1__init.sql).

ALTER TABLE users
    ADD COLUMN latitude  DECIMAL(10, 8) NULL AFTER address,
    ADD COLUMN longitude DECIMAL(11, 8) NULL AFTER latitude;
