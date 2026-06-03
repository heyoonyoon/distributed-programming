package com.distribution.insurance.service;

import com.distribution.insurance.domain.claim.ClaimAttachment;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatCode;

class LocalFileStorageTest {

    @Test
    void 파일을_저장하고_메타를_반환한다(@TempDir Path tempDir) throws IOException {
        FileStorage storage = new LocalFileStorage(tempDir.toString());
        MultipartFile f = new MockMultipartFile("file", "r.pdf", "application/pdf", new byte[]{1, 2, 3});

        ClaimAttachment meta = storage.store(100L, f);

        assertThat(meta.getFilename()).isEqualTo("r.pdf");
        assertThat(meta.getContentType()).isEqualTo("application/pdf");
        assertThat(meta.getSizeBytes()).isEqualTo(3);
        assertThat(Files.exists(Path.of(meta.getStoredPath()))).isTrue();
        assertThat(meta.getStoredPath()).contains("100");
    }

    /** Fix 5: path traversal 시도 — 파일명이 basename으로 sanitize되어 owner 디렉터리 안에 저장된다. */
    @Test
    void 경로순회_파일명은_basename으로_sanitize되어_owner디렉터리에_저장된다(@TempDir Path tempDir) throws IOException {
        FileStorage storage = new LocalFileStorage(tempDir.toString());
        MultipartFile evil = new MockMultipartFile("file", "../../evil.pdf", "application/pdf", new byte[]{9});

        ClaimAttachment meta = storage.store(200L, evil);

        // filename must be just the basename
        assertThat(meta.getFilename()).isEqualTo("evil.pdf");
        // stored path must be inside owner dir
        Path ownerDir = tempDir.resolve("200").normalize();
        Path stored = Path.of(meta.getStoredPath()).normalize();
        assertThat(stored.startsWith(ownerDir)).isTrue();
        // the file must actually exist there
        assertThat(Files.exists(stored)).isTrue();
    }

    /** Fix 4: delete() — 파일 삭제 후 존재하지 않는다. */
    @Test
    void delete_호출하면_파일이_삭제된다(@TempDir Path tempDir) throws IOException {
        FileStorage storage = new LocalFileStorage(tempDir.toString());
        MultipartFile f = new MockMultipartFile("file", "del.pdf", "application/pdf", new byte[]{5});
        ClaimAttachment meta = storage.store(300L, f);
        assertThat(Files.exists(Path.of(meta.getStoredPath()))).isTrue();

        storage.delete(meta.getStoredPath());

        assertThat(Files.exists(Path.of(meta.getStoredPath()))).isFalse();
    }

    /** Fix 4: delete() on non-existent path must not throw. */
    @Test
    void delete_존재하지않는_경로는_예외없이_무시된다(@TempDir Path tempDir) {
        FileStorage storage = new LocalFileStorage(tempDir.toString());
        assertThatCode(() -> storage.delete(tempDir.resolve("no_such_file.pdf").toString()))
                .doesNotThrowAnyException();
    }
}
