-- 내부 규칙: 탈퇴한 유저 데이터는 1년간 보관 후 하드 삭제한다.
-- 하드 삭제 시 신고 기록은 모더레이션·감사 목적으로 보존하되, 신고자 참조만 해제한다.
-- RESTRICT → 하드 삭제 차단, CASCADE → 신고 이력 소실이므로 SET NULL 채택.
CREATE TABLE post_reports
(
    id          BIGINT      NOT NULL AUTO_INCREMENT,
    post_id     BIGINT      NOT NULL,
    reporter_id BIGINT      NULL,
    reason      VARCHAR(30) NOT NULL,
    created_at  DATETIME    NOT NULL DEFAULT CURRENT_TIMESTAMP,
    PRIMARY KEY (id),
    UNIQUE KEY uq_post_reports_post_reporter (post_id, reporter_id),
    CONSTRAINT fk_post_reports_post     FOREIGN KEY (post_id)     REFERENCES posts (id) ON DELETE CASCADE,
    CONSTRAINT fk_post_reports_reporter FOREIGN KEY (reporter_id) REFERENCES users (id) ON DELETE SET NULL
);
