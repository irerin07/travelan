package com.irerin.travelan.board.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;

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

    private static final byte[] JPEG_BYTES = {(byte) 0xFF, (byte) 0xD8, (byte) 0xFF, (byte) 0xE0, 0, 0, 0, 0, 0, 0, 0, 0};
    private static final byte[] PNG_BYTES = {(byte) 0x89, 0x50, 0x4E, 0x47, 0, 0, 0, 0, 0, 0, 0, 0};

    @Mock FileStorage fileStorage;
    @Mock PostImageRepository postImageRepository;
    @InjectMocks ImageUploadService imageUploadService;

    @Test
    void upload_successfullyUploadsAndReturnsResponses() {
        MockMultipartFile file = new MockMultipartFile(
            "images", "photo.jpg", "image/jpeg", JPEG_BYTES);
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
            "images", "doc.pdf", "application/pdf", JPEG_BYTES);
        UploadImageCommand command = UploadImageCommand.from(1L, List.of(file));

        assertThatThrownBy(() -> imageUploadService.upload(command))
            .isInstanceOf(InvalidFileException.class)
            .hasMessageContaining("확장자");
    }

    @Test
    void upload_throwsWhenContentTypeNotAllowed() {
        MockMultipartFile file = new MockMultipartFile(
            "images", "photo.jpg", "text/plain", JPEG_BYTES);
        UploadImageCommand command = UploadImageCommand.from(1L, List.of(file));

        assertThatThrownBy(() -> imageUploadService.upload(command))
            .isInstanceOf(InvalidFileException.class)
            .hasMessageContaining("파일 형식");
    }

    @Test
    void upload_throwsWhenFileSizeExceedsLimit() {
        byte[] largeData = new byte[6 * 1024 * 1024];
        System.arraycopy(JPEG_BYTES, 0, largeData, 0, JPEG_BYTES.length);
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
                "images", "photo" + i + ".jpg", "image/jpeg", JPEG_BYTES));
        }
        UploadImageCommand command = UploadImageCommand.from(1L, files);

        assertThatThrownBy(() -> imageUploadService.upload(command))
            .isInstanceOf(InvalidFileException.class)
            .hasMessageContaining("10장");
    }

    @Test
    void upload_throwsWhenMagicBytesDoNotMatchExtension() {
        byte[] svgBytes = "<svg xmlns=\"http://www.w3.org/2000/svg\"><script>alert(1)</script></svg>".getBytes();
        MockMultipartFile file = new MockMultipartFile(
            "images", "evil.jpg", "image/jpeg", svgBytes);
        UploadImageCommand command = UploadImageCommand.from(1L, List.of(file));

        assertThatThrownBy(() -> imageUploadService.upload(command))
            .isInstanceOf(InvalidFileException.class)
            .hasMessageContaining("파일 내용이 허용된 이미지 형식과 일치하지 않습니다");
    }

    @Test
    void upload_acceptsPngMagicBytes() {
        MockMultipartFile file = new MockMultipartFile(
            "images", "photo.png", "image/png", PNG_BYTES);
        UploadImageCommand command = UploadImageCommand.from(1L, List.of(file));

        given(fileStorage.store(eq(1L), any())).willReturn("posts/1/2026/04/uuid.png");
        given(fileStorage.toUrl(any())).willReturn("/uploads/posts/1/2026/04/uuid.png");
        given(postImageRepository.save(any(PostImage.class))).willAnswer(inv -> {
            PostImage img = inv.getArgument(0);
            ReflectionTestUtils.setField(img, "id", 2L);
            return img;
        });

        List<PostImageResponse> result = imageUploadService.upload(command);
        assertThat(result).hasSize(1);
    }

    @Test
    void upload_doesNotStoreFilesWhenLaterFileFailsValidation() {
        MockMultipartFile validFile = new MockMultipartFile(
            "images", "valid.jpg", "image/jpeg", JPEG_BYTES);
        byte[] svgBytes = "<svg>bad</svg>".getBytes();
        MockMultipartFile invalidFile = new MockMultipartFile(
            "images", "evil.jpg", "image/jpeg", svgBytes);
        UploadImageCommand command = UploadImageCommand.from(1L, List.of(validFile, invalidFile));

        assertThatThrownBy(() -> imageUploadService.upload(command))
            .isInstanceOf(InvalidFileException.class);

        verify(fileStorage, never()).store(any(), any());
    }
}
