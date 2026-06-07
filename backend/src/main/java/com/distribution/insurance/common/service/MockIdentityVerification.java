package com.distribution.insurance.common.service;

import org.springframework.stereotype.Component;

@Component
public class MockIdentityVerification implements IdentityVerificationService {
    @Override
    public boolean verify(Long userId) {
        return true; // 데모: 항상 성공. 실패 흐름(UC06 E1)은 범위 밖.
    }
}
