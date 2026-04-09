CREATE TABLE posts
(
    id         BIGINT       NOT NULL AUTO_INCREMENT,
    region_id  BIGINT       NOT NULL,
    author_id  BIGINT       NOT NULL,
    title      VARCHAR(150) NOT NULL,
    content    TEXT         NOT NULL,
    view_count BIGINT       NOT NULL DEFAULT 0,
    status     VARCHAR(20)  NOT NULL DEFAULT 'PUBLISHED',
    created_at DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    PRIMARY KEY (id),
    KEY idx_posts_region_status (region_id, status),
    CONSTRAINT fk_posts_region FOREIGN KEY (region_id) REFERENCES regions (id),
    CONSTRAINT fk_posts_author FOREIGN KEY (author_id) REFERENCES users (id)
);
