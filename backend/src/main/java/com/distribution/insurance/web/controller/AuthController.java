package com.distribution.insurance.web.controller;

import com.distribution.insurance.service.AuthService;
import com.distribution.insurance.web.dto.LoginRequest;
import com.distribution.insurance.web.dto.TokenResponse;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/auth")
public class AuthController {

    private final AuthService authService;

    public AuthController(AuthService authService) {
        this.authService = authService;
    }

    @PostMapping("/login")
    public TokenResponse login(@Valid @RequestBody LoginRequest request) {
        return new TokenResponse(authService.login(request.email(), request.password()));
    }

    @PostMapping("/logout")
    public ResponseEntity<Void> logout() {
        // 무상태 JWT: 서버는 토큰을 폐기하지 않는다. 프론트가 토큰을 삭제.
        return ResponseEntity.noContent().build();
    }

    @ExceptionHandler(IllegalArgumentException.class)
    public ResponseEntity<String> handleBadCredentials(IllegalArgumentException e) {
        return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body(e.getMessage());
    }
}
