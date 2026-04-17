-- 내부 규칙: 탈퇴한 유저 데이터는 1년간 보관 후 하드 삭제한다.
-- 하드 삭제 시 신고 기록은 모더레이션·감사 목적으로 보존하되, 신고자 참조만 해제한다.
-- RESTRICT → 하드 삭제 차단, CASCADE → 신고 이력 소실이므로 SET NULL 채택.
ALTER TABLE post_reports MODIFY reporter_id BIGINT NULL;

ALTER TABLE post_reports DROP FOREIGN KEY fk_post_reports_reporter;

ALTER TABLE post_reports
    ADD CONSTRAINT fk_post_reports_reporter
        FOREIGN KEY (reporter_id) REFERENCES users (id) ON DELETE SET NULL;
