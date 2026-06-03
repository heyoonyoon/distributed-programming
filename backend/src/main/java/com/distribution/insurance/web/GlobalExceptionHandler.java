package com.distribution.insurance.web;

import com.distribution.insurance.service.DuplicateEmailException;
import com.distribution.insurance.service.IllegalStateTransitionException;
import com.distribution.insurance.service.InvalidRequestException;
import jakarta.validation.ConstraintViolationException;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.method.annotation.MethodArgumentTypeMismatchException;

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

    /** 잘못된 요청 파라미터(없는 종류 값 등) → 400 */
    @ExceptionHandler(MethodArgumentTypeMismatchException.class)
    public ResponseEntity<String> handleTypeMismatch(MethodArgumentTypeMismatchException e) {
        return ResponseEntity.status(HttpStatus.BAD_REQUEST).body("잘못된 요청 파라미터입니다.");
    }

    /** 제약 위반(음수 보험료 등) → 400. 내부 파라미터 경로 노출 없이 일반 메시지 반환. */
    @ExceptionHandler(ConstraintViolationException.class)
    public ResponseEntity<String> handleConstraintViolation(ConstraintViolationException e) {
        return ResponseEntity.status(HttpStatus.BAD_REQUEST).body("잘못된 요청 파라미터입니다.");
    }

    /** 도메인 검증 실패(종류-추가정보 불일치, 할증 규칙 위반 등) → 400. */
    @ExceptionHandler(InvalidRequestException.class)
    public ResponseEntity<String> handleInvalidRequest(InvalidRequestException e) {
        return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(e.getMessage());
    }

    /** 허용되지 않은 상태 전이(비PENDING 취소·재심사) → 409. */
    @ExceptionHandler(IllegalStateTransitionException.class)
    public ResponseEntity<String> handleStateTransition(IllegalStateTransitionException e) {
        return ResponseEntity.status(HttpStatus.CONFLICT).body(e.getMessage());
    }

    /** DB 무결성 제약 위반(중복 심사 등) → 409. 내부 SQL 노출 없이 고정 메시지 반환. */
    @ExceptionHandler(DataIntegrityViolationException.class)
    public ResponseEntity<String> handleDataIntegrityViolation(DataIntegrityViolationException e) {
        return ResponseEntity.status(HttpStatus.CONFLICT).body("이미 처리된 요청이거나 데이터 제약을 위반했습니다.");
    }
}
