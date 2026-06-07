package com.distribution.insurance.common.service;

import org.junit.jupiter.api.Test;
import static org.assertj.core.api.Assertions.assertThat;

class LogNotificationSenderTest {

    @Test
    void 발송하면_마지막_메시지를_보관한다() {
        LogNotificationSender sender = new LogNotificationSender();
        sender.send("h@test.com", "010-1234", "접수번호 7 — 예상 처리 3일");
        assertThat(sender.lastMessage()).contains("접수번호 7");
    }

    @Test
    void maskEmail_로컬첫글자만_남기고_마스킹한다() {
        assertThat(LogNotificationSender.maskEmail("hello@example.com")).isEqualTo("h***@example.com");
        assertThat(LogNotificationSender.maskEmail("a@b.com")).isEqualTo("a***@b.com");
        assertThat(LogNotificationSender.maskEmail(null)).isEqualTo("***");
        assertThat(LogNotificationSender.maskEmail("nodomain")).isEqualTo("***");
    }

    @Test
    void maskPhone_마지막4자리만_노출한다() {
        assertThat(LogNotificationSender.maskPhone("010-1234-5678")).isEqualTo("***-****-5678");
        assertThat(LogNotificationSender.maskPhone("01012345678")).isEqualTo("***-****-5678");
        assertThat(LogNotificationSender.maskPhone(null)).isEqualTo("***");
        assertThat(LogNotificationSender.maskPhone("123")).isEqualTo("***");
    }
}
