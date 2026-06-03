package com.distribution.insurance.service;

public interface IdentityVerificationService {
    /** 본인인증 수행. 외부 연동(휴대폰/공동인증서)의 자리표시자. */
    boolean verify(Long userId);
}
