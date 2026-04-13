package com.irerin.travelan.board.support;

import org.springframework.web.multipart.MultipartFile;

public interface FileStorage {

    String store(Long userId, MultipartFile file);

    void delete(String path);

    String toUrl(String path);
}
