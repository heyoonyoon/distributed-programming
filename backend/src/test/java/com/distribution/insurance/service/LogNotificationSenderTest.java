package com.distribution.insurance.service;

import org.junit.jupiter.api.Test;
import static org.assertj.core.api.Assertions.assertThat;

class LogNotificationSenderTest {

    @Test
    void 발송하면_마지막_메시지를_보관한다() {
        LogNotificationSender sender = new LogNotificationSender();
        sender.send("h@test.com", "010-1234", "접수번호 7 — 예상 처리 3일");
        assertThat(sender.lastMessage()).contains("접수번호 7");
    }
}
