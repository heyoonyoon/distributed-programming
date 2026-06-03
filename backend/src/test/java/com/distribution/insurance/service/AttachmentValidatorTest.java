package com.distribution.insurance.service;

import org.junit.jupiter.api.Test;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.web.multipart.MultipartFile;

import static org.assertj.core.api.Assertions.*;

class AttachmentValidatorTest {

    private final AttachmentValidator validator = new AttachmentValidator();

    @Test
    void 허용타입은_통과한다() {
        MultipartFile f = new MockMultipartFile("file", "r.pdf", "application/pdf", new byte[]{1, 2, 3});
        assertThatNoException().isThrownBy(() -> validator.validate(f));
    }

    @Test
    void 허용되지_않은_타입은_예외() {
        MultipartFile f = new MockMultipartFile("file", "a.txt", "text/plain", new byte[]{1});
        assertThatThrownBy(() -> validator.validate(f))
                .isInstanceOf(InvalidRequestException.class)
                .hasMessageContaining("지원하지 않는 파일 형식");
    }

    @Test
    void 파일크기_10MB_초과는_예외() {
        byte[] big = new byte[10 * 1024 * 1024 + 1];
        MultipartFile f = new MockMultipartFile("file", "big.png", "image/png", big);
        assertThatThrownBy(() -> validator.validate(f))
                .isInstanceOf(InvalidRequestException.class)
                .hasMessageContaining("10MB");
    }
}
