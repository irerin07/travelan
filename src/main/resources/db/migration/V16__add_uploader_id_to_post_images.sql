ALTER TABLE post_images
    ADD COLUMN uploader_id BIGINT NOT NULL AFTER post_id,
    ADD CONSTRAINT fk_post_images_uploader FOREIGN KEY (uploader_id) REFERENCES users (id);
