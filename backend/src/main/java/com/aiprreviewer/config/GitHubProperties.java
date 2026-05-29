package com.aiprreviewer.config;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

/**
 * GitHub API 配置（映射 app.github.*）
 */
@Data
@Component
@ConfigurationProperties(prefix = "app.github")
public class GitHubProperties {

    /** GitHub API 基础地址 */
    private String apiBase = "https://api.github.com";

    /** GitHub 个人访问令牌（可选，提升 API 频率限制） */
    private String token;
}
