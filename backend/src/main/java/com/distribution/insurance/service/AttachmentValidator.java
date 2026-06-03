package com.distribution.insurance.service;

import org.springframework.stereotype.Component;
import org.springframework.web.multipart.MultipartFile;

import java.util.Set;

/** 청구 증빙 검증: 허용 타입(PDF/JPG/PNG), 개당 10MB 이하(UC05 E1, UC09 E1). */
@Component
public class AttachmentValidator {

    private static final Set<String> ALLOWED = Set.of("application/pdf", "image/jpeg", "image/png");
    private static final long MAX_BYTES = 10L * 1024 * 1024;

    public void validate(MultipartFile file) {
        if (!ALLOWED.contains(file.getContentType())) {
            throw new InvalidRequestException("지원하지 않는 파일 형식입니다. (허용: PDF, JPG, PNG)");
        }
        if (file.getSize() > MAX_BYTES) {
            throw new InvalidRequestException("파일 크기는 개당 10MB 이하여야 합니다.");
        }
    }
}
