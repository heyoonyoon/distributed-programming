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
}
