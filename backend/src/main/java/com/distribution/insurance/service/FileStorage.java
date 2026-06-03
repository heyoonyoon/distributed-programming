package com.distribution.insurance.service;

import com.distribution.insurance.domain.claim.ClaimAttachment;
import org.springframework.web.multipart.MultipartFile;

/** 청구 증빙 바이너리를 저장하고 메타를 반환한다. ownerId는 저장 디렉터리 구분용(청구 가입자). */
public interface FileStorage {
    ClaimAttachment store(Long ownerId, MultipartFile file);
}
