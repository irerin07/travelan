CREATE TABLE post_reports
(
    id          BIGINT      NOT NULL AUTO_INCREMENT,
    post_id     BIGINT      NOT NULL,
    reporter_id BIGINT      NOT NULL,
    reason      VARCHAR(30) NOT NULL,
    created_at  DATETIME    NOT NULL DEFAULT CURRENT_TIMESTAMP,
    PRIMARY KEY (id),
    UNIQUE KEY uq_post_reports_post_reporter (post_id, reporter_id),
    KEY idx_post_reports_post_id (post_id),
    CONSTRAINT fk_post_reports_post     FOREIGN KEY (post_id)     REFERENCES posts (id) ON DELETE CASCADE,
    CONSTRAINT fk_post_reports_reporter FOREIGN KEY (reporter_id) REFERENCES users (id) ON DELETE CASCADE
);
