package com.distribution.insurance.service;

import com.distribution.insurance.domain.user.InsuranceEmployee;
import com.distribution.insurance.domain.user.User;
import com.distribution.insurance.repository.UserRepository;
import com.distribution.insurance.security.JwtTokenProvider;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;

@Service
public class AuthService {

    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;
    private final JwtTokenProvider tokenProvider;

    public AuthService(UserRepository userRepository, PasswordEncoder passwordEncoder,
                       JwtTokenProvider tokenProvider) {
        this.userRepository = userRepository;
        this.passwordEncoder = passwordEncoder;
        this.tokenProvider = tokenProvider;
    }

    public String login(String email, String rawPassword) {
        User user = userRepository.findByEmail(email)
                .orElseThrow(() -> new IllegalArgumentException("이메일 또는 비밀번호가 올바르지 않습니다."));
        if (!user.checkPassword(rawPassword, passwordEncoder)) {
            throw new IllegalArgumentException("이메일 또는 비밀번호가 올바르지 않습니다.");
        }
        String userType = user instanceof InsuranceEmployee ? "EMPLOYEE" : "POLICYHOLDER";
        return tokenProvider.createToken(user.getId(), userType);
    }
}
