package com.distribution.insurance.config;

import org.springframework.context.annotation.Configuration;
import org.springframework.scheduling.annotation.EnableScheduling;

/** @Scheduled 활성화(UC16 일일 자동 실행). */
@Configuration
@EnableScheduling
public class SchedulingConfig {
}
