package com.distribution.insurance.common.controller;

import com.distribution.insurance.common.service.IllegalStateTransitionException;
import com.distribution.insurance.common.service.InvalidRequestException;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpStatus;

import static org.assertj.core.api.Assertions.assertThat;

class GlobalExceptionHandlerTest {

    private final GlobalExceptionHandler handler = new GlobalExceptionHandler();

    @Test
    void 도메인_검증_예외는_400() {
        var response = handler.handleInvalidRequest(new InvalidRequestException("형식 오류"));
        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.BAD_REQUEST);
        assertThat(response.getBody()).isEqualTo("형식 오류");
    }

    @Test
    void 상태전이_예외는_409() {
        var response = handler.handleStateTransition(new IllegalStateTransitionException("이미 처리됨"));
        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.CONFLICT);
        assertThat(response.getBody()).isEqualTo("이미 처리됨");
    }
}
