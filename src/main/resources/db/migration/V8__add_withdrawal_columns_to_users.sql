ALTER TABLE users
    ADD COLUMN original_email VARCHAR(255) NULL,
    ADD COLUMN withdrawn_at DATETIME NULL;
