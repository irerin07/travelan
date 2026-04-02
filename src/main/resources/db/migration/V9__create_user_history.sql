CREATE TABLE user_history (
    id          BIGINT       NOT NULL AUTO_INCREMENT,
    user_id     BIGINT       NOT NULL,
    action      VARCHAR(30)  NOT NULL,
    field       VARCHAR(50)  NULL,
    old_value   VARCHAR(500) NULL,
    new_value   VARCHAR(500) NULL,
    created_at  DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP,
    PRIMARY KEY (id),
    CONSTRAINT fk_uh_user FOREIGN KEY (user_id) REFERENCES users(id),
    INDEX idx_user_history_user_id (user_id)
);
