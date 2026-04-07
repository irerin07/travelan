ALTER TABLE users
    ADD COLUMN login_fail_count INT NOT NULL DEFAULT 0,
    ADD COLUMN locked_until DATETIME NULL;
