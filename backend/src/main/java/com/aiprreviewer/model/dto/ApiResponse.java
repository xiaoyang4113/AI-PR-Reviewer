package com.aiprreviewer.model.dto;

import com.aiprreviewer.model.constant.ReviewConstants;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * 统一 API 响应格式
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
public class ApiResponse<T> {

    /** 状态码 */
    private Integer code;

    /** 提示信息 */
    private String message;

    /** 响应数据 */
    private T data;

    /** 成功响应快捷方法 */
    public static <T> ApiResponse<T> success(T data) {
        return new ApiResponse<>(ReviewConstants.RESPONSE_CODE_OK, ReviewConstants.RESPONSE_MSG_SUCCESS, data);
    }

    /** 失败响应快捷方法 */
    public static <T> ApiResponse<T> error(Integer code, String message) {
        return new ApiResponse<>(code, message, null);
    }
}
