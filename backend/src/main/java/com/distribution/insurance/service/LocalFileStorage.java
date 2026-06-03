package com.distribution.insurance.service;

import com.distribution.insurance.domain.claim.ClaimAttachment;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.io.UncheckedIOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.UUID;

/** 로컬 디스크 저장: {upload.dir}/{ownerId}/{uuid}_{원본파일명}. */
@Component
public class LocalFileStorage implements FileStorage {

    private final String baseDir;

    public LocalFileStorage(@Value("${insurance.upload.dir:./uploads/claims}") String baseDir) {
        this.baseDir = baseDir;
    }

    @Override
    public ClaimAttachment store(Long ownerId, MultipartFile file) {
        try {
            Path dir = Path.of(baseDir, String.valueOf(ownerId));
            Files.createDirectories(dir);
            String original = file.getOriginalFilename() == null ? "file" : file.getOriginalFilename();
            Path target = dir.resolve(UUID.randomUUID() + "_" + original);
            file.transferTo(target);
            return new ClaimAttachment(original, file.getContentType(), file.getSize(),
                    target.toAbsolutePath().toString());
        } catch (IOException e) {
            throw new UncheckedIOException("증빙 저장 실패", e);
        }
    }
}
