package com.distribution.insurance.web.controller;

import com.distribution.insurance.domain.user.Policyholder;
import com.distribution.insurance.repository.UserRepository;
import com.distribution.insurance.service.ProfileService;
import com.distribution.insurance.web.dto.ProfileResponse;
import com.distribution.insurance.web.dto.UpdateProfileRequest;
import jakarta.validation.Valid;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

@RestController
public class ProfileController {

    private final ProfileService profileService;
    private final UserRepository userRepository;

    public ProfileController(ProfileService profileService, UserRepository userRepository) {
        this.profileService = profileService;
        this.userRepository = userRepository;
    }

    @GetMapping("/me")
    public ProfileResponse me(@AuthenticationPrincipal Long userId) {
        if (!(userRepository.findById(userId).orElseThrow(
                () -> new IllegalArgumentException("사용자를 찾을 수 없습니다."))
                instanceof Policyholder p)) {
            throw new IllegalStateException("가입자(Policyholder)만 조회할 수 있습니다.");
        }
        return ProfileResponse.from(p);
    }

    @PutMapping("/me/profile")
    public ProfileResponse update(@AuthenticationPrincipal Long userId,
                                  @Valid @RequestBody UpdateProfileRequest request) {
        Policyholder updated = profileService.updateProfile(
                userId, request.email(), request.phone(), request.address(), request.bankAccount());
        return ProfileResponse.from(updated);
    }
}
