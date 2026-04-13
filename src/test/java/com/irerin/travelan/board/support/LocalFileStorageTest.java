package com.irerin.travelan.board.support;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import org.springframework.mock.web.MockMultipartFile;

class LocalFileStorageTest {

    @TempDir
    Path tempDir;
    private LocalFileStorage storage;

    @BeforeEach
    void setUp() {
        storage = new LocalFileStorage(tempDir.toString());
    }

    @Test
    void store_createsFileAndReturnsPath() {
        MockMultipartFile file = new MockMultipartFile(
            "image", "photo.jpg", "image/jpeg", "fake-image".getBytes());

        String path = storage.store(1L, file);

        assertThat(path).startsWith("posts/1/");
        assertThat(path).endsWith(".jpg");
        assertThat(Files.exists(tempDir.resolve(path))).isTrue();
    }

    @Test
    void delete_removesFile() throws IOException {
        MockMultipartFile file = new MockMultipartFile(
            "image", "photo.png", "image/png", "data".getBytes());
        String path = storage.store(1L, file);

        assertThat(Files.exists(tempDir.resolve(path))).isTrue();

        storage.delete(path);

        assertThat(Files.exists(tempDir.resolve(path))).isFalse();
    }

    @Test
    void toUrl_prependsUploadsPrefix() {
        String url = storage.toUrl("posts/1/2026/04/abc.jpg");
        assertThat(url).isEqualTo("/uploads/posts/1/2026/04/abc.jpg");
    }
}
