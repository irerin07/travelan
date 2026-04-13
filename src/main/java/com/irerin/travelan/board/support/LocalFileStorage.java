package com.irerin.travelan.board.support;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.LocalDate;
import java.util.UUID;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;
import org.springframework.web.multipart.MultipartFile;

@Component
public class LocalFileStorage implements FileStorage {

    private final Path rootPath;

    public LocalFileStorage(@Value("${app.upload.root:./uploads}") String root) {
        this.rootPath = Path.of(root);
    }

    @Override
    public String store(Long userId, MultipartFile file) {
        String ext = StringUtils.getFilenameExtension(file.getOriginalFilename());
        LocalDate now = LocalDate.now();
        String dir = String.format("posts/%d/%d/%02d", userId, now.getYear(), now.getMonthValue());
        String filename = UUID.randomUUID() + "." + ext;

        Path dirPath = rootPath.resolve(dir);
        Path filePath = dirPath.resolve(filename);

        try {
            Files.createDirectories(dirPath);
            file.transferTo(filePath);
        } catch (IOException e) {
            throw new RuntimeException("파일 저장에 실패했습니다", e);
        }

        return dir + "/" + filename;
    }

    @Override
    public void delete(String path) {
        try {
            Files.deleteIfExists(rootPath.resolve(path));
        } catch (IOException e) {
            throw new RuntimeException("파일 삭제에 실패했습니다", e);
        }
    }

    @Override
    public String toUrl(String path) {
        return "/uploads/" + path;
    }
}
