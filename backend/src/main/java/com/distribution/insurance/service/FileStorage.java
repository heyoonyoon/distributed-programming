package com.distribution.insurance.service;

import com.distribution.insurance.domain.claim.ClaimAttachment;
import org.springframework.web.multipart.MultipartFile;

/** 청구 증빙 바이너리를 저장하고 메타를 반환한다. */
public interface FileStorage {
    ClaimAttachment store(Long claimId, MultipartFile file);
}
