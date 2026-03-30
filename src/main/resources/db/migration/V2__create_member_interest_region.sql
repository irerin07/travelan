CREATE TABLE member_interest_region
(
    id        BIGINT       NOT NULL AUTO_INCREMENT,
    member_id BIGINT       NOT NULL,
    region    VARCHAR(100) NOT NULL,
    PRIMARY KEY (id),
    CONSTRAINT fk_mir_member FOREIGN KEY (member_id) REFERENCES member (id)
);
