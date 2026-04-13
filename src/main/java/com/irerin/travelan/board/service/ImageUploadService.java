package com.irerin.travelan.board.service;

import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Set;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;
import org.springframework.web.multipart.MultipartFile;

import com.irerin.travelan.board.dto.PostImageResponse;
import com.irerin.travelan.board.dto.UploadImageCommand;
import com.irerin.travelan.board.entity.PostImage;
import com.irerin.travelan.board.repository.PostImageRepository;
import com.irerin.travelan.board.support.FileStorage;
import com.irerin.travelan.common.exception.InvalidFileException;

import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class ImageUploadService {

    private static final Set<String> ALLOWED_EXTENSIONS = Set.of("jpg", "jpeg", "png", "webp");
    private static final Set<String> ALLOWED_CONTENT_TYPES = Set.of("image/jpeg", "image/png", "image/webp");
    private static final long MAX_FILE_SIZE = 5 * 1024 * 1024;
    private static final int MAX_FILE_COUNT = 10;

    private final FileStorage fileStorage;
    private final PostImageRepository postImageRepository;

    @Transactional
    public List<PostImageResponse> upload(UploadImageCommand command) {
        List<MultipartFile> files = command.getFiles();

        if (files.size() > MAX_FILE_COUNT) {
            throw new InvalidFileException("이미지는 최대 " + MAX_FILE_COUNT + "장까지 업로드할 수 있습니다");
        }

        // Pass 1: 모든 파일 검증 (파일 시스템 I/O 전에 실패 가능)
        for (MultipartFile file : files) {
            validate(file);
        }

        // Pass 2: 검증 통과 후 저장
        List<PostImageResponse> responses = new ArrayList<>();
        for (MultipartFile file : files) {
            String path = fileStorage.store(command.getUserId(), file);
            String url = fileStorage.toUrl(path);
            PostImage image = PostImage.of(url, file.getOriginalFilename(), file.getSize(), command.getUserId());
            postImageRepository.save(image);
            responses.add(PostImageResponse.from(image));
        }

        return responses;
    }

    private void validate(MultipartFile file) {
        String ext = StringUtils.getFilenameExtension(file.getOriginalFilename());
        if (ext == null || !ALLOWED_EXTENSIONS.contains(ext.toLowerCase())) {
            throw new InvalidFileException("허용되지 않는 파일 확장자입니다. (jpg, png, webp만 허용)");
        }

        String contentType = file.getContentType();
        if (contentType == null || !ALLOWED_CONTENT_TYPES.contains(contentType)) {
            throw new InvalidFileException("허용되지 않는 파일 형식입니다");
        }

        if (file.getSize() > MAX_FILE_SIZE) {
            throw new InvalidFileException("파일 크기는 5MB 이하여야 합니다");
        }

        validateMagicBytes(file);
    }

    private void validateMagicBytes(MultipartFile file) {
        byte[] header;
        try (InputStream is = file.getInputStream()) {
            header = is.readNBytes(12);
        } catch (IOException e) {
            throw new InvalidFileException("파일을 읽을 수 없습니다");
        }

        if (header.length < 4) {
            throw new InvalidFileException("파일 내용이 허용된 이미지 형식과 일치하지 않습니다");
        }

        boolean isJpeg = header.length >= 3
            && header[0] == (byte) 0xFF && header[1] == (byte) 0xD8 && header[2] == (byte) 0xFF;

        boolean isPng = header.length >= 4
            && header[0] == (byte) 0x89 && header[1] == (byte) 0x50
            && header[2] == (byte) 0x4E && header[3] == (byte) 0x47;

        boolean isWebp = header.length >= 12
            && header[0] == (byte) 0x52 && header[1] == (byte) 0x49
            && header[2] == (byte) 0x46 && header[3] == (byte) 0x46
            && header[8] == (byte) 0x57 && header[9] == (byte) 0x45
            && header[10] == (byte) 0x42 && header[11] == (byte) 0x50;

        if (!isJpeg && !isPng && !isWebp) {
            throw new InvalidFileException("파일 내용이 허용된 이미지 형식과 일치하지 않습니다");
        }
    }
}
