package com.distribution.insurance.service;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

/** 로그 기반 mock 알림(Epic 0 MockIdentityVerification 패턴). */
@Component
public class LogNotificationSender implements NotificationSender {

    private static final Logger log = LoggerFactory.getLogger(LogNotificationSender.class);
    private String lastMessage;

    @Override
    public void send(String email, String phone, String message) {
        this.lastMessage = message;
        log.info("[알림] to email={}, phone={} : {}", email, phone, message);
    }

    /** 테스트·디버그용 마지막 발송 메시지. */
    public String lastMessage() {
        return lastMessage;
    }
}
