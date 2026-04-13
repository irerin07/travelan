CREATE TABLE post_images
(
    id            BIGINT       NOT NULL AUTO_INCREMENT,
    post_id       BIGINT       NULL,
    url           VARCHAR(500) NOT NULL,
    original_name VARCHAR(255) NOT NULL,
    size          BIGINT       NOT NULL,
    display_order INT          NOT NULL DEFAULT 0,
    created_at    DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP,
    PRIMARY KEY (id),
    KEY idx_post_images_post_id (post_id),
    CONSTRAINT fk_post_images_post FOREIGN KEY (post_id) REFERENCES posts (id)
);
