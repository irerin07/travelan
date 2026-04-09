CREATE TABLE post_history
(
    id         BIGINT       NOT NULL AUTO_INCREMENT,
    post_id    BIGINT       NOT NULL,
    action     VARCHAR(20)  NOT NULL,
    title      VARCHAR(150) NOT NULL,
    content    TEXT         NOT NULL,
    editor_id  BIGINT       NOT NULL,
    created_at DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP,
    PRIMARY KEY (id),
    KEY idx_post_history_post_id (post_id, created_at),
    CONSTRAINT fk_post_history_post FOREIGN KEY (post_id) REFERENCES posts (id),
    CONSTRAINT fk_post_history_editor FOREIGN KEY (editor_id) REFERENCES users (id)
);
