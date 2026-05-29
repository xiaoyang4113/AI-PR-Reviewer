package com.aiprreviewer.exception;

/**
 * 业务异常，由 GlobalExceptionHandler 统一拦截处理
 */
public class BusinessException extends RuntimeException {

    public BusinessException(String message) {
        super(message);
    }

    public BusinessException(String message, Throwable cause) {
        super(message, cause);
    }
}
