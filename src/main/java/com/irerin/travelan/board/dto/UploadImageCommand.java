package com.irerin.travelan.board.dto;

import java.util.List;

import org.springframework.web.multipart.MultipartFile;

import lombok.AccessLevel;
import lombok.Builder;
import lombok.Getter;

/**
 * 이미지 업로드 Command.
 *
 * 현재는 MultipartFile을 Command로 래핑하여 레이어 분리 컨벤션을 따른다.
 * 대안: Service 메서드에 (Long userId, List<MultipartFile> files) 파라미터를 직접 전달하는 방식도 가능.
 * MultipartFile은 웹 레이어 객체이므로 Command에 포함시키는 것이 어색할 수 있음.
 * 추후 S3 등 외부 스토리지 전환 시 Command 구조를 재검토할 것.
 */
@Getter
public class UploadImageCommand {

    private final Long userId;
    private final List<MultipartFile> files;

    @Builder(access = AccessLevel.PRIVATE)
    private UploadImageCommand(Long userId, List<MultipartFile> files) {
        this.userId = userId;
        this.files = files;
    }

    public static UploadImageCommand from(Long userId, List<MultipartFile> files) {
        return UploadImageCommand.builder()
            .userId(userId)
            .files(files)
            .build();
    }
}
