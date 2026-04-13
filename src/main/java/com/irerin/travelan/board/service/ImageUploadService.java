package com.irerin.travelan.board.service;

import java.util.ArrayList;
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

        List<PostImageResponse> responses = new ArrayList<>();

        for (MultipartFile file : files) {
            validate(file);
            String path = fileStorage.store(command.getUserId(), file);
            String url = fileStorage.toUrl(path);
            PostImage image = PostImage.of(url, file.getOriginalFilename(), file.getSize());
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
    }
}
