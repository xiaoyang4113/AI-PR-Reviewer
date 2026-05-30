package com.aiprreviewer.exception;

import com.aiprreviewer.model.constant.ReviewConstants;
import com.aiprreviewer.model.dto.ApiResponse;
import jakarta.servlet.http.HttpServletRequest;
import lombok.extern.slf4j.Slf4j;
import org.springframework.dao.DuplicateKeyException;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

import java.util.stream.Collectors;

/**
 * 全局异常处理器，将各类异常转换为统一的 ApiResponse 格式
 */
@Slf4j
@RestControllerAdvice
public class GlobalExceptionHandler {

    /**
     * 处理业务异常
     */
    @ExceptionHandler(BusinessException.class)
    public ResponseEntity<ApiResponse<Void>> handleBusinessException(BusinessException e) {
        log.warn("业务异常: {}", e.getMessage());
        return ResponseEntity.badRequest()
                .body(ApiResponse.error(ReviewConstants.RESPONSE_CODE_BAD_REQUEST, e.getMessage()));
    }

    /**
     * 处理请求参数校验失败
     */
    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ResponseEntity<ApiResponse<Void>> handleValidationException(MethodArgumentNotValidException e) {
        String message = e.getBindingResult().getFieldErrors().stream()
                .map(fe -> fe.getField() + ": " + fe.getDefaultMessage())
                .collect(Collectors.joining("; "));
        log.warn("参数校验失败: {}", message);
        return ResponseEntity.badRequest()
                .body(ApiResponse.error(ReviewConstants.RESPONSE_CODE_BAD_REQUEST, message));
    }

    /**
     * 处理数据库唯一约束冲突（重复提交同一 PR）
     */
    @ExceptionHandler(DuplicateKeyException.class)
    public ResponseEntity<ApiResponse<Void>> handleDuplicateKeyException(DuplicateKeyException e) {
        log.warn("数据库唯一约束冲突: {}", e.getMessage());
        return ResponseEntity.badRequest()
                .body(ApiResponse.error(ReviewConstants.RESPONSE_CODE_BAD_REQUEST,
                        "该 PR 已存在评审记录，可能是短时间内重复提交。如上次评审因网络中断等原因未完成，请等待 5 分钟后再试。"));
    }

    /**
     * 兜底处理未预期的异常，不暴露内部错误详情
     */
    @ExceptionHandler(Exception.class)
    public ResponseEntity<ApiResponse<Void>> handleUnknownException(Exception e, HttpServletRequest request) {
        log.error("未捕获异常 [{} {}]: ", request.getMethod(), request.getRequestURI(), e);
        return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                .body(ApiResponse.error(ReviewConstants.RESPONSE_CODE_ERROR,
                        ReviewConstants.RESPONSE_MSG_SERVER_ERROR));
    }
}
