package com.aiprreviewer.config;

import com.aiprreviewer.model.constant.ReviewConstants;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.client.SimpleClientHttpRequestFactory;
import org.springframework.web.client.RestTemplate;

/**
 * RestTemplate 配置，用于调用 GitHub API 和 AI API
 */
@Configuration
public class RestTemplateConfig {

    @Bean
    public RestTemplate restTemplate() {
        SimpleClientHttpRequestFactory factory = new SimpleClientHttpRequestFactory();
        // 连接超时：10秒
        factory.setConnectTimeout(ReviewConstants.CONNECT_TIMEOUT_MS);
        // 读取超时：120秒（AI 分析可能耗时较长）
        factory.setReadTimeout(ReviewConstants.READ_TIMEOUT_MS);
        return new RestTemplate(factory);
    }
}
