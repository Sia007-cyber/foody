-- Homepage curation is explicit: existing businesses are not promoted by default.
ALTER TABLE businesses
    ADD COLUMN featured BOOLEAN NOT NULL DEFAULT FALSE,
    ADD COLUMN popular BOOLEAN NOT NULL DEFAULT FALSE;

CREATE INDEX idx_businesses_public_featured ON businesses (status, featured, updated_at);
CREATE INDEX idx_businesses_public_popular ON businesses (status, popular, updated_at);
