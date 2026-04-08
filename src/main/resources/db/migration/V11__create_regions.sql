CREATE TABLE regions
(
    id            BIGINT       NOT NULL AUTO_INCREMENT,
    code          VARCHAR(50)  NOT NULL,
    name          VARCHAR(50)  NOT NULL,
    description   VARCHAR(255),
    display_order INT          NOT NULL,
    active        BIT(1)       NOT NULL DEFAULT 1,
    PRIMARY KEY (id),
    UNIQUE KEY uk_regions_code (code)
);

INSERT INTO regions (code, name, description, display_order, active) VALUES
    ('seoul',     '서울',       '서울특별시',               1,  1),
    ('gyeonggi',  '경기',       '경기도',                    2,  1),
    ('incheon',   '인천',       '인천광역시',               3,  1),
    ('gangwon',   '강원',       '강원특별자치도',           4,  1),
    ('chungcheong','충청',      '충청북도 및 충청남도',     5,  1),
    ('jeolla',    '전라',       '전라북도 및 전라남도',     6,  1),
    ('gyeongsang','경상',       '경상북도 및 경상남도',     7,  1),
    ('jeju',      '제주',       '제주특별자치도',           8,  1),
    ('japan',     '일본',       '일본',                      9,  1),
    ('china',     '중국',       '중국',                     10,  1),
    ('vietnam',   '베트남',     '베트남',                   11,  1),
    ('thailand',  '태국',       '태국',                     12,  1),
    ('philippines','필리핀',    '필리핀',                   13,  1),
    ('taiwan',    '대만',       '대만',                     14,  1),
    ('usa',       '미국',       '미국',                     15,  1),
    ('overseas_etc','기타 해외','기타 해외 여행지',         16,  1);
