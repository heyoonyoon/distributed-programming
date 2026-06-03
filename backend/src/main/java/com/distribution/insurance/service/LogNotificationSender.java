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
        log.info("[알림] to email={}, phone={} : {}", maskEmail(email), maskPhone(phone), message);
    }

    /** 이메일 마스킹: 로컬 첫 글자만 남기고 나머지를 ***로 대체. e.g. a***@domain.com */
    static String maskEmail(String email) {
        if (email == null || !email.contains("@")) return "***";
        int atIdx = email.indexOf('@');
        return email.charAt(0) + "***" + email.substring(atIdx);
    }

    /** 전화번호 마스킹: 마지막 4자리만 노출. e.g. ***-****-1234 */
    static String maskPhone(String phone) {
        if (phone == null || phone.length() < 4) return "***";
        String last4 = phone.substring(phone.length() - 4);
        return "***-****-" + last4;
    }

    /** 테스트·디버그용 마지막 발송 메시지. */
    public String lastMessage() {
        return lastMessage;
    }
}
