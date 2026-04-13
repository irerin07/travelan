package com.irerin.travelan.board.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.BDDMockito.given;

import java.util.ArrayList;
import java.util.List;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.test.util.ReflectionTestUtils;
import org.springframework.web.multipart.MultipartFile;

import com.irerin.travelan.board.dto.PostImageResponse;
import com.irerin.travelan.board.dto.UploadImageCommand;
import com.irerin.travelan.board.entity.PostImage;
import com.irerin.travelan.board.repository.PostImageRepository;
import com.irerin.travelan.board.support.FileStorage;
import com.irerin.travelan.common.exception.InvalidFileException;

@ExtendWith(MockitoExtension.class)
class ImageUploadServiceTest {

    @Mock FileStorage fileStorage;
    @Mock PostImageRepository postImageRepository;
    @InjectMocks ImageUploadService imageUploadService;

    @Test
    void upload_successfullyUploadsAndReturnsResponses() {
        MockMultipartFile file = new MockMultipartFile(
            "images", "photo.jpg", "image/jpeg", "data".getBytes());
        UploadImageCommand command = UploadImageCommand.from(1L, List.of(file));

        given(fileStorage.store(eq(1L), any())).willReturn("posts/1/2026/04/uuid.jpg");
        given(fileStorage.toUrl("posts/1/2026/04/uuid.jpg")).willReturn("/uploads/posts/1/2026/04/uuid.jpg");
        given(postImageRepository.save(any(PostImage.class))).willAnswer(inv -> {
            PostImage img = inv.getArgument(0);
            ReflectionTestUtils.setField(img, "id", 1L);
            return img;
        });

        List<PostImageResponse> result = imageUploadService.upload(command);

        assertThat(result).hasSize(1);
        assertThat(result.get(0).getUrl()).isEqualTo("/uploads/posts/1/2026/04/uuid.jpg");
    }

    @Test
    void upload_throwsWhenExtensionNotAllowed() {
        MockMultipartFile file = new MockMultipartFile(
            "images", "doc.pdf", "application/pdf", "data".getBytes());
        UploadImageCommand command = UploadImageCommand.from(1L, List.of(file));

        assertThatThrownBy(() -> imageUploadService.upload(command))
            .isInstanceOf(InvalidFileException.class)
            .hasMessageContaining("확장자");
    }

    @Test
    void upload_throwsWhenContentTypeNotAllowed() {
        MockMultipartFile file = new MockMultipartFile(
            "images", "photo.jpg", "text/plain", "data".getBytes());
        UploadImageCommand command = UploadImageCommand.from(1L, List.of(file));

        assertThatThrownBy(() -> imageUploadService.upload(command))
            .isInstanceOf(InvalidFileException.class)
            .hasMessageContaining("파일 형식");
    }

    @Test
    void upload_throwsWhenFileSizeExceedsLimit() {
        byte[] largeData = new byte[6 * 1024 * 1024];
        MockMultipartFile file = new MockMultipartFile(
            "images", "photo.jpg", "image/jpeg", largeData);
        UploadImageCommand command = UploadImageCommand.from(1L, List.of(file));

        assertThatThrownBy(() -> imageUploadService.upload(command))
            .isInstanceOf(InvalidFileException.class)
            .hasMessageContaining("5MB");
    }

    @Test
    void upload_throwsWhenFileCountExceedsLimit() {
        List<MultipartFile> files = new ArrayList<>();
        for (int i = 0; i < 11; i++) {
            files.add(new MockMultipartFile(
                "images", "photo" + i + ".jpg", "image/jpeg", "data".getBytes()));
        }
        UploadImageCommand command = UploadImageCommand.from(1L, files);

        assertThatThrownBy(() -> imageUploadService.upload(command))
            .isInstanceOf(InvalidFileException.class)
            .hasMessageContaining("10장");
    }
}
