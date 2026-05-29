package com.aiprreviewer;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.context.properties.ConfigurationPropertiesScan;

/**
 * AI PR Review 助手 —— 启动入口
 *
 * 七牛云 XEngineer 暑期实训营 第二批次作品
 * 开发时间: 2026-05-29 ~ 2026-05-31
 */
@SpringBootApplication
@ConfigurationPropertiesScan
public class AiPrReviewerApplication {

    public static void main(String[] args) {
        SpringApplication.run(AiPrReviewerApplication.class, args);
    }
}
