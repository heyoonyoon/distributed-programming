package com.distribution.insurance.service;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import java.time.LocalDate;

/** 매일 오전 9시 미납 고지서 발송(UC16 1단계). 로직은 NoticeService에 위임. */
@Component
public class OverdueNoticeScheduler {

    private static final Logger log = LoggerFactory.getLogger(OverdueNoticeScheduler.class);

    private final NoticeService noticeService;

    public OverdueNoticeScheduler(NoticeService noticeService) {
        this.noticeService = noticeService;
    }

    @Scheduled(cron = "0 0 9 * * *")
    public void run() {
        int created = noticeService.issueOverdueNotices(LocalDate.now());
        log.info("미납 고지서 발송 완료: {}건", created);
    }
}
