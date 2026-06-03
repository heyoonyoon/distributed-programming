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
import java.util.logging.Logger;

/** 로컬 디스크 저장: {upload.dir}/{ownerId}/{uuid}_{원본파일명}. */
@Component
public class LocalFileStorage implements FileStorage {

    private static final Logger log = Logger.getLogger(LocalFileStorage.class.getName());

    private final String baseDir;

    public LocalFileStorage(@Value("${insurance.upload.dir:./uploads/claims}") String baseDir) {
        this.baseDir = baseDir;
    }

    @Override
    public ClaimAttachment store(Long ownerId, MultipartFile file) {
        try {
            Path dir = Path.of(baseDir, String.valueOf(ownerId));
            Files.createDirectories(dir);

            // Fix 5: sanitize original filename to basename only (prevent path traversal)
            String raw = file.getOriginalFilename() == null ? "file" : file.getOriginalFilename();
            String basename = Path.of(raw).getFileName().toString()
                    .replace("/", "").replace("\\", "");
            if (basename.isBlank()) basename = "file";

            Path target = dir.resolve(UUID.randomUUID() + "_" + basename).normalize();
            if (!target.startsWith(dir.normalize())) {
                throw new InvalidRequestException("허용되지 않는 파일 경로입니다.");
            }

            file.transferTo(target);
            return new ClaimAttachment(basename, file.getContentType(), file.getSize(),
                    target.toAbsolutePath().toString());
        } catch (IOException e) {
            throw new UncheckedIOException("증빙 저장 실패", e);
        }
    }

    @Override
    public void delete(String storedPath) {
        try {
            Files.deleteIfExists(Path.of(storedPath));
        } catch (IOException e) {
            log.warning("파일 삭제 실패(무시): " + storedPath + " — " + e.getMessage());
        }
    }
}
