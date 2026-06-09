package com.distribution.insurance.common.service;

/** 가입 접수·심사 결과 통보(UC02/UC13). 텍스트 구현에서는 mock. */
public interface NotificationSender {
    void send(String email, String phone, String message);
}
