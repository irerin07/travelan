CREATE TABLE user_interest_region
(
    id      BIGINT       NOT NULL AUTO_INCREMENT,
    user_id BIGINT       NOT NULL,
    region  VARCHAR(100) NOT NULL,
    PRIMARY KEY (id),
    CONSTRAINT fk_uir_user FOREIGN KEY (user_id) REFERENCES users (id)
);
