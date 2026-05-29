package com.aiprreviewer.config;

import com.aiprreviewer.model.constant.ReviewConstants;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.servlet.config.annotation.CorsRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

/**
 * Web MVC 配置，处理前后端分离的 CORS 跨域请求
 */
@Configuration
public class WebConfig implements WebMvcConfigurer {

    @Override
    public void addCorsMappings(CorsRegistry registry) {
        registry.addMapping(ReviewConstants.API_REVIEW_PATTERN)
                .allowedOriginPatterns("*")
                .allowedMethods(ReviewConstants.ALLOWED_METHODS)
                .allowedHeaders("*")
                .maxAge(ReviewConstants.CORS_MAX_AGE);
    }
}
