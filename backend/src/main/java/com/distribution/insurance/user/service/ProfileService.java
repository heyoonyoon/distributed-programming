package com.distribution.insurance.user.service;

import com.distribution.insurance.user.domain.Policyholder;
import com.distribution.insurance.user.repository.UserRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import com.distribution.insurance.common.service.IdentityVerificationService;

@Service
public class ProfileService {

    private static final Logger log = LoggerFactory.getLogger(ProfileService.class);

    private final UserRepository userRepository;
    private final IdentityVerificationService identityVerification;

    public ProfileService(UserRepository userRepository,
                          IdentityVerificationService identityVerification) {
        this.userRepository = userRepository;
        this.identityVerification = identityVerification;
    }

    @Transactional
    public Policyholder updateProfile(Long userId, String email, String phone,
                                      String address, String bankAccount) {
        if (!identityVerification.verify(userId)) {
            throw new IllegalStateException("본인 인증에 실패하였습니다.");
        }
        if (!(userRepository.findById(userId).orElseThrow(
                () -> new IllegalArgumentException("사용자를 찾을 수 없습니다."))
                instanceof Policyholder policyholder)) {
            throw new IllegalStateException("개인정보 수정은 가입자(Policyholder)만 가능합니다.");
        }

        // 이메일을 다른 사용자가 이미 쓰고 있으면 거부(이메일 유일성 보장).
        userRepository.findByEmail(email)
                .filter(other -> !other.getId().equals(userId))
                .ifPresent(other -> {
                    throw new DuplicateEmailException("이미 사용 중인 이메일입니다.");
                });

        policyholder.updateContact(email, phone);
        policyholder.updateProfile(address, bankAccount);
        userRepository.save(policyholder);

        // UC06 후행조건: 변경 기록을 남긴다(로그). PII(이메일·전화)는 값 대신 필드명만 기록.
        log.info("개인정보 변경: userId={} (email, phone, address, bankAccount 갱신)", userId);
        return policyholder;
    }
}
