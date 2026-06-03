package com.distribution.insurance.web;

import com.distribution.insurance.service.DuplicateEmailException;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

/**
 * 전역 예외 → 일관된 4xx 응답 매핑.
 * 컨트롤러 내부의 로컬 @ExceptionHandler(예: AuthController의 401)가 우선하므로,
 * 로그인 잘못된 자격증명은 여기 영향을 받지 않는다.
 */
@RestControllerAdvice
public class GlobalExceptionHandler {

    /** 리소스 없음(예: 사용자를 찾을 수 없음) → 404 */
    @ExceptionHandler(IllegalArgumentException.class)
    public ResponseEntity<String> handleNotFound(IllegalArgumentException e) {
        return ResponseEntity.status(HttpStatus.NOT_FOUND).body(e.getMessage());
    }

    /** 허용되지 않은 상태(비가입자 수정, 본인인증 실패 등) → 403 */
    @ExceptionHandler(IllegalStateException.class)
    public ResponseEntity<String> handleForbidden(IllegalStateException e) {
        return ResponseEntity.status(HttpStatus.FORBIDDEN).body(e.getMessage());
    }

    /** 이메일 중복 → 409 */
    @ExceptionHandler(DuplicateEmailException.class)
    public ResponseEntity<String> handleDuplicateEmail(DuplicateEmailException e) {
        return ResponseEntity.status(HttpStatus.CONFLICT).body(e.getMessage());
    }
}
